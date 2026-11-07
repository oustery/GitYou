package com.gityou.app.ui.screens.repo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.R
import com.gityou.app.data.remote.dto.ContentItem
import com.gityou.app.data.remote.dto.Issue
import com.gityou.app.data.remote.dto.PullRequest
import com.gityou.app.data.remote.dto.Repo
import com.gityou.app.data.repo.ReposRepository
import com.gityou.app.data.repo.ReposRepository.FilePreviewException
import com.gityou.app.data.util.ApiErrors
import com.gityou.app.data.util.safeCall
import com.gityou.app.ui.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IssueListUi(
    val filter: String = "open",
    val items: List<Issue> = emptyList(),
    val nextPage: Int? = 1,
    val loadingMore: Boolean = false,
    // Keeps pagination retryable instead of permanently wedged (F-013).
    val loadMoreFailed: Boolean = false,
)

data class PullListUi(
    val filter: String = "open",
    val items: List<PullRequest> = emptyList(),
    val nextPage: Int? = 1,
    val loadingMore: Boolean = false,
    val loadMoreFailed: Boolean = false,
)

data class FileContentUi(
    val name: String,
    val path: String = "",
    val size: Long = 0,
    val text: String,
)

data class RepoDetailUi(
    val repo: Repo,
    val readme: UiState<String> = UiState.Loading,
    val branches: List<String> = emptyList(),
    val selectedBranch: String? = null,
    val currentPath: String = "",
    val contents: List<ContentItem> = emptyList(),
    val contentsLoading: Boolean = false,
    val issues: IssueListUi = IssueListUi(),
    val pulls: PullListUi = PullListUi(),
    val isStarred: Boolean = false,
    val starBusy: Boolean = false,
    val selectedTab: Int = 0,
    val fileContent: FileContentUi? = null,
) {
    /** Branch the code browser is showing; falls back to the default one. */
    val activeBranch: String get() = selectedBranch ?: repo.defaultBranch
}

