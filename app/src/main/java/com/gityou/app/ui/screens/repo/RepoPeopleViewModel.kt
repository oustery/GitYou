package com.gityou.app.ui.screens.repo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.remote.dto.Page
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

/** Which population of a repository the screen shows. */
enum class RepoPeopleKind { STARGAZERS, FORKS }

data class RepoPeopleUi(
    val items: List<Any> = emptyList(),
    val nextPage: Int? = 1,
    val loadingMore: Boolean = false,
    val loadMoreFailed: Boolean = false,
) {
    val hasMore: Boolean get() = nextPage != null
}

/** Stargazers or forks of one repository, with Link-header pagination. */
@HiltViewModel
class RepoPeopleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reposRepository: ReposRepository,
) : ViewModel() {

    val owner: String = savedStateHandle.get<String>("owner").orEmpty()
    val repoName: String = savedStateHandle.get<String>("name").orEmpty()

    val kind: RepoPeopleKind = when (savedStateHandle.get<String>("kind")) {
        "forks" -> RepoPeopleKind.FORKS
        else -> RepoPeopleKind.STARGAZERS
    }

    private val _state = MutableStateFlow<UiState<RepoPeopleUi>>(UiState.Loading)
    val state: StateFlow<UiState<RepoPeopleUi>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = safeCall { fetchPage(1) }.fold(
                onSuccess = { page ->
                    UiState.Content(RepoPeopleUi(items = page.items, nextPage = page.nextPage))
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
            val page = safeCall { fetchPage(nextPage) }
            val latest = (_state.value as? UiState.Content)?.value
            if (latest == null || !latest.loadingMore) return@launch
            _state.value = page.fold(
                onSuccess = { p ->
                    UiState.Content(
                        latest.copy(
                            items = latest.items + p.items,
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

    private suspend fun fetchPage(page: Int): Page<Any> {
        @Suppress("UNCHECKED_CAST")
        return when (kind) {
            RepoPeopleKind.STARGAZERS -> reposRepository.stargazers(owner, repoName, page) as Page<Any>
            RepoPeopleKind.FORKS -> reposRepository.forks(owner, repoName, page) as Page<Any>
        }
    }
}
