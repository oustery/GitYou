package com.gityou.app.ui.screens.issue

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.remote.dto.Comment
import com.gityou.app.data.repo.IssuesRepository
import com.gityou.app.data.util.ApiErrors
import com.gityou.app.data.util.safeCall
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

    // One-shot success flag: the UI clears the draft only after the comment
    // was actually accepted, so a network failure never eats typed text (F-008).
    private val _commentSent = MutableStateFlow(false)
    val commentSent: StateFlow<Boolean> = _commentSent.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val detailResult = safeCall {
                if (isPull) {
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
            }
            val detail = detailResult.getOrNull()
            if (detail == null) {
                _state.value = UiState.Error(ApiErrors.keyOf(detailResult.exceptionOrNull()!!))
                return@launch
            }
            _state.value = UiState.Content(IssueDetailScreenState(detail = detail, commentsLoading = true))
            loadComments()
        }
    }

    private suspend fun loadComments() {
        val result = safeCall { issuesRepository.comments(owner, repoName, number) }
        (_state.value as? UiState.Content)?.let { content ->
            _state.value = UiState.Content(
                content.value.copy(
                    detail = content.value.detail.copy(comments = result.getOrDefault(content.value.detail.comments)),
                    commentsLoading = false,
                ),
            )
        }
    }

    fun addComment(body: String) {
        if (body.isBlank()) return
        viewModelScope.launch {
            _commentError.value = false
            patch { it.copy(sendingComment = true) }
            val result = safeCall {
                issuesRepository.addComment(owner, repoName, number, body.trim())
            }
            result.fold(
                onSuccess = { comment ->
                    patch {
                        it.copy(
                            sendingComment = false,
                            detail = it.detail.copy(
                                comments = it.detail.comments + comment,
                                commentCount = it.detail.commentCount + 1,
                            ),
                        )
                    }
                    _commentSent.value = true
                },
                onFailure = { _ ->
                    patch { it.copy(sendingComment = false) }
                    // The draft stays in the field on purpose.
                    _commentError.value = true
                },
            )
        }
    }

    fun consumeCommentSent() {
        _commentSent.value = false
    }

    private fun patch(update: (IssueDetailScreenState) -> IssueDetailScreenState) {
        (_state.value as? UiState.Content)?.let { content ->
            _state.value = UiState.Content(update(content.value))
        }
    }
}
