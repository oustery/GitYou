package com.gityou.app.ui.screens.mywork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.remote.dto.Issue
import com.gityou.app.data.repo.ReposRepository
import com.gityou.app.data.util.ApiErrors
import com.gityou.app.data.util.SearchQueries
import com.gityou.app.data.util.safeCall
import com.gityou.app.ui.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MyWorkTabUi(
    val items: List<Issue> = emptyList(),
    val nextPage: Int? = 1,
    val loadingMore: Boolean = false,
    val loadMoreFailed: Boolean = false,
)

data class MyWorkUi(
    val tab: SearchQueries.MyWorkTab = SearchQueries.MyWorkTab.ASSIGNED,
    val assigned: MyWorkTabUi = MyWorkTabUi(),
    val created: MyWorkTabUi = MyWorkTabUi(),
    val reviewRequested: MyWorkTabUi = MyWorkTabUi(),
) {
    fun current(): MyWorkTabUi = when (tab) {
        SearchQueries.MyWorkTab.ASSIGNED -> assigned
        SearchQueries.MyWorkTab.CREATED -> created
        SearchQueries.MyWorkTab.REVIEW_REQUESTED -> reviewRequested
    }

    fun withCurrent(update: MyWorkTabUi): MyWorkUi = when (tab) {
        SearchQueries.MyWorkTab.ASSIGNED -> copy(assigned = update)
        SearchQueries.MyWorkTab.CREATED -> copy(created = update)
        SearchQueries.MyWorkTab.REVIEW_REQUESTED -> copy(reviewRequested = update)
    }
}

/**
 * The "My work" hub (G-010): what is assigned to me, what I created, and what
 * waits for my review. Each tab keeps its own list and pagination state so
 * switching tabs never loses a scroll position or re-queries the API.
 */
@HiltViewModel
class MyWorkViewModel @Inject constructor(
    private val reposRepository: ReposRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<MyWorkUi>>(UiState.Content(MyWorkUi()))
    val state: StateFlow<UiState<MyWorkUi>> = _state.asStateFlow()

    /** Tab that has been fetched at least once; null until the first load. */
    private val loaded = mutableSetOf<SearchQueries.MyWorkTab>()

    init {
        load(SearchQueries.MyWorkTab.ASSIGNED)
    }

    fun selectTab(tab: SearchQueries.MyWorkTab) {
        update { it.copy(tab = tab) }
        if (tab !in loaded) load(tab)
    }

    /** Pull-to-refresh: re-fetches the visible tab from the first page. */
    fun refresh() {
        val tab = current()?.tab ?: SearchQueries.MyWorkTab.ASSIGNED
        load(tab)
    }

    fun retry() {
        refresh()
    }

    fun loadMore() {
        val ui = current() ?: return
        val tabUi = ui.current()
        val nextPage = tabUi.nextPage ?: return
        if (tabUi.loadingMore) return
        viewModelScope.launch {
            update { it.withCurrent(it.current().copy(loadingMore = true)) }
            val page = safeCall { reposRepository.myWork(ui.tab, page = nextPage) }
            // A refresh or tab switch may have replaced the list meanwhile.
            val latest = current()?.takeIf { it.tab == ui.tab } ?: return@launch
            val currentPage = latest.current()
            update {
                it.withCurrent(
                    page.fold(
                        onSuccess = { p ->
                            currentPage.copy(
                                items = currentPage.items + p.items,
                                nextPage = p.nextPage,
                                loadingMore = false,
                                loadMoreFailed = false,
                            )
                        },
                        onFailure = {
                            currentPage.copy(loadingMore = false, loadMoreFailed = true)
                        },
                    ),
                )
            }
        }
    }

    private fun load(tab: SearchQueries.MyWorkTab) {
        viewModelScope.launch {
            // Only replace the whole screen on the first fetch; later refreshes
            // keep the list visible instead of flashing a spinner.
            if (tab !in loaded) {
                _state.value = UiState.Loading
            } else {
                update { it.withCurrent(it.current().copy(loadingMore = false)) }
            }
            val result = safeCall { reposRepository.myWork(tab, page = 1) }
            result.fold(
                onSuccess = { page ->
                    loaded += tab
                    _state.value = UiState.Content(
                        (current() ?: MyWorkUi()).withCurrent(
                            MyWorkTabUi(items = page.items, nextPage = page.nextPage),
                        ),
                    )
                },
                onFailure = { t ->
                    // A failed refresh keeps the previously loaded list.
                    if (tab in loaded) {
                        update { it.withCurrent(it.current().copy(loadingMore = false)) }
                    } else {
                        _state.value = UiState.Error(ApiErrors.keyOf(t))
                    }
                },
            )
        }
    }

    private fun current(): MyWorkUi? = (_state.value as? UiState.Content)?.value

    private fun update(block: (MyWorkUi) -> MyWorkUi) {
        current()?.let { _state.value = UiState.Content(block(it)) }
    }
}
