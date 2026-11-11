package com.gityou.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.remote.dto.Issue
import com.gityou.app.data.remote.dto.Repo
import com.gityou.app.data.remote.dto.User
import com.gityou.app.data.repo.ReposRepository
import com.gityou.app.data.telemetry.Telemetry
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

/** What the query searches: repositories, users or issues/PRs. */
enum class SearchType { REPOS, USERS, ISSUES }

data class SearchUi(
    val query: String = "",
    val type: SearchType = SearchType.REPOS,
    val repos: List<Repo> = emptyList(),
    val users: List<User> = emptyList(),
    val issues: List<Issue> = emptyList(),
    val nextPage: Int? = null,
    val loadingMore: Boolean = false,
    // A failed «load more» must not wedge pagination forever: the footer
    // turns into an explicit retry affordance until it succeeds (F-013).
    val loadMoreFailed: Boolean = false,
    val searched: Boolean = false,
    val sort: SearchSort = SearchSort.BEST,
    // No query typed: the repo tab shows this week's trending repositories.
    val trendingMode: Boolean = false,
) {
    val hasMore: Boolean get() = nextPage != null
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val reposRepository: ReposRepository,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _sort = MutableStateFlow(SearchSort.BEST)
    val sort: StateFlow<SearchSort> = _sort.asStateFlow()

    private val _type = MutableStateFlow(SearchType.REPOS)
    val type: StateFlow<SearchType> = _type.asStateFlow()

    // Bumping this value re-runs the current search: StateFlows are combined
    // directly (NOT via snapshotFlow, which never re-reads them — F-001).
    private val _retry = MutableStateFlow(0)

    private val _results = MutableStateFlow<UiState<SearchUi>>(UiState.Content(SearchUi()))
    val results: StateFlow<UiState<SearchUi>> = _results.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                _query.debounce(600),
                _type,
                _sort,
                _retry,
            ) { query, type, sort, _ -> Triple(query, type, sort) }
                .collectLatest { (query, type, sort) ->
                    if (query.isBlank()) {
                        if (type == SearchType.REPOS) {
                            loadTrending(sort)
                        } else {
                            _results.value = UiState.Content(SearchUi(type = type, sort = sort))
                        }
                    } else {
                        // Fired once per settled query (after the debounce),
                        // not per keystroke (G-017).
                        telemetry.event("search_performed", mapOf("type" to type.name.lowercase()))
                        _results.value = UiState.Loading
                        _results.value = runSearch(query.trim(), type, sort, page = 1)
                    }
                }
        }
    }

    private suspend fun runSearch(
        query: String,
        type: SearchType,
        sort: SearchSort,
        page: Int,
    ): UiState<SearchUi> = safeCall {
        when (type) {
            SearchType.REPOS -> reposRepository.search(query = query, page = page, sort = sort.apiValue)
            SearchType.USERS -> reposRepository.searchUsers(query = query, page = page)
            SearchType.ISSUES -> reposRepository.searchIssues(query = query, page = page)
        }
    }.fold(
        onSuccess = { pageData ->
            UiState.Content(
                SearchUi(
                    query = query,
                    type = type,
                    repos = if (type == SearchType.REPOS) pageData.items.filterIsInstance<Repo>() else emptyList(),
                    users = if (type == SearchType.USERS) pageData.items.filterIsInstance<User>() else emptyList(),
                    issues = if (type == SearchType.ISSUES) pageData.items.filterIsInstance<Issue>() else emptyList(),
                    nextPage = pageData.nextPage,
                    searched = true,
                    sort = sort,
                ),
            )
        },
        onFailure = { t -> UiState.Error(ApiErrors.keyOf(t)) },
    )

    private suspend fun loadTrending(sort: SearchSort) {
        _results.value = UiState.Loading
        _results.value = safeCall { reposRepository.trending(page = 1) }.fold(
            onSuccess = { pageData ->
                UiState.Content(
                    SearchUi(
                        repos = pageData.items,
                        nextPage = pageData.nextPage,
                        sort = sort,
                        trendingMode = true,
                    ),
                )
            },
            onFailure = { t -> UiState.Error(ApiErrors.keyOf(t)) },
        )
    }

    fun onQueryChanged(value: String) {
        _query.value = value
    }

    fun setType(type: SearchType) {
        _type.value = type
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
            // Trending keeps sorting inside the repository call; queries of the
            // other types pass no sort chips in this build.
            val page = safeCall {
                when (current.type) {
                    SearchType.REPOS ->
                        if (current.trendingMode) {
                            reposRepository.trending(page = nextPage)
                        } else {
                            reposRepository.search(
                                query = current.query,
                                page = nextPage,
                                sort = current.sort.apiValue,
                            )
                        }
                    SearchType.USERS -> reposRepository.searchUsers(current.query, nextPage)
                    SearchType.ISSUES -> reposRepository.searchIssues(current.query, nextPage)
                }
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
                            repos = latest.repos + p.items.filterIsInstance<Repo>(),
                            users = latest.users + p.items.filterIsInstance<User>(),
                            issues = latest.issues + p.items.filterIsInstance<Issue>(),
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
