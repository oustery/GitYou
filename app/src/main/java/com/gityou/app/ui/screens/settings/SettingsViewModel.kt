package com.gityou.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.remote.dto.User
import com.gityou.app.data.repo.AuthRepository
import com.gityou.app.data.store.TokenStore
import com.gityou.app.data.util.ApiErrors
import com.gityou.app.data.util.safeCall
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
        retryUser()
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

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
