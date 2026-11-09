package com.gityou.app.ui.screens.issue

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.gityou.app.R
import com.gityou.app.data.remote.dto.Comment
import com.gityou.app.data.util.Formatters
import com.gityou.app.data.util.relativeOrAbsolute
import com.gityou.app.ui.components.EyebrowLabel
import com.gityou.app.ui.components.MarkdownText
import com.gityou.app.ui.components.StateScreen
import com.gityou.app.ui.components.relativeTimeString
import androidx.compose.foundation.clickable
import com.gityou.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IssueDetailScreen(
    number: Int,
    isPull: Boolean,
    onBack: () -> Unit,
    onOpenFiles: () -> Unit = {},
    onOpenUser: (String) -> Unit = {},
    viewModel: IssueDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val commentError by viewModel.commentError.collectAsStateWithLifecycle()
    val commentSent by viewModel.commentSent.collectAsStateWithLifecycle()
    val managementMessage by viewModel.managementMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Dialog control: at most one is visible at a time.
    var showLabelPicker by rememberSaveable { mutableStateOf(false) }
    var editingComment by remember { mutableStateOf<Comment?>(null) }
    var deletingComment by remember { mutableStateOf<Comment?>(null) }
    var reviewEvent by remember { mutableStateOf<String?>(null) }
    var showMergeSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(managementMessage) {
        managementMessage?.let {
            snackbarHostState.showSnackbar(context.getString(it))
            viewModel.consumeManagementMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isPull) {
                            stringResource(R.string.issue_detail_pull_title, number)
                        } else {
                            stringResource(R.string.issue_detail_issue_title, number)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (isPull) {
                        IconButton(onClick = onOpenFiles) {
                            Icon(
                                Icons.Filled.Description,
                                contentDescription = stringResource(R.string.pull_files_action, 0),
                            )
                        }
                    }
                    ManagementMenu(
                        viewModel = viewModel,
                        isPull = isPull,
                        onEditLabels = {
                            viewModel.loadAvailableLabels()
                            showLabelPicker = true
                        },
                        onReview = { event -> reviewEvent = event },
                        onMerge = { showMergeSheet = true },
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        StateScreen(state = state, onRetry = viewModel::load) { screenState ->
            // The draft survives every recomposition and is wiped only after
            // the server accepts the comment (F-008).
            var draft by rememberSaveable { mutableStateOf("") }

            LaunchedEffect(commentSent) {
                if (commentSent) {
                    draft = ""
                    viewModel.consumeCommentSent()
                }
            }

            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // One scrollable list: the issue body and the comments share
                // the same LazyColumn, so a long body never pushes the
                // comments (or itself) off screen (F-009).
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                ) {
                    item(key = "header") { IssueHeader(screenState.detail, onOpenUser) }
                    if (isPull && screenState.reviews.isNotEmpty()) {
                        item(key = "reviews") { ReviewsSummaryCard(screenState.reviews) }
                    }
                    item(key = "comments-label") {
                        EyebrowLabel(
                            stringResource(R.string.issue_comments_section, screenState.detail.commentCount),
                        )
                    }
                    if (screenState.commentsLoading) {
                        item(key = "comments-loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.xl),
                                contentAlignment = Alignment.Center,
                            ) {
                                LinearWavyProgressIndicator()
                            }
                        }
                    }
                    itemsIndexed(
                        screenState.detail.comments,
                        key = { index, comment ->
                            // Fallback keeps keys unique even if a comment DTO
                            // arrives with the default id = 0 (F-014).
                            if (comment.id != 0L) comment.id else "comment-$index"
                        },
                    ) { _, comment ->
                        CommentCard(
                            comment = comment,
                            onEdit = { editingComment = comment },
                            onDelete = { deletingComment = comment },
                            onReact = { content -> viewModel.reactToComment(comment.id, content) },
                            onOpenUser = onOpenUser,
                        )
                        Spacer(Modifier.height(Spacing.xs))
                    }
                    if (!screenState.commentsLoading && screenState.detail.comments.isEmpty()) {
                        item(key = "no-comments") {
                            Text(
                                stringResource(R.string.issue_no_comments),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = Spacing.lg),
                            )
                        }
                    }
                }

                if (commentError) {
                    Text(
                        stringResource(R.string.error_generic_action),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = Spacing.lg),
                    )
                }

                Surface(tonalElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            placeholder = { Text(stringResource(R.string.issue_comment_hint)) },
                            modifier = Modifier.weight(1f),
                            maxLines = 4,
                        )
                        Spacer(Modifier.width(Spacing.md))
                        Button(
                            onClick = { viewModel.addComment(draft) },
                            enabled = draft.isNotBlank() && !screenState.sendingComment,
                        ) {
                            if (screenState.sendingComment) {
                                CircularWavyProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                )
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(R.string.issue_send_comment),
                                )
                            }
                        }
                    }
                }
            }

            if (showMergeSheet) {
                MergeDialog(
                    detail = screenState.detail,
                    merging = screenState.merging,
                    onConfirm = { method ->
                        showMergeSheet = false
                        viewModel.merge(method)
                    },
                    onDismiss = { showMergeSheet = false },
                )
            }
            if (showLabelPicker) {
                LabelPickerDialog(
                    available = screenState.availableLabels,
                    selected = screenState.detail.labels,
                    busy = screenState.busy,
                    onConfirm = { selection ->
                        showLabelPicker = false
                        viewModel.applyLabelSelection(selection)
                    },
                    onDismiss = { showLabelPicker = false },
                )
            }
            editingComment?.let { comment ->
                EditCommentDialog(
                    initial = comment.body.orEmpty(),
                    busy = screenState.busy,
                    onConfirm = { body ->
                        editingComment = null
                        viewModel.editComment(comment.id, body)
                    },
                    onDismiss = { editingComment = null },
                )
            }
            deletingComment?.let { comment ->
                AlertDialog(
                    onDismissRequest = { deletingComment = null },
                    title = { Text(stringResource(R.string.comment_delete_confirm)) },
                    confirmButton = {
                        TextButton(
                            enabled = !screenState.busy,
                            onClick = {
                                deletingComment = null
                                viewModel.deleteComment(comment.id)
                            },
                        ) { Text(stringResource(R.string.comment_delete)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { deletingComment = null }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    },
                )
            }
            reviewEvent?.let { event ->
                ReviewDialog(
                    event = event,
                    busy = screenState.busy,
                    onConfirm = { body ->
                        reviewEvent = null
                        viewModel.submitReview(event, body)
                    },
                    onDismiss = { reviewEvent = null },
                )
            }
        }
    }
}

