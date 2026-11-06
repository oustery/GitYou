package com.gityou.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.remote.dto.Repo
import com.gityou.app.data.remote.dto.User
import com.gityou.app.data.repo.AuthRepository
import com.gityou.app.data.repo.ReposRepository
import com.gityou.app.data.util.ApiErrors
import com.gityou.app.ui.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUi(
    val user: User,
    val repos: List<Repo> = emptyList(),
    val reposNextPage: Int? = 1,
    val loadingRepos: Boolean = false,
    val stars: List<Repo> = emptyList(),
    val starsNextPage: Int? = 1,
    val loadingStars: Boolean = false,
    val selectedTab: Int = 0,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val reposRepository: ReposRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ProfileUi>>(UiState.Loading)
    val state: StateFlow<UiState<ProfileUi>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                val user = authRepository.currentUser()
                UiState.Content(ProfileUi(user = user))
            } catch (t: Throwable) {
                UiState.Error(ApiErrors.keyOf(t))
                return@launch
            }
            val reposDeferred = async { runCatching { reposRepository.myRepos(page = 1) } }
            val starsDeferred = async { runCatching { reposRepository.myStars(page = 1) } }

            val reposResult = reposDeferred.await()
            val starsResult = starsDeferred.await()

            (_state.value as? UiState.Content)?.let { content ->
                var ui = content.value
                reposResult.getOrNull()?.let { page ->
                    ui = ui.copy(repos = page.items, reposNextPage = page.nextPage)
                }
                starsResult.getOrNull()?.let { page ->
                    ui = ui.copy(stars = page.items, starsNextPage = page.nextPage)
                }
                _state.value = UiState.Content(ui)
            }
        }
    }

    fun selectTab(index: Int) {
        (_state.value as? UiState.Content)?.let { content ->
            _state.value = UiState.Content(content.value.copy(selectedTab = index))
        }
    }

    fun loadReposMore() {
        val current = (_state.value as? UiState.Content)?.value ?: return
        if (current.loadingRepos || current.reposNextPage == null) return
        viewModelScope.launch {
            patch { it.copy(loadingRepos = true) }
            try {
                val page = reposRepository.myRepos(page = current.reposNextPage)
                patch { it.copy(repos = it.repos + page.items, reposNextPage = page.nextPage, loadingRepos = false) }
            } catch (t: Throwable) {
                patch { it.copy(loadingRepos = false) }
            }
        }
    }

    fun loadStarsMore() {
        val current = (_state.value as? UiState.Content)?.value ?: return
        if (current.loadingStars || current.starsNextPage == null) return
        viewModelScope.launch {
            patch { it.copy(loadingStars = true) }
            try {
                val page = reposRepository.myStars(page = current.starsNextPage)
                patch { it.copy(stars = it.stars + page.items, starsNextPage = page.nextPage, loadingStars = false) }
            } catch (t: Throwable) {
                patch { it.copy(loadingStars = false) }
            }
        }
    }

    private fun patch(update: (ProfileUi) -> ProfileUi) {
        (_state.value as? UiState.Content)?.let { content ->
            _state.value = UiState.Content(update(content.value))
        }
    }
}