@HiltViewModel
class RepoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reposRepository: ReposRepository,
) : ViewModel() {

    val owner: String = savedStateHandle.get<String>("owner").orEmpty()
    val repoName: String = savedStateHandle.get<String>("name").orEmpty()

    private val _state = MutableStateFlow<UiState<RepoDetailUi>>(UiState.Loading)
    val state: StateFlow<UiState<RepoDetailUi>> = _state.asStateFlow()

    private val _actionMessage = MutableStateFlow<Int?>(null)
    val actionMessage: StateFlow<Int?> = _actionMessage.asStateFlow()

    init {
        load()
    }

    fun consumeActionMessage() {
        _actionMessage.value = null
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val repo = safeCall { reposRepository.repo(owner, repoName) }
            val loaded = repo.getOrNull()
            if (loaded == null) {
                _state.value = UiState.Error(ApiErrors.keyOf(repo.exceptionOrNull()!!))
                return@launch
            }
            val starred = safeCall { reposRepository.isStarred(owner, repoName) }
                .getOrDefault(false)
            _state.value = UiState.Content(RepoDetailUi(repo = loaded, isStarred = starred))
            loadReadme()
            loadContents("")
            loadIssues()
            loadPulls()
            loadBranches()
        }
    }

    fun selectTab(index: Int) = patch { it.copy(selectedTab = index) }

    private fun patch(update: (RepoDetailUi) -> RepoDetailUi) {
        (_state.value as? UiState.Content)?.let { content ->
            _state.value = UiState.Content(update(content.value))
        }
    }

    // ---- README ----

    /** Public so the inline README error can offer a retry. */
    fun loadReadme() {
        viewModelScope.launch {
            patch { it.copy(readme = UiState.Loading) }
            val newState = safeCall { reposRepository.readme(owner, repoName) }.fold(
                onSuccess = { UiState.Content(it) },
                onFailure = { t -> UiState.Error(ApiErrors.keyOf(t)) },
            )
            patch { it.copy(readme = newState) }
        }
    }

    // ---- Code ----

    private fun loadBranches() {
        viewModelScope.launch {
            val names = safeCall { reposRepository.branches(owner, repoName) }
                .getOrDefault(emptyList())
            patch { it.copy(branches = names) }
        }
    }

    fun selectBranch(name: String) {
        val current = (_state.value as? UiState.Content)?.value ?: return
        if (name == current.activeBranch) return
        patch { it.copy(selectedBranch = name) }
        loadContents(current.currentPath)
    }

    fun loadContents(path: String) {
        viewModelScope.launch {
            patch { it.copy(contentsLoading = true, currentPath = path) }
            val ref = (_state.value as? UiState.Content)?.value?.activeBranch
            val result = safeCall { reposRepository.contents(owner, repoName, path, ref) }
            patch { it.copy(contents = result.getOrDefault(emptyList()), contentsLoading = false) }
            if (result.isFailure) {
                _actionMessage.value = R.string.error_generic_action
            }
        }
    }

    fun navigateUpInCode() {
        val current = (_state.value as? UiState.Content)?.value?.currentPath ?: return
        if (current.isBlank()) return
        val parent = if (!current.contains('/')) "" else current.substringBeforeLast('/')
        loadContents(parent)
    }

    fun openFile(item: ContentItem) {
        viewModelScope.launch {
            val ref = (_state.value as? UiState.Content)?.value?.activeBranch
            val result = safeCall { reposRepository.fileContent(owner, repoName, item.path, ref) }
            result.fold(
                onSuccess = { text ->
                    patch {
                        it.copy(fileContent = FileContentUi(name = item.name, path = item.path, size = item.size, text = text))
                    }
                },
                onFailure = { t ->
                    _actionMessage.value = when ((t as? FilePreviewException)?.reason) {
                        FilePreviewException.Reason.TOO_LARGE -> R.string.error_file_too_large
                        FilePreviewException.Reason.BINARY -> R.string.error_file_binary
                        FilePreviewException.Reason.UNSUPPORTED -> R.string.error_file_unsupported
                        null -> R.string.error_generic_action
                    }
                },
            )
        }
    }

    fun dismissFile() = patch { it.copy(fileContent = null) }

    // ---- Issues ----

    fun setIssueFilter(filter: String) {
        patch { it.copy(issues = it.issues.copy(filter = filter, items = emptyList(), nextPage = 1, loadMoreFailed = false)) }
        loadIssues()
    }

    private fun loadIssues() {
        val filter = (_state.value as? UiState.Content)?.value?.issues?.filter ?: "open"
        viewModelScope.launch {
            val result = safeCall { reposRepository.issues(owner, repoName, filter, page = 1) }
            result.fold(
                onSuccess = { page ->
                    patch { it.copy(issues = it.issues.copy(items = page.items, nextPage = page.nextPage, loadMoreFailed = false)) }
                },
                onFailure = { _ -> _actionMessage.value = R.string.error_generic_action },
            )
        }
    }

    fun loadIssuesMore() {
        val current = (_state.value as? UiState.Content)?.value?.issues ?: return
        if (current.loadingMore || current.nextPage == null) return
        val nextPage = current.nextPage ?: return
        val filter = current.filter
        viewModelScope.launch {
            patch { it.copy(issues = it.issues.copy(loadingMore = true)) }
            val page = safeCall { reposRepository.issues(owner, repoName, filter, page = nextPage) }
            // Apply only while the request we started is still current (F-006):
            // a reload or filter switch resets `loadingMore` to false.
            val stillOurs = (_state.value as? UiState.Content)?.value?.issues?.loadingMore == true
            if (!stillOurs) return@launch
            patch {
                page.fold(
                    onSuccess = { p ->
                        it.copy(
                            issues = it.issues.copy(
                                items = it.issues.items + p.items,
                                nextPage = p.nextPage,
                                loadingMore = false,
                                loadMoreFailed = false,
                            ),
                        )
                    },
                    onFailure = { _ -> it.copy(issues = it.issues.copy(loadingMore = false, loadMoreFailed = true)) },
                )
            }
        }
    }

    // ---- Pull requests ----

    fun setPullFilter(filter: String) {
        patch { it.copy(pulls = it.pulls.copy(filter = filter, items = emptyList(), nextPage = 1, loadMoreFailed = false)) }
        loadPulls()
    }

    private fun loadPulls() {
        val filter = (_state.value as? UiState.Content)?.value?.pulls?.filter ?: "open"
        viewModelScope.launch {
            val result = safeCall { reposRepository.pulls(owner, repoName, filter, page = 1) }
            result.fold(
                onSuccess = { page ->
                    patch { it.copy(pulls = it.pulls.copy(items = page.items, nextPage = page.nextPage, loadMoreFailed = false)) }
                },
                onFailure = { _ -> _actionMessage.value = R.string.error_generic_action },
            )
        }
    }

    fun loadPullsMore() {
        val current = (_state.value as? UiState.Content)?.value?.pulls ?: return
        if (current.loadingMore || current.nextPage == null) return
        val nextPage = current.nextPage ?: return
        val filter = current.filter
        viewModelScope.launch {
            patch { it.copy(pulls = it.pulls.copy(loadingMore = true)) }
            val page = safeCall { reposRepository.pulls(owner, repoName, filter, page = nextPage) }
            val stillOurs = (_state.value as? UiState.Content)?.value?.pulls?.loadingMore == true
            if (!stillOurs) return@launch
            patch {
                page.fold(
                    onSuccess = { p ->
                        it.copy(
                            pulls = it.pulls.copy(
                                items = it.pulls.items + p.items,
                                nextPage = p.nextPage,
                                loadingMore = false,
                                loadMoreFailed = false,
                            ),
                        )
                    },
                    onFailure = { _ -> it.copy(pulls = it.pulls.copy(loadingMore = false, loadMoreFailed = true)) },
                )
            }
        }
    }

    // ---- Star & fork ----

    fun toggleStar() {
        val current = (_state.value as? UiState.Content)?.value ?: return
        if (current.starBusy) return
        viewModelScope.launch {
            patch { it.copy(starBusy = true) }
            val result = safeCall {
                if (current.isStarred) {
                    reposRepository.unstar(owner, repoName)
                } else {
                    reposRepository.star(owner, repoName)
                }
            }
            result.fold(
                onSuccess = {
                    patch {
                        if (!it.starBusy) {
                            // A reload happened meanwhile; its isStarred is authoritative.
                            it
                        } else {
                            val starred = !current.isStarred
                            it.copy(
                                isStarred = starred,
                                starBusy = false,
                                repo = it.repo.copy(
                                    stargazersCount = (it.repo.stargazersCount + (if (starred) 1 else -1)).coerceAtLeast(0),
                                ),
                            )
                        }
                    }
                },
                onFailure = { _ ->
                    patch { it.copy(starBusy = false) }
                    _actionMessage.value = R.string.error_generic_action
                },
            )
        }
    }

    fun fork() {
        viewModelScope.launch {
            safeCall { reposRepository.fork(owner, repoName) }.fold(
                onSuccess = { _actionMessage.value = R.string.repo_fork_created },
                onFailure = { _ -> _actionMessage.value = R.string.error_generic_action },
            )
        }
    }
}