/** Top-bar overflow: state changes, label editing and pull-request reviews. */
@Composable
private fun ManagementMenu(
    viewModel: IssueDetailViewModel,
    isPull: Boolean,
    onEditLabels: () -> Unit,
    onReview: (String) -> Unit,
    onMerge: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    val detail = (state as? com.gityou.app.ui.components.UiState.Content)?.value?.detail

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.issue_edit_labels)) },
                onClick = {
                    expanded = false
                    onEditLabels()
                },
            )
            if (detail != null) {
                DropdownMenuItem(
                    text = {
                        Text(
                            if (detail.state == "open") {
                                stringResource(R.string.issue_action_close)
                            } else {
                                stringResource(R.string.issue_action_reopen)
                            },
                        )
                    },
                    onClick = {
                        expanded = false
                        viewModel.toggleIssueState()
                    },
                )
            }
            if (isPull && detail?.state == "open" && !detail.merged) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.pr_merge_action)) },
                    onClick = {
                        expanded = false
                        onMerge()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.review_approve)) },
                    onClick = {
                        expanded = false
                        onReview("APPROVE")
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.review_request_changes)) },
                    onClick = {
                        expanded = false
                        onReview("REQUEST_CHANGES")
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IssueHeader(detail: IssueDetailUi, onOpenUser: (String) -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (container, content) = when {
                detail.merged -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                detail.state == "closed" -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
            }
            val statusLabel = when {
                detail.merged -> stringResource(R.string.issue_state_merged)
                detail.state == "closed" -> stringResource(R.string.issue_state_closed)
                detail.draft -> stringResource(R.string.issue_state_draft)
                else -> stringResource(R.string.issue_state_open)
            }
            val icon = when {
                detail.merged -> Icons.Filled.MergeType
                detail.state == "closed" -> Icons.Filled.Check
                else -> Icons.Filled.BugReport
            }
            Surface(shape = MaterialTheme.shapes.small, color = container) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(statusLabel, style = MaterialTheme.typography.labelMedium, color = content)
                }
            }
            Spacer(Modifier.weight(1f))
            if (detail.isPull) {
                Text(
                    "+${detail.additions} / −${detail.deletions}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Spacing.sm))
        Text(
            detail.title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(Spacing.md))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(enabled = detail.authorLogin.isNotBlank()) {
                onOpenUser(detail.authorLogin)
            },
        ) {
            AsyncImage(
                model = detail.authorAvatar,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                stringResource(
                    R.string.issue_author_line,
                    detail.authorLogin,
                    relativeTimeString(Formatters.relativeOrAbsolute(detail.createdAt)),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (detail.isPull && detail.headRef != null && detail.baseRef != null) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                stringResource(R.string.issue_branches_line, detail.baseRef ?: "", detail.headRef ?: ""),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (detail.labels.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.md))
            // FlowRow wraps instead of overflowing past the screen edge (F-022).
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                detail.labels.forEach { label ->
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }

        detail.body?.takeIf { it.isNotBlank() }?.let { body ->
            Spacer(Modifier.height(Spacing.lg))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                MarkdownText(
                    markdown = body,
                    modifier = Modifier.padding(Spacing.lg),
                )
            }
        }
    }
}

