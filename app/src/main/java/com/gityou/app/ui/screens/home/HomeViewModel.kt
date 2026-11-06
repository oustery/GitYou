package com.gityou.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.remote.dto.Repo
import com.gityou.app.data.repo.ReposRepository
import com.gityou.app.data.util.ApiErrors
import com.gityou.app.ui.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUi(
    val repos: List<Repo> = emptyList(),
    val nextPage: Int? = 1,
    val loadingMore: Boolean = false,
) {
    val hasMore: Boolean get() = nextPage != null
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val reposRepository: ReposRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<HomeUi>>(UiState.Loading)
    val state: StateFlow<UiState<HomeUi>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            _state.value = try {
                val page = reposRepository.myRepos(page = 1)
                UiState.Content(HomeUi(repos = page.items, nextPage = page.nextPage))
            } catch (t: Throwable) {
                UiState.Error(ApiErrors.keyOf(t))
            }
            _refreshing.value = false
        }
    }

    fun loadMore() {
        val current = (_state.value as? UiState.Content)?.value ?: return
        if (current.loadingMore || current.nextPage == null) return
        viewModelScope.launch {
            _state.value = UiState.Content(current.copy(loadingMore = true))
            _state.value = try {
                val page = reposRepository.myRepos(page = current.nextPage)
                UiState.Content(
                    HomeUi(
                        repos = current.repos + page.items,
                        nextPage = page.nextPage,
                        loadingMore = false,
                    ),
                )
            } catch (t: Throwable) {
                // Keep the loaded content; the footer simply stops spinning.
                UiState.Content(current.copy(loadingMore = false))
            }
        }
    }
}
