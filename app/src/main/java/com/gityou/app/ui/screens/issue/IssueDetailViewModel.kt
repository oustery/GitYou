package com.gityou.app.ui.screens.issue

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.R
import com.gityou.app.data.remote.dto.Comment
import com.gityou.app.data.remote.dto.Label
import com.gityou.app.data.remote.dto.PrReview
import com.gityou.app.data.remote.dto.ReactionsSummary
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
    // A management action (state/labels/comment/review) is in flight.
    val busy: Boolean = false,
    val reviews: List<PrReview> = emptyList(),
    /** Full label palette of the repository, for the label picker dialog. */
    val availableLabels: List<Label> = emptyList(),
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

    private val _managementMessage = MutableStateFlow<Int?>(null)
    val managementMessage: StateFlow<Int?> = _managementMessage.asStateFlow()

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
            if (isPull) loadReviews()
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

    private suspend fun loadReviews() {
        val result = safeCall { issuesRepository.reviews(owner, repoName, number) }
        result.getOrNull()?.let { reviews ->
            patch { it.copy(reviews = reviews) }
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

    fun consumeManagementMessage() {
        _managementMessage.value = null
    }

    // ---- Management: state, labels, comments --------------------------------

    /** Closes an open issue/PR, reopens a closed one. */
    fun toggleIssueState() {
        val current = (_state.value as? UiState.Content)?.value ?: return
        val target = if (current.detail.state == "open") "closed" else "open"
        viewModelScope.launch {
            patchBusy(true)
            val result = safeCall { issuesRepository.setIssueState(owner, repoName, number, target) }
            result.fold(
                onSuccess = { updated ->
                    patch {
                        it.copy(busy = false, detail = it.detail.copy(state = updated.state))
                    }
                    _managementMessage.value = R.string.issue_state_changed
                },
                onFailure = {
                    patchBusy(false)
                    _managementMessage.value = R.string.error_generic_action
                },
            )
        }
    }

    /** Loads the repository label palette once, for the picker dialog. */
    fun loadAvailableLabels() {
        val current = (_state.value as? UiState.Content)?.value ?: return
        if (current.availableLabels.isNotEmpty()) return
        viewModelScope.launch {
            val result = safeCall { issuesRepository.repoLabels(owner, repoName) }
            result.getOrNull()?.let { labels -> patch { it.copy(availableLabels = labels) } }
        }
    }

    /** Replaces the whole label set with the picker's selection. */
    fun applyLabelSelection(selected: List<String>) {
        viewModelScope.launch {
            patchBusy(true)
            val result = safeCall {
                issuesRepository.replaceIssueLabels(owner, repoName, number, selected)
            }
            result.fold(
                onSuccess = { labels ->
                    patch {
                        it.copy(
                            busy = false,
                            detail = it.detail.copy(labels = labels.map { label -> label.name }),
                        )
                    }
                },
                onFailure = {
                    patchBusy(false)
                    _managementMessage.value = R.string.error_generic_action
                },
            )
        }
    }

    fun editComment(commentId: Long, body: String) {
        if (body.isBlank()) return
        viewModelScope.launch {
            patchBusy(true)
            val result = safeCall { issuesRepository.editComment(commentId, body.trim()) }
            result.fold(
                onSuccess = { updated ->
                    patch {
                        it.copy(
                            busy = false,
                            detail = it.detail.copy(
                                comments = it.detail.comments.map { c ->
                                    if (c.id == commentId) updated else c
                                },
                            ),
                        )
                    }
                    _managementMessage.value = R.string.comment_updated
                },
                onFailure = {
                    patchBusy(false)
                    _managementMessage.value = R.string.error_generic_action
                },
            )
        }
    }

    fun deleteComment(commentId: Long) {
        viewModelScope.launch {
            patchBusy(true)
            val result = safeCall { issuesRepository.deleteComment(commentId) }
            result.fold(
                onSuccess = {
                    patch {
                        it.copy(
                            busy = false,
                            detail = it.detail.copy(
                                comments = it.detail.comments.filterNot { c -> c.id == commentId },
                                commentCount = (it.detail.commentCount - 1).coerceAtLeast(0),
                            ),
                        )
                    }
                    _managementMessage.value = R.string.comment_deleted
                },
                onFailure = {
                    patchBusy(false)
                    _managementMessage.value = R.string.error_generic_action
                },
            )
        }
    }

    // ---- Reviews and reactions ----------------------------------------------

    /** Submits APPROVE / REQUEST_CHANGES / COMMENT with an optional summary. */
    fun submitReview(event: String, body: String?) {
        viewModelScope.launch {
            patchBusy(true)
            val result = safeCall {
                issuesRepository.submitReview(owner, repoName, number, event, body?.takeIf { it.isNotBlank() })
            }
            result.fold(
                onSuccess = {
                    patchBusy(false)
                    _managementMessage.value = R.string.review_submitted
                    loadReviews()
                },
                onFailure = {
                    patchBusy(false)
                    _managementMessage.value = R.string.error_generic_action
                },
            )
        }
    }

    /** Posts a reaction and bumps the local counter optimistically. */
    fun reactToComment(commentId: Long, content: String) {
        viewModelScope.launch {
            val result = safeCall { issuesRepository.react(commentId, content) }
            result.getOrNull()?.let {
                patch { screen ->
                    screen.copy(
                        detail = screen.detail.copy(
                            comments = screen.detail.comments.map { comment ->
                                if (comment.id != commentId) {
                                    comment
                                } else {
                                    comment.copy(reactions = bumpReaction(comment.reactions, content))
                                }
                            },
                        ),
                    )
                }
            }
        }
    }

    private fun bumpReaction(current: ReactionsSummary?, content: String): ReactionsSummary {
        val base = current ?: ReactionsSummary()
        return when (content) {
            "+1" -> base.copy(totalCount = base.totalCount + 1, plusOne = base.plusOne + 1)
            "-1" -> base.copy(totalCount = base.totalCount + 1, minusOne = base.minusOne + 1)
            "laugh" -> base.copy(totalCount = base.totalCount + 1, laugh = base.laugh + 1)
            "hooray" -> base.copy(totalCount = base.totalCount + 1, hooray = base.hooray + 1)
            "confused" -> base.copy(totalCount = base.totalCount + 1, confused = base.confused + 1)
            "heart" -> base.copy(totalCount = base.totalCount + 1, heart = base.heart + 1)
            "rocket" -> base.copy(totalCount = base.totalCount + 1, rocket = base.rocket + 1)
            "eyes" -> base.copy(totalCount = base.totalCount + 1, eyes = base.eyes + 1)
            else -> base
        }
    }

    private fun patchBusy(busy: Boolean) {
        patch { it.copy(busy = busy) }
    }

    private fun patch(update: (IssueDetailScreenState) -> IssueDetailScreenState) {
        (_state.value as? UiState.Content)?.let { content ->
            _state.value = UiState.Content(update(content.value))
        }
    }
}
