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
import com.gityou.app.data.util.ApiErrors
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
)

data class PullListUi(
    val filter: String = "open",
    val items: List<PullRequest> = emptyList(),
    val nextPage: Int? = 1,
    val loadingMore: Boolean = false,
)

data class FileContentUi(
    val name: String,
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
            try {
                val repo = reposRepository.repo(owner, repoName)
                val starred = runCatching { reposRepository.isStarred(owner, repoName) }
                    .getOrDefault(false)
                _state.value = UiState.Content(RepoDetailUi(repo = repo, isStarred = starred))
                loadReadme()
                loadContents("")
                loadIssues()
                loadPulls()
                loadBranches()
            } catch (t: Throwable) {
                _state.value = UiState.Error(ApiErrors.keyOf(t))
            }
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
            val newState = try {
                UiState.Content(reposRepository.readme(owner, repoName))
            } catch (t: Throwable) {
                UiState.Error(ApiErrors.keyOf(t))
            }
            patch { it.copy(readme = newState) }
        }
    }

    // ---- Code ----

    private fun loadBranches() {
        viewModelScope.launch {
            val names = runCatching { reposRepository.branches(owner, repoName) }
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
            try {
                val ref = (_state.value as? UiState.Content)?.value?.activeBranch
                val items = reposRepository.contents(owner, repoName, path, ref)
                patch { it.copy(contents = items, contentsLoading = false) }
            } catch (t: Throwable) {
                patch { it.copy(contents = emptyList(), contentsLoading = false) }
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
            try {
                val ref = (_state.value as? UiState.Content)?.value?.activeBranch
                val text = reposRepository.fileContent(owner, repoName, item.path, ref)
                patch { it.copy(fileContent = FileContentUi(name = item.name, text = text)) }
            } catch (t: Throwable) {
                _actionMessage.value = R.string.error_generic_action
            }
        }
    }

    fun dismissFile() = patch { it.copy(fileContent = null) }

    // ---- Issues ----

    fun setIssueFilter(filter: String) {
        patch { it.copy(issues = it.issues.copy(filter = filter, items = emptyList(), nextPage = 1)) }
        loadIssues()
    }

    private fun loadIssues() {
        val filter = (_state.value as? UiState.Content)?.value?.issues?.filter ?: "open"
        viewModelScope.launch {
            try {
                val page = reposRepository.issues(owner, repoName, filter, page = 1)
                patch { it.copy(issues = it.issues.copy(items = page.items, nextPage = page.nextPage)) }
            } catch (t: Throwable) {
                _actionMessage.value = R.string.error_generic_action
            }
        }
    }

    fun loadIssuesMore() {
        val current = (_state.value as? UiState.Content)?.value?.issues ?: return
        if (current.loadingMore || current.nextPage == null) return
        viewModelScope.launch {
            patch { it.copy(issues = it.issues.copy(loadingMore = true)) }
            try {
                val page = reposRepository.issues(owner, repoName, current.filter, page = current.nextPage)
                patch {
                    it.copy(
                        issues = it.issues.copy(
                            items = it.issues.items + page.items,
                            nextPage = page.nextPage,
                            loadingMore = false,
                        ),
                    )
                }
            } catch (t: Throwable) {
                patch { it.copy(issues = it.issues.copy(loadingMore = false)) }
            }
        }
    }

    // ---- Pull requests ----

    fun setPullFilter(filter: String) {
        patch { it.copy(pulls = it.pulls.copy(filter = filter, items = emptyList(), nextPage = 1)) }
        loadPulls()
    }

    private fun loadPulls() {
        val filter = (_state.value as? UiState.Content)?.value?.pulls?.filter ?: "open"
        viewModelScope.launch {
            try {
                val page = reposRepository.pulls(owner, repoName, filter, page = 1)
                patch { it.copy(pulls = it.pulls.copy(items = page.items, nextPage = page.nextPage)) }
            } catch (t: Throwable) {
                _actionMessage.value = R.string.error_generic_action
            }
        }
    }

    fun loadPullsMore() {
        val current = (_state.value as? UiState.Content)?.value?.pulls ?: return
        if (current.loadingMore || current.nextPage == null) return
        viewModelScope.launch {
            patch { it.copy(pulls = it.pulls.copy(loadingMore = true)) }
            try {
                val page = reposRepository.pulls(owner, repoName, current.filter, page = current.nextPage)
                patch {
                    it.copy(
                        pulls = it.pulls.copy(
                            items = it.pulls.items + page.items,
                            nextPage = page.nextPage,
                            loadingMore = false,
                        ),
                    )
                }
            } catch (t: Throwable) {
                patch { it.copy(pulls = it.pulls.copy(loadingMore = false)) }
            }
        }
    }

    // ---- Star & fork ----

    fun toggleStar() {
        val current = (_state.value as? UiState.Content)?.value ?: return
        if (current.starBusy) return
        viewModelScope.launch {
            patch { it.copy(starBusy = true) }
            try {
                if (current.isStarred) {
                    reposRepository.unstar(owner, repoName)
                    patch {
                        it.copy(
                            isStarred = false,
                            starBusy = false,
                            repo = it.repo.copy(stargazersCount = (it.repo.stargazersCount - 1).coerceAtLeast(0)),
                        )
                    }
                } else {
                    reposRepository.star(owner, repoName)
                    patch {
                        it.copy(
                            isStarred = true,
                            starBusy = false,
                            repo = it.repo.copy(stargazersCount = it.repo.stargazersCount + 1),
                        )
                    }
                }
            } catch (t: Throwable) {
                patch { it.copy(starBusy = false) }
                _actionMessage.value = R.string.error_generic_action
            }
        }
    }

    fun fork() {
        viewModelScope.launch {
            try {
                reposRepository.fork(owner, repoName)
                _actionMessage.value = R.string.repo_fork_created
            } catch (t: Throwable) {
                _actionMessage.value = R.string.error_generic_action
            }
        }
    }
}
