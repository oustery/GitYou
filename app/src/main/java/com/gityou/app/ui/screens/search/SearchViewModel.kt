package com.gityou.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.remote.dto.Repo
import com.gityou.app.data.repo.ReposRepository
import com.gityou.app.data.util.ApiErrors
import com.gityou.app.data.util.safeCall
import com.gityou.app.ui.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Sort options mapped to the GitHub search API `sort` parameter. */
enum class SearchSort(val apiValue: String?) {
    BEST(null),
    STARS("stars"),
    UPDATED("updated"),
}

data class SearchUi(
    val query: String = "",
    val repos: List<Repo> = emptyList(),
    val nextPage: Int? = null,
    val loadingMore: Boolean = false,
    // A failed «load more» must not wedge pagination forever: the footer
    // turns into an explicit retry affordance until it succeeds (F-013).
    val loadMoreFailed: Boolean = false,
    val searched: Boolean = false,
    val sort: SearchSort = SearchSort.BEST,
) {
    val hasMore: Boolean get() = nextPage != null
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val reposRepository: ReposRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _sort = MutableStateFlow(SearchSort.BEST)
    val sort: StateFlow<SearchSort> = _sort.asStateFlow()

    // Bumping this value re-runs the current search: StateFlows are combined
    // directly (NOT via snapshotFlow, which never re-reads them — F-001).
    private val _retry = MutableStateFlow(0)

    private val _results = MutableStateFlow<UiState<SearchUi>>(UiState.Content(SearchUi()))
    val results: StateFlow<UiState<SearchUi>> = _results.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                _query.debounce(600),
                _sort,
                _retry,
            ) { query, sort, _ -> query to sort }
                .collectLatest { (query, sort) ->
                    if (query.isBlank()) {
                        _results.value = UiState.Content(SearchUi(sort = sort))
                    } else {
                        _results.value = UiState.Loading
                        _results.value = safeCall {
                            reposRepository.search(
                                query = query.trim(),
                                page = 1,
                                sort = sort.apiValue,
                            )
                        }.fold(
                            onSuccess = { pageData ->
                                UiState.Content(
                                    SearchUi(
                                        query = query.trim(),
                                        repos = pageData.items,
                                        nextPage = pageData.nextPage,
                                        searched = true,
                                        sort = sort,
                                    ),
                                )
                            },
                            onFailure = { t -> UiState.Error(ApiErrors.keyOf(t)) },
                        )
                    }
                }
        }
    }

    fun onQueryChanged(value: String) {
        _query.value = value
    }

    fun setSort(sort: SearchSort) {
        _sort.value = sort
    }

    /** Re-runs the current query immediately (search button, error retry). */
    fun retrySearch() {
        _retry.update { it + 1 }
    }

    fun loadMore() {
        val current = (_results.value as? UiState.Content)?.value ?: return
        // A failed attempt stays retryable through [retrySearch]; only a
        // running request or an exhausted list blocks a new one.
        if (current.loadingMore || current.nextPage == null) return
        val nextPage = current.nextPage ?: return
        viewModelScope.launch {
            _results.value = UiState.Content(current.copy(loadingMore = true))
            val page = safeCall {
                reposRepository.search(
                    query = current.query,
                    page = nextPage,
                    sort = current.sort.apiValue,
                )
            }
            // Merge into the latest content only if it is still the request we
            // started; a refresh that completed meanwhile must not be rolled
            // back (F-006). After a refresh `loadingMore` is false again.
            val latest = (_results.value as? UiState.Content)?.value
            if (latest == null || !latest.loadingMore) return@launch
            _results.value = page.fold(
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
                    UiState.Content(latest.copy(loadingMore = false, loadMoreFailed = true))
                },
            )
        }
    }
}
