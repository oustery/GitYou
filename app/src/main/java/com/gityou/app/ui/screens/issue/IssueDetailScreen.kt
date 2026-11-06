package com.gityou.app.ui.screens.issue

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.gityou.app.R
import com.gityou.app.data.util.Formatters
import com.gityou.app.ui.components.EyebrowLabel
import com.gityou.app.ui.components.MarkdownText
import com.gityou.app.ui.components.StateScreen
import com.gityou.app.ui.components.errorMessageFor
import com.gityou.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailScreen(
    owner: String,
    repoName: String,
    number: Int,
    isPull: Boolean,
    onBack: () -> Unit,
    viewModel: IssueDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val commentError by viewModel.commentError.collectAsStateWithLifecycle()

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
            )
        },
    ) { padding ->
        StateScreen(state = state, onRetry = viewModel::load) { screenState ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                IssueHeader(screenState.detail)
                CommentsSection(
                    screenState = screenState,
                    commentError = commentError,
                    onSend = viewModel::addComment,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun IssueHeader(detail: IssueDetailUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
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
            Surface(shape = RoundedCornerShape(12.dp), color = container) {
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
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(Spacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Formatters.date(detail.createdAt) ?: "",
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
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (detail.labels.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.md))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.sm)) {
                detail.labels.take(4).forEach { label ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
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
                shape = RoundedCornerShape(16.dp),
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CommentsSection(
    screenState: IssueDetailScreenState,
    commentError: Boolean,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by rememberSaveable { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth()) {
        EyebrowLabel(
            stringResource(R.string.issue_comments_section, screenState.detail.commentCount),
            modifier = Modifier.padding(horizontal = Spacing.lg),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
        ) {
            if (screenState.commentsLoading) {
                item {
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
            items(screenState.detail.comments, key = { it.id }) { comment ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xs),
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                Formatters.date(comment.createdAt) ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        comment.body?.let {
                            Spacer(Modifier.height(Spacing.sm))
                            MarkdownText(markdown = it)
                        }
                    }
                }
            }
            if (!screenState.commentsLoading && screenState.detail.comments.isEmpty()) {
                item {
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
                androidx.compose.material3.Button(
                    onClick = {
                        onSend(draft)
                        draft = ""
                    },
                    enabled = draft.isNotBlank() && !screenState.sendingComment,
                ) {
                    if (screenState.sendingComment) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
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
}
