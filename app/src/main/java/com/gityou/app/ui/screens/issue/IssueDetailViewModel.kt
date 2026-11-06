package com.gityou.app.ui.screens.issue

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.remote.dto.Comment
import com.gityou.app.data.repo.IssuesRepository
import com.gityou.app.data.util.ApiErrors
import com.gityou.app.ui.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Unified issue / pull request detail state. */
data class IssueDetailUi(
    val isPull: Boolean,
    val number: Int,
    val title: String,
    val state: String,
    val body: String?,
    val authorLogin: String,
    val authorAvatar: String?,
    val createdAt: String?,
    val labels: List<String>,
    val commentCount: Int,
    val comments: List<Comment> = emptyList(),
    // Pull-request-only fields
    val draft: Boolean = false,
    val merged: Boolean = false,
    val additions: Int = 0,
    val deletions: Int = 0,
    val changedFiles: Int = 0,
    val headRef: String? = null,
    val baseRef: String? = null,
)

data class IssueDetailScreenState(
    val detail: IssueDetailUi,
    val commentsLoading: Boolean = false,
    val sendingComment: Boolean = false,
)

@HiltViewModel
class IssueDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val issuesRepository: IssuesRepository,
) : ViewModel() {

    val owner: String = savedStateHandle.get<String>("owner").orEmpty()
    val repoName: String = savedStateHandle.get<String>("name").orEmpty()
    val number: Int = savedStateHandle.get<Int>("number") ?: 0
    val isPull: Boolean = savedStateHandle.get<Boolean>("isPull") ?: false

    private val _state = MutableStateFlow<UiState<IssueDetailScreenState>>(UiState.Loading)
    val state: StateFlow<UiState<IssueDetailScreenState>> = _state.asStateFlow()

    private val _commentError = MutableStateFlow(false)
    val commentError: StateFlow<Boolean> = _commentError.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                val detail = if (isPull) {
                    issuesRepository.pullRequest(owner, repoName, number).let { pull ->
                        IssueDetailUi(
                            isPull = true,
                            number = pull.number,
                            title = pull.title,
                            state = pull.state,
                            body = pull.body,
                            authorLogin = pull.user?.login ?: "",
                            authorAvatar = pull.user?.avatarUrl,
                            createdAt = pull.createdAt,
                            labels = pull.labels.map { it.name },
                            commentCount = pull.commentCount,
                            draft = pull.draft,
                            merged = pull.merged,
                            additions = pull.additions,
                            deletions = pull.deletions,
                            changedFiles = pull.changedFiles,
                            headRef = pull.head?.ref,
                            baseRef = pull.base?.ref,
                        )
                    }
                } else {
                    issuesRepository.issue(owner, repoName, number).let { issue ->
                        IssueDetailUi(
                            isPull = false,
                            number = issue.number,
                            title = issue.title,
                            state = issue.state,
                            body = issue.body,
                            authorLogin = issue.user?.login ?: "",
                            authorAvatar = issue.user?.avatarUrl,
                            createdAt = issue.createdAt,
                            labels = issue.labels.map { it.name },
                            commentCount = issue.commentCount,
                        )
                    }
                }
                _state.value = UiState.Content(IssueDetailScreenState(detail = detail, commentsLoading = true))
                loadComments()
            } catch (t: Throwable) {
                _state.value = UiState.Error(ApiErrors.keyOf(t))
            }
        }
    }

    private suspend fun loadComments() {
        try {
            val comments = issuesRepository.comments(owner, repoName, number)
            (_state.value as? UiState.Content)?.let { content ->
                _state.value = UiState.Content(
                    content.value.copy(detail = content.value.detail.copy(comments = comments), commentsLoading = false),
                )
            }
        } catch (t: Throwable) {
            (_state.value as? UiState.Content)?.let { content ->
                _state.value = UiState.Content(content.value.copy(commentsLoading = false))
            }
        }
    }

    fun addComment(body: String) {
        if (body.isBlank()) return
        viewModelScope.launch {
            _commentError.value = false
            patch { it.copy(sendingComment = true) }
            try {
                val comment = issuesRepository.addComment(owner, repoName, number, body.trim())
                patch {
                    it.copy(
                        sendingComment = false,
                        detail = it.detail.copy(
                            comments = it.detail.comments + comment,
                            commentCount = it.detail.commentCount + 1,
                        ),
                    )
                }
            } catch (t: Throwable) {
                patch { it.copy(sendingComment = false) }
                _commentError.value = true
            }
        }
    }

    private fun patch(update: (IssueDetailScreenState) -> IssueDetailScreenState) {
        (_state.value as? UiState.Content)?.let { content ->
            _state.value = UiState.Content(update(content.value))
        }
    }
}
