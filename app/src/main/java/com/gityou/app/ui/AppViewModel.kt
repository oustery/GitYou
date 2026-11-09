package com.gityou.app.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.repo.AuthRepository
import com.gityou.app.data.repo.NotificationsRepository
import com.gityou.app.data.remote.dto.User
import com.gityou.app.data.store.TokenStore
import com.gityou.app.data.util.DeepLinkRouting
import com.gityou.app.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Root application state: authentication shell + persisted appearance settings. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val authRepository: AuthRepository,
    private val notificationsRepository: NotificationsRepository,
) : ViewModel() {

    sealed interface AppState {
        data object Loading : AppState
        data object SignedOut : AppState
        data class SignedIn(val user: User?) : AppState
    }

    private val _appState = MutableStateFlow<AppState>(AppState.Loading)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private val _settings = MutableStateFlow(TokenStore.AppSettings())
    val settings: StateFlow<TokenStore.AppSettings> = _settings.asStateFlow()

    /**
     * The badge reads the shared count from TokenStore instead of owning a
     * second copy (G-004): the notifications screen publishes the real number
     * after every fetch/mark-read, and the badge follows.
     */
    val unreadCount: StateFlow<Int> = tokenStore.unreadCount

    /**
     * Navigation route requested by an inbound App Link (G-012), held until
     * the UI is signed in and the NavController exists. Drained by
     * [consumePendingRoute].
     */
    private val _pendingRoute = MutableStateFlow<String?>(null)
    val pendingRoute: StateFlow<String?> = _pendingRoute.asStateFlow()

    /**
     * Whether the app is locked (G-016). Set from the lifecycle: the app locks
     * when it went to the background longer than the configured timeout, or
     * always on a cold start.
     */
    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private var backgroundedAt: Long? = null

    /** Text shared into the app ("Share to GitYou"), offered as a new issue. */
    private val _pendingShare = MutableStateFlow<String?>(null)
    val pendingShare: StateFlow<String?> = _pendingShare.asStateFlow()

    init {
        viewModelScope.launch {
            tokenStore.settingsFlow.collect { _settings.value = it }
        }
        viewModelScope.launch {
            tokenStore.tokenFlow.collect { token ->
                if (token.isNullOrBlank()) {
                    _appState.value = AppState.SignedOut
                    tokenStore.clearUnread()
                    return@collect
                }
                val user = try {
                    authRepository.currentUser()
                } catch (e: CancellationException) {
                    throw e
                } catch (error: HttpException) {
                    if (error.code() == 401) {
                        // Stale token: drop it, the flow re-emits and signs us out.
                        tokenStore.clearToken()
                    }
                    null // auth problems other than 401 keep the signed-in shell
                } catch (ignored: Exception) {
                    // Network problems or rate limits keep the signed-in shell;
                    // the token flow re-validates on the next change.
                    null
                }
                if (tokenStore.cachedToken() != null) {
                    _appState.value = AppState.SignedIn(user)
                    refreshUnread()
                }
            }
        }
    }

    /** Routes an inbound intent; called by the Activity on create and on new intent. */
    fun onSubmit(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val data = intent.dataString ?: return
                _pendingRoute.value = DeepLinkRouting.route(
                    url = data,
                    issueRoute = Route::issue,
                    repoRoute = Route::repo,
                    userRoute = Route::user,
                )
            }
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                _pendingShare.value = DeepLinkRouting.firstGitHubUrl(text) ?: text?.trim()
            }
        }
    }

    /**
     * Re-arms the lock after the app spent more than the configured timeout in
     * the background. A short trip (notification shade, permission dialog)
     * must not ask for the credential again.
     */
    fun onAppBackgrounded() {
        backgroundedAt = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        val leftAt = backgroundedAt ?: return
        backgroundedAt = null
        if (!_settings.value.biometricLock) return
        val timeoutMs = _settings.value.lockTimeoutMinutes.coerceAtLeast(0) * 60_000L
        if (System.currentTimeMillis() - leftAt >= timeoutMs) _locked.value = true
    }

    fun unlock() {
        _locked.value = false
    }

    /** Locks immediately (used when the setting is switched on). */
    fun lockNow() {
        if (_settings.value.biometricLock) _locked.value = true
    }

    fun consumePendingRoute() {
        _pendingRoute.value = null
    }

    fun consumePendingShare() {
        _pendingShare.value = null
    }

    /**
     * Refreshes the badge from the API. A null result (offline, rate limit)
     * keeps the last known number instead of resetting it to zero, which used
     * to make the bell look "all read" after a failed refresh.
     */
    fun refreshUnread() {
        viewModelScope.launch {
            notificationsRepository.unreadCount()?.let { tokenStore.setUnreadCount(it) }
        }
    }
}
