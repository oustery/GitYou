package com.gityou.app.ui.screens.releases

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.remote.dto.Page
import com.gityou.app.data.remote.dto.Release
import com.gityou.app.data.remote.dto.Tag
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

data class ReleasesUi(
    val releases: List<Release> = emptyList(),
    val releasesNextPage: Int? = 1,
    val loadingReleases: Boolean = false,
    val releasesLoadMoreFailed: Boolean = false,
    val tags: List<Tag> = emptyList(),
    val tagsNextPage: Int? = 1,
    val loadingTags: Boolean = false,
    val tagsLoadMoreFailed: Boolean = false,
    val selectedTab: Int = 0,
) {
    val hasMore: Boolean get() = if (selectedTab == 0) releasesNextPage != null else tagsNextPage != null
    val loadingMore: Boolean get() = if (selectedTab == 0) loadingReleases else loadingTags
    val loadMoreFailed: Boolean get() = if (selectedTab == 0) releasesLoadMoreFailed else tagsLoadMoreFailed
}

/** Releases with their changelogs, plus the plain tag list. */
@HiltViewModel
class ReleasesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reposRepository: ReposRepository,
) : ViewModel() {

    val owner: String = savedStateHandle.get<String>("owner").orEmpty()
    val repoName: String = savedStateHandle.get<String>("name").orEmpty()

    private val _state = MutableStateFlow<UiState<ReleasesUi>>(UiState.Loading)
    val state: StateFlow<UiState<ReleasesUi>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = safeCall { reposRepository.releases(owner, repoName, page = 1) }.fold(
                onSuccess = { page ->
                    UiState.Content(
                        ReleasesUi(releases = page.items, releasesNextPage = page.nextPage),
                    )
                },
                onFailure = { t -> UiState.Error(ApiErrors.keyOf(t)) },
            )
        }
    }

    fun selectTab(index: Int) {
        (_state.value as? UiState.Content)?.let { content ->
            _state.value = UiState.Content(content.value.copy(selectedTab = index))
        }
        maybeLoadFirstTagPage(index)
    }

    /** Tags load lazily on the first visit of their tab. */
    private fun maybeLoadFirstTagPage(index: Int) {
        if (index != 1) return
        val current = (_state.value as? UiState.Content)?.value ?: return
        if (current.loadingTags) return
        // Already fetched or exhausted — nothing to request.
        if (current.tags.isNotEmpty() || current.tagsNextPage == null) return
        loadTags()
    }

    fun loadMore() {
        val current = (_state.value as? UiState.Content)?.value ?: return
        if (current.loadingMore || !current.hasMore) return
        if (current.selectedTab == 0) {
            val page = current.releasesNextPage ?: return
            _state.value = UiState.Content(current.copy(loadingReleases = true))
            viewModelScope.launch {
                val result = safeCall { reposRepository.releases(owner, repoName, page = page) }
                val latest = (_state.value as? UiState.Content)?.value
                if (latest == null || !latest.loadingReleases) return@launch
                _state.value = UiState.Content(
                    result.fold(
                        onSuccess = { p ->
                            latest.copy(
                                releases = latest.releases + p.items,
                                releasesNextPage = p.nextPage,
                                loadingReleases = false,
                                releasesLoadMoreFailed = false,
                            )
                        },
                        onFailure = { _ ->
                            latest.copy(loadingReleases = false, releasesLoadMoreFailed = true)
                        },
                    ),
                )
            }
        } else {
            loadTags()
        }
    }

    private fun loadTags() {
        val current = (_state.value as? UiState.Content)?.value ?: return
        if (current.loadingTags) return
        val page = current.tagsNextPage ?: return
        _state.value = UiState.Content(current.copy(loadingTags = true))
        viewModelScope.launch {
            val result = safeCall { reposRepository.tags(owner, repoName, page = page) }
            val latest = (_state.value as? UiState.Content)?.value
            if (latest == null || !latest.loadingTags) return@launch
            _state.value = UiState.Content(
                result.fold(
                    onSuccess = { p ->
                        latest.copy(
                            tags = latest.tags + p.items,
                            tagsNextPage = p.nextPage,
                            loadingTags = false,
                            tagsLoadMoreFailed = false,
                        )
                    },
                    onFailure = { _ ->
                        latest.copy(loadingTags = false, tagsLoadMoreFailed = true)
                    },
                ),
            )
        }
    }
}
