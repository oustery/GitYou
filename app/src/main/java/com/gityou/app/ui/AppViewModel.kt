package com.gityou.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.repo.AuthRepository
import com.gityou.app.data.repo.NotificationsRepository
import com.gityou.app.data.remote.dto.User
import com.gityou.app.data.store.TokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        viewModelScope.launch {
            tokenStore.settingsFlow.collect { _settings.value = it }
        }
        viewModelScope.launch {
            tokenStore.tokenFlow.collect { token ->
                if (token.isNullOrBlank()) {
                    _appState.value = AppState.SignedOut
                    _unreadCount.value = 0
                    return@collect
                }
                val user = try {
                    authRepository.currentUser()
                } catch (error: Exception) {
                    if (error is HttpException && error.code() == 401) {
                        // Stale token: drop it, the flow re-emits and signs us out.
                        tokenStore.clearToken()
                    }
                    null // network problems or rate limits keep the signed-in shell
                }
                if (tokenStore.cachedToken() != null) {
                    _appState.value = AppState.SignedIn(user)
                    refreshUnread()
                }
            }
        }
    }

    /** Refreshes the badge count shown on the navigation bell. */
    fun refreshUnread() {
        viewModelScope.launch {
            _unreadCount.value = notificationsRepository.unreadEstimate() ?: 0
        }
    }
}