/** Latest review verdict per reviewer, as compact chips under the body. */
@Composable
private fun ReviewsSummaryCard(reviews: List<com.gityou.app.data.remote.dto.PrReview>) {
    val latest = remember(reviews) {
        reviews
            .filter { it.state == "APPROVED" || it.state == "CHANGES_REQUESTED" }
            .groupBy { it.user?.login.orEmpty() }
            .mapNotNull { (_, group) -> group.maxByOrNull { it.submittedAt.orEmpty() } }
    }
    if (latest.isEmpty()) return
    Column(modifier = Modifier.padding(bottom = Spacing.sm)) {
        EyebrowLabel(stringResource(R.string.review_section))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            latest.forEach { review ->
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = if (review.state == "APPROVED") {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                ) {
                    Text(
                        "${review.user?.login.orEmpty()} · " +
                            stringResource(reviewStateLabel(review.state)),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (review.state == "APPROVED") {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

private fun reviewStateLabel(state: String?): Int = when (state) {
    "APPROVED" -> R.string.review_state_APPROVED
    "CHANGES_REQUESTED" -> R.string.review_state_CHANGES_REQUESTED
    else -> R.string.review_state_COMMENTED
}

/** Multi-select dialog replacing the issue's label set on confirm. */
@Composable
private fun LabelPickerDialog(
    available: List<com.gityou.app.data.remote.dto.Label>,
    selected: List<String>,
    busy: Boolean,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val selection = remember(available, selected) { selected.toMutableStateList() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.issue_edit_labels)) },
        text = {
            if (available.isEmpty()) {
                Text(stringResource(R.string.issue_labels_empty))
            } else {
                Column {
                    available.forEach { label ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Checkbox(
                                checked = label.name in selection,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        selection.add(label.name)
                                    } else {
                                        selection.remove(label.name)
                                    }
                                },
                            )
                            Text(
                                label.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = Spacing.sm),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = { onConfirm(selection.toList()) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun EditCommentDialog(
    initial: String,
    busy: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.comment_edit_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                minLines = 4,
                maxLines = 10,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = !busy && text.isNotBlank(), onClick = { onConfirm(text) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun ReviewDialog(
    event: String,
    busy: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val title = if (event == "APPROVE") {
        stringResource(R.string.review_approve)
    } else {
        stringResource(R.string.review_request_changes)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.review_body_hint)) },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = { onConfirm(text) }) { Text(title) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private val ReactionEmoji = linkedMapOf(
    "+1" to "👍",
    "-1" to "👎",
    "laugh" to "😄",
    "hooray" to "🎉",
    "confused" to "😕",
    "heart" to "❤️",
    "rocket" to "🚀",
    "eyes" to "👀",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommentCard(
    comment: Comment,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReact: (String) -> Unit,
    onOpenUser: (String) -> Unit = {},
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var reactionsExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(enabled = comment.user?.login != null) {
                    onOpenUser(comment.user?.login.orEmpty())
                },
            ) {
                AsyncImage(
                    model = comment.user?.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape),
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    comment.user?.login ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (comment.user?.login != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Spacer(Modifier.weight(1f))
                Text(
                    relativeTimeString(Formatters.relativeOrAbsolute(comment.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.action_more),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.comment_edit)) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.comment_delete)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }
            comment.body?.let {
                Spacer(Modifier.height(Spacing.sm))
                MarkdownText(markdown = it)
            }
            Spacer(Modifier.height(Spacing.sm))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                val reactions = comment.reactions
                if (reactions != null && reactions.totalCount > 0) {
                    ReactionEmoji.forEach { (content, emoji) ->
                        val count = reactionCount(reactions, content)
                        if (count > 0) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            ) {
                                Text(
                                    "$emoji $count",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                }
                Box {
                    IconButton(onClick = { reactionsExpanded = true }) {
                        Icon(
                            Icons.Filled.AddReaction,
                            contentDescription = stringResource(R.string.reaction_add),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = reactionsExpanded,
                        onDismissRequest = { reactionsExpanded = false },
                    ) {
                        ReactionEmoji.forEach { (content, emoji) ->
                            DropdownMenuItem(
                                text = { Text(emoji) },
                                onClick = {
                                    reactionsExpanded = false
                                    onReact(content)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Merge sheet (G-014): pick a method and confirm. The state chip states what
 * GitHub thinks of the branch (`mergeable_state`) so a blocked merge is
 * explained before the user taps, not through an error afterwards.
 */
@Composable
private fun MergeDialog(
    detail: IssueDetailUi,
    merging: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var method by rememberSaveable { mutableStateOf("merge") }
    AlertDialog(
        onDismissRequest = { if (!merging) onDismiss() },
        title = { Text(stringResource(R.string.pr_merge_sheet_title, detail.number)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.pr_merge_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.md))
                listOf(
                    "merge" to R.string.pr_merge_method_merge,
                    "squash" to R.string.pr_merge_method_squash,
                    "rebase" to R.string.pr_merge_method_rebase,
                ).forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { method = value }
                            .padding(vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = method == value, onClick = { method = value })
                        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                val stateLabel = mergeStateLabel(detail)
                if (stateLabel != null) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        stringResource(stateLabel),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (detail.mergeable == false) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(method) },
                enabled = !merging && detail.mergeable != false,
            ) {
                if (merging) {
                    CircularWavyProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text(stringResource(R.string.pr_merge_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !merging) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@StringRes
private fun mergeStateLabel(detail: IssueDetailUi): Int? = when (detail.mergeableState) {
    "dirty" -> R.string.pr_state_conflict
    "behind" -> R.string.pr_state_behind
    "blocked", "draft" -> R.string.pr_state_blocked
    "clean", "unstable", "has_hooks" -> R.string.pr_state_clean
    else -> if (detail.mergeable == false) R.string.pr_state_conflict else null
}

private fun reactionCount(reactions: com.gityou.app.data.remote.dto.ReactionsSummary, content: String): Int =
    when (content) {
        "+1" -> reactions.plusOne
        "-1" -> reactions.minusOne
        "laugh" -> reactions.laugh
        "hooray" -> reactions.hooray
        "confused" -> reactions.confused
        "heart" -> reactions.heart
        "rocket" -> reactions.rocket
        "eyes" -> reactions.eyes
        else -> 0
    }
