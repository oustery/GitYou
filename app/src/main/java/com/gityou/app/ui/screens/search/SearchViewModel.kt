package com.gityou.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.remote.dto.Repo
import com.gityou.app.data.repo.ReposRepository
import com.gityou.app.data.util.ApiErrors
import com.gityou.app.ui.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch

data class SearchUi(
    val query: String = "",
    val repos: List<Repo> = emptyList(),
    val nextPage: Int? = null,
    val loadingMore: Boolean = false,
    val searched: Boolean = false,
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

    private val _results = MutableStateFlow<UiState<SearchUi>>(UiState.Content(SearchUi()))
    val results: StateFlow<UiState<SearchUi>> = _results.asStateFlow()

    init {
        viewModelScope.launch {
            snapshotFlow { _query.value }
                .debounce(600)
                .collectLatest { query ->
                    if (query.isBlank()) {
                        _results.value = UiState.Content(SearchUi())
                    } else {
                        _results.value = UiState.Loading
                        _results.value = try {
                            val pageData = reposRepository.search(query.trim(), page = 1)
                            UiState.Content(
                                SearchUi(
                                    query = query.trim(),
                                    repos = pageData.items,
                                    nextPage = pageData.nextPage,
                                    searched = true,
                                ),
                            )
                        } catch (t: Throwable) {
                            UiState.Error(ApiErrors.keyOf(t))
                        }
                    }
                }
        }
    }

    fun onQueryChanged(value: String) {
        _query.value = value
    }

    fun loadMore() {
        val current = (_results.value as? UiState.Content)?.value ?: return
        if (current.loadingMore || current.nextPage == null) return
        viewModelScope.launch {
            _results.value = UiState.Content(current.copy(loadingMore = true))
            _results.value = try {
                val page = reposRepository.search(current.query, current.nextPage)
                UiState.Content(
                    current.copy(
                        repos = current.repos + page.items,
                        nextPage = page.nextPage,
                        loadingMore = false,
                    ),
                )
            } catch (t: Throwable) {
                UiState.Content(current.copy(loadingMore = false))
            }
        }
    }
}
