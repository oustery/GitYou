package com.gityou.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.remote.dto.User
import com.gityou.app.data.repo.AuthRepository
import com.gityou.app.data.store.TokenStore
import com.gityou.app.data.telemetry.CrashReporter
import com.gityou.app.data.telemetry.Telemetry
import com.gityou.app.data.util.ApiErrors
import com.gityou.app.data.util.safeCall
import com.gityou.app.ui.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the Diagnostics section shows about stored crash reports (G-017). */
data class CrashInfo(
    val count: Int = 0,
    /** Epoch millis of the newest report, or null when none exist. */
    val lastAt: Long? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val authRepository: AuthRepository,
    private val telemetry: Telemetry,
    private val crashReporter: CrashReporter,
) : ViewModel() {

    private val _settings = MutableStateFlow(TokenStore.AppSettings())
    val settings: StateFlow<TokenStore.AppSettings> = _settings.asStateFlow()

    private val _user = MutableStateFlow<UiState<User>>(UiState.Loading)
    val user: StateFlow<UiState<User>> = _user.asStateFlow()

    private val _crash = MutableStateFlow(CrashInfo())
    val crash: StateFlow<CrashInfo> = _crash.asStateFlow()

    init {
        viewModelScope.launch {
            tokenStore.settingsFlow.collect { _settings.value = it }
        }
        retryUser()
        refreshCrashInfo()
    }

    /** Loads (or reloads) the account card; wired to the card's Retry button (F-012). */
    fun retryUser() {
        viewModelScope.launch {
            _user.value = UiState.Loading
            _user.value = safeCall { authRepository.currentUser() }.fold(
                onSuccess = { UiState.Content(it) },
                onFailure = { t -> UiState.Error(ApiErrors.keyOf(t)) },
            )
        }
    }

    /** File of the newest crash report, shared from the screen via FileProvider. */
    fun latestCrashFile(): File? = crashReporter.latest()

    fun refreshCrashInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val info = CrashInfo(count = crashReporter.count(), lastAt = crashReporter.latest()?.let {
                runCatching { parseCrashTimestamp(it) }.getOrNull()
            })
            _crash.value = info
        }
    }

    fun clearCrashReports() {
        viewModelScope.launch(Dispatchers.IO) {
            crashReporter.clear()
            withContext(Dispatchers.Main) { _crash.value = CrashInfo() }
        }
    }

    private fun parseCrashTimestamp(file: File): Long =
        file.name.removePrefix(CrashReporter.PREFIX).removeSuffix(".json").toLong()

    fun setThemeMode(mode: String) {
        viewModelScope.launch { tokenStore.saveThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { tokenStore.saveDynamicColor(enabled) }
    }

    fun setAmoled(enabled: Boolean) {
        viewModelScope.launch { tokenStore.saveAmoled(enabled) }
    }

    fun setAccent(accent: String) {
        viewModelScope.launch { tokenStore.saveAccent(accent) }
    }

    fun setFontScale(scale: String) {
        viewModelScope.launch { tokenStore.saveFontScale(scale) }
    }

    fun setExpressiveMotion(enabled: Boolean) {
        viewModelScope.launch { tokenStore.saveExpressiveMotion(enabled) }
    }

    fun setDefaultTab(tab: String) {
        viewModelScope.launch { tokenStore.saveDefaultTab(tab) }
    }

    fun setAppLanguage(language: String) {
        viewModelScope.launch { tokenStore.saveAppLanguage(language) }
    }

    fun setRenderMarkdown(enabled: Boolean) {
        viewModelScope.launch { tokenStore.saveRenderMarkdown(enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { tokenStore.saveHapticsEnabled(enabled) }
    }

    fun setMarkReadOnOpen(enabled: Boolean) {
        viewModelScope.launch { tokenStore.saveMarkReadOnOpen(enabled) }
    }

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch { tokenStore.saveBiometricLock(enabled) }
    }

    fun setLockTimeout(minutes: Int) {
        viewModelScope.launch { tokenStore.saveLockTimeout(minutes) }
    }

    fun setTelemetryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            tokenStore.saveTelemetryEnabled(enabled)
            // "Off" erases what was recorded (G-017): no data is retained
            // behind a switch the user turned off.
            if (!enabled) telemetry.wipe()
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
