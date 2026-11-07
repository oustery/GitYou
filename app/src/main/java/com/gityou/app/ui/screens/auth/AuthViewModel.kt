package com.gityou.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.remote.DeviceFlow
import com.gityou.app.data.remote.PollDecision
import com.gityou.app.data.remote.dto.DeviceCodeResponse
import com.gityou.app.data.repo.AuthRepository
import com.gityou.app.data.util.ApiErrors
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

enum class DeviceFlowPhase { IDLE, WAITING, EXPIRED, DENIED, ERROR }

data class DeviceFlowState(
    val phase: DeviceFlowPhase = DeviceFlowPhase.IDLE,
    val userCode: String = "",
    val verificationUri: String = "",
    val errorKey: String = "",
)

enum class PatPhase { IDLE, SUBMITTING, ERROR }
data class PatState(val phase: PatPhase = PatPhase.IDLE, val errorKey: String = "")

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val _deviceFlow = MutableStateFlow(DeviceFlowState())
    val deviceFlow: StateFlow<DeviceFlowState> = _deviceFlow.asStateFlow()

    private val _patState = MutableStateFlow(PatState())
    val patState: StateFlow<PatState> = _patState.asStateFlow()

    private val _clientId = MutableStateFlow("")
    val clientId: StateFlow<String> = _clientId.asStateFlow()

    private val _clientIdSaved = MutableStateFlow(false)
    val clientIdSaved: StateFlow<Boolean> = _clientIdSaved.asStateFlow()

    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            _clientId.value = authRepository.clientId()
            _clientIdSaved.value = _clientId.value.isNotBlank()
        }
    }

    fun onClientIdChanged(value: String) {
        _clientId.value = value
        _clientIdSaved.value = false
    }

    fun saveClientId() {
        viewModelScope.launch {
            authRepository.saveClientId(_clientId.value.trim())
            _clientId.value = authRepository.clientId()
            _clientIdSaved.value = _clientId.value.isNotBlank()
        }
    }

    fun startDeviceFlow() {
        pollJob?.cancel()
        val clientId = _clientId.value.trim()
        if (clientId.isBlank()) {
            _deviceFlow.value = DeviceFlowState(
                phase = DeviceFlowPhase.ERROR,
                errorKey = "missing_client_id",
            )
            return
        }
        // Keep the Job: «Отмена» must be able to stop the polling loop (F-003).
        pollJob = viewModelScope.launch {
            try {
                val code = authRepository.beginDeviceFlow(clientId)
                _deviceFlow.value = DeviceFlowState(
                    phase = DeviceFlowPhase.WAITING,
                    userCode = code.userCode,
                    verificationUri = code.verificationUri,
                )
                pollForToken(clientId, code)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                _deviceFlow.value = DeviceFlowState(
                    phase = DeviceFlowPhase.ERROR,
                    errorKey = ApiErrors.keyOf(t),
                )
            }
        }
    }

    private suspend fun pollForToken(clientId: String, code: DeviceCodeResponse) {
        var interval = code.interval.coerceAtLeast(DeviceFlow.DEFAULT_INTERVAL_SECONDS)
        // Monotonic mark instead of wall-clock millis: no hidden dependency on
        // system time changes, testable with a fake TimeSource (F-015).
        val started = timeSource.markNow()
        val timeout = code.expiresIn.seconds
        while (started.elapsedNow() < timeout && kotlinx.coroutines.currentCoroutineContext().isActive) {
            delay(interval * 1000L)
            val response = try {
                authRepository.pollForToken(clientId, code.deviceCode)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                _deviceFlow.value = _deviceFlow.value.copy(
                    phase = DeviceFlowPhase.ERROR,
                    errorKey = ApiErrors.keyOf(t),
                )
                return
            }
            when (val decision = DeviceFlow.decide(response, interval)) {
                is PollDecision.Success -> {
                    _deviceFlow.value = DeviceFlowState(phase = DeviceFlowPhase.IDLE)
                    try {
                        authRepository.signInWithPat(decision.token)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Throwable) {
                        // AppViewModel flips to SignedIn once the token persists;
                        // validation failures surface there.
                    }
                    return
                }
                is PollDecision.Continue -> interval = decision.intervalSeconds
                PollDecision.Expired -> {
                    _deviceFlow.value = _deviceFlow.value.copy(phase = DeviceFlowPhase.EXPIRED)
                    return
                }
                PollDecision.Denied -> {
                    _deviceFlow.value = _deviceFlow.value.copy(phase = DeviceFlowPhase.DENIED)
                    return
                }
                is PollDecision.Failed -> {
                    _deviceFlow.value = _deviceFlow.value.copy(
                        phase = DeviceFlowPhase.ERROR,
                        errorKey = "unknown",
                    )
                    return
                }
            }
        }
        if (_deviceFlow.value.phase == DeviceFlowPhase.WAITING) {
            _deviceFlow.value = _deviceFlow.value.copy(phase = DeviceFlowPhase.EXPIRED)
        }
    }

    fun cancelDeviceFlow() {
        pollJob?.cancel()
        pollJob = null
        _deviceFlow.value = DeviceFlowState(phase = DeviceFlowPhase.IDLE)
    }

    fun signInWithPat(token: String) {
        if (token.isBlank()) return
        viewModelScope.launch {
            _patState.value = PatState(phase = PatPhase.SUBMITTING)
            _patState.value = try {
                authRepository.signInWithPat(token)
                PatState(phase = PatPhase.IDLE)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                PatState(phase = PatPhase.ERROR, errorKey = ApiErrors.keyOf(t))
            }
        }
    }
}
