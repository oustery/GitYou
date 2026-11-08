package com.gityou.app.ui.screens.commits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.remote.dto.CommitListItem
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

data class CommitsUi(
    val commits: List<CommitListItem> = emptyList(),
    val nextPage: Int? = 1,
    val loadingMore: Boolean = false,
    val loadMoreFailed: Boolean = false,
) {
    val hasMore: Boolean get() = nextPage != null
}

/** Commit history of one repository branch, with Link-header pagination. */
@HiltViewModel
class CommitsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reposRepository: ReposRepository,
) : ViewModel() {

    val owner: String = savedStateHandle.get<String>("owner").orEmpty()
    val repoName: String = savedStateHandle.get<String>("name").orEmpty()

    /** Branch/tag the list is bound to; null means the default branch. */
    val ref: String? = savedStateHandle.get<String>("ref")?.takeIf { it.isNotBlank() }

    private val _state = MutableStateFlow<UiState<CommitsUi>>(UiState.Loading)
    val state: StateFlow<UiState<CommitsUi>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = safeCall { reposRepository.commits(owner, repoName, ref, page = 1) }.fold(
                onSuccess = { page ->
                    UiState.Content(CommitsUi(commits = page.items, nextPage = page.nextPage))
                },
                onFailure = { t -> UiState.Error(ApiErrors.keyOf(t)) },
            )
        }
    }

    fun loadMore() {
        val current = (_state.value as? UiState.Content)?.value ?: return
        if (current.loadingMore || current.nextPage == null) return
        val nextPage = current.nextPage ?: return
        viewModelScope.launch {
            _state.value = UiState.Content(current.copy(loadingMore = true))
            val page = safeCall { reposRepository.commits(owner, repoName, ref, page = nextPage) }
            val latest = (_state.value as? UiState.Content)?.value
            if (latest == null || !latest.loadingMore) return@launch
            _state.value = page.fold(
                onSuccess = { p ->
                    UiState.Content(
                        latest.copy(
                            commits = latest.commits + p.items,
                            nextPage = p.nextPage,
                            loadingMore = false,
                            loadMoreFailed = false,
                        ),
                    )
                },
                onFailure = { _ ->
                    UiState.Content(latest.copy(loadingMore = false, loadMoreFailed = true))
                },
            )
        }
    }
}
