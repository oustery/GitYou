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

/** Pagination state of one tab. */
data class MyWorkTabUi(
    val items: List<Issue> = emptyList(),
    val nextPage: Int? = 1,
    /** A first-page request is in flight; drives the pull-to-refresh spinner. */
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val loadMoreFailed: Boolean = false,
    /** At least one successful fetch, so the list may be shown instead of a spinner. */
    val loadedOnce: Boolean = false,
)

data class MyWorkUi(
    val tab: SearchQueries.MyWorkTab = SearchQueries.MyWorkTab.ASSIGNED,
    val tabs: Map<SearchQueries.MyWorkTab, MyWorkTabUi> = emptyMap(),
) {
    fun current(): MyWorkTabUi = tabs[tab] ?: MyWorkTabUi()
}

/**
 * The "My work" hub (G-010): what is assigned to me, what I created, and what
 * waits for my review. Each tab keeps its own list and pagination cursor, so
 * switching tabs never loses state and never re-queries the same page.
 *
 * State is assembled from the tab map on every change instead of being patched
 * in place — a refresh that finishes after a tab switch must not write one
 * tab's issues into another tab's list.
 */
@HiltViewModel
class MyWorkViewModel @Inject constructor(
    private val reposRepository: ReposRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<MyWorkUi>>(UiState.Loading)
    val state: StateFlow<UiState<MyWorkUi>> = _state.asStateFlow()

    private var selectedTab = SearchQueries.MyWorkTab.ASSIGNED
    private val tabs = mutableMapOf<SearchQueries.MyWorkTab, MyWorkTabUi>()
    private val errors = mutableMapOf<SearchQueries.MyWorkTab, String>()

    init {
        load(selectedTab)
    }

    fun selectTab(tab: SearchQueries.MyWorkTab) {
        selectedTab = tab
        publish()
        if (tabs[tab]?.loadedOnce != true) load(tab)
    }

    /** Pull-to-refresh: re-fetches the visible tab from the first page. */
    fun refresh() {
        load(selectedTab)
    }

    fun retry() {
        errors.remove(selectedTab)
        load(selectedTab)
    }

    fun loadMore() {
        val tab = selectedTab
        val tabUi = tabUi(tab)
        val nextPage = tabUi.nextPage ?: return
        if (tabUi.loadingMore || tabUi.refreshing) return
        update(tab) { it.copy(loadingMore = true, loadMoreFailed = false) }
        viewModelScope.launch {
            val page = safeCall { reposRepository.myWork(tab, page = nextPage) }
            // A refresh of the same tab may have replaced the list meanwhile.
            val latest = tabUi(tab)
            update(tab) {
                page.fold(
                    onSuccess = { p ->
                        latest.copy(
                            items = latest.items + p.items,
                            nextPage = p.nextPage,
                            loadingMore = false,
                            loadMoreFailed = false,
                        )
                    },
                    onFailure = { latest.copy(loadingMore = false, loadMoreFailed = true) },
                )
            }
        }
    }

    private fun load(tab: SearchQueries.MyWorkTab) {
        if (tabUi(tab).refreshing) return
        errors.remove(tab)
        update(tab) { it.copy(refreshing = true, loadMoreFailed = false) }
        viewModelScope.launch {
            val result = safeCall { reposRepository.myWork(tab, page = 1) }
            result.fold(
                onSuccess = { page ->
                    update(tab) {
                        MyWorkTabUi(
                            items = page.items,
                            nextPage = page.nextPage,
                            refreshing = false,
                            loadedOnce = true,
                        )
                    }
                },
                onFailure = { t ->
                    update(tab) { it.copy(refreshing = false) }
                    // A failed refresh keeps the list that is already on screen;
                    // only the first fetch of a tab can empty the screen.
                    if (!tabUi(tab).loadedOnce) errors[tab] = ApiErrors.keyOf(t)
                    publish()
                },
            )
        }
    }

    private fun tabUi(tab: SearchQueries.MyWorkTab): MyWorkTabUi = tabs[tab] ?: MyWorkTabUi()

    private fun update(tab: SearchQueries.MyWorkTab, block: (MyWorkTabUi) -> MyWorkTabUi) {
        tabs[tab] = block(tabUi(tab))
        publish()
    }

    private fun publish() {
        val visible = tabUi(selectedTab)
        val error = errors[selectedTab]
        _state.value = when {
            visible.loadedOnce -> UiState.Content(
                MyWorkUi(tab = selectedTab, tabs = tabs.toMap()),
            )
            error != null -> UiState.Error(error)
            else -> UiState.Loading
        }
    }
}
