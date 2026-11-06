package com.gityou.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.remote.dto.User
import com.gityou.app.data.repo.AuthRepository
import com.gityou.app.data.store.TokenStore
import com.gityou.app.data.util.ApiErrors
import com.gityou.app.ui.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _settings = MutableStateFlow(TokenStore.AppSettings())
    val settings: StateFlow<TokenStore.AppSettings> = _settings.asStateFlow()

    private val _user = MutableStateFlow<UiState<User>>(UiState.Loading)
    val user: StateFlow<UiState<User>> = _user.asStateFlow()

    init {
        viewModelScope.launch {
            tokenStore.settingsFlow.collect { _settings.value = it }
        }
        viewModelScope.launch {
            _user.value = try {
                UiState.Content(authRepository.currentUser())
            } catch (t: Throwable) {
                UiState.Error(ApiErrors.keyOf(t))
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { tokenStore.saveThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { tokenStore.saveDynamicColor(enabled) }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
