package com.gityou.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.remote.dto.Repo
import com.gityou.app.data.repo.ReposRepository
import com.gityou.app.data.util.ApiErrors
import com.gityou.app.data.util.safeCall
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
    // Turns the pagination footer into a retry button after a failure (F-013).
    val loadMoreFailed: Boolean = false,
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
            _state.value = safeCall { reposRepository.myRepos(page = 1) }.fold(
                onSuccess = { page ->
                    UiState.Content(HomeUi(repos = page.items, nextPage = page.nextPage))
                },
                onFailure = { t -> UiState.Error(ApiErrors.keyOf(t)) },
            )
            _refreshing.value = false
        }
    }

    fun loadMore() {
        val current = (_state.value as? UiState.Content)?.value ?: return
        if (current.loadingMore || current.nextPage == null) return
        val nextPage = current.nextPage ?: return
        viewModelScope.launch {
            _state.value = UiState.Content(current.copy(loadingMore = true))
            val page = safeCall { reposRepository.myRepos(page = nextPage) }
            // Merge only while the request we started is still current: a
            // pull-to-refresh that completed meanwhile has already replaced
            // the state (loadingMore == false) and must not be rolled back (F-006).
            val latest = (_state.value as? UiState.Content)?.value
            if (latest == null || !latest.loadingMore) return@launch
            _state.value = page.fold(
                onSuccess = { p ->
                    UiState.Content(
                        latest.copy(
                            repos = latest.repos + p.items,
                            nextPage = p.nextPage,
                            loadingMore = false,
                            loadMoreFailed = false,
                        ),
                    )
                },
                onFailure = { _ ->
                    // Keep the loaded content; the footer becomes a retry button.
                    UiState.Content(latest.copy(loadingMore = false, loadMoreFailed = true))
                },
            )
        }
    }
}
