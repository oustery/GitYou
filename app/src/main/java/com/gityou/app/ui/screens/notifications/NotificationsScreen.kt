package com.gityou.app.ui.screens.notifications

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gityou.app.R
import com.gityou.app.data.remote.dto.Notification
import com.gityou.app.data.util.Formatters
import com.gityou.app.data.util.relativeOrAbsolute
import com.gityou.app.ui.components.EmptyState
import com.gityou.app.ui.components.LoadMoreFooter
import com.gityou.app.ui.components.StateScreen
import com.gityou.app.ui.components.UiState
import com.gityou.app.ui.components.appHaptics
import com.gityou.app.ui.components.relativeTimeString
import com.gityou.app.ui.theme.Spacing

private enum class NotificationsFilter { ALL, UNREAD }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotificationsScreen(
    onOpenIssue: (String, String, Int, Boolean) -> Unit,
    onOpenRepo: (String, String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val markReadOnOpen by viewModel.markReadOnOpen.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(NotificationsFilter.ALL) }

    val items = (state as? UiState.Content<*>)?.value
        ?.let { (it as? NotificationsUi)?.items }
        .orEmpty()
    val unreadCount = items.count { it.unread }
    val visibleItems = when (filter) {
        NotificationsFilter.ALL -> items
        NotificationsFilter.UNREAD -> items.filter { it.unread }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptics = appHaptics()

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(context.getString(it))
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_notifications)) },
                actions = {
                    if (unreadCount > 0) {
                        IconButton(onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.markAllRead()
                        }) {
                            Icon(Icons.Filled.DoneAll, contentDescription = stringResource(R.string.notifications_mark_all))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        StateScreen(state = state, onRetry = viewModel::refresh) { ui ->
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.padding(padding),
            ) {
                if (ui.items.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.Inbox,
                        title = stringResource(R.string.notifications_empty_title),
                        body = stringResource(R.string.notifications_empty_body),
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            FilterChip(
                                selected = filter == NotificationsFilter.ALL,
                                onClick = { filter = NotificationsFilter.ALL },
                                label = { Text(stringResource(R.string.notifications_filter_all)) },
                            )
                            FilterChip(
                                selected = filter == NotificationsFilter.UNREAD,
                                onClick = { filter = NotificationsFilter.UNREAD },
                                label = { Text(stringResource(R.string.notifications_filter_unread)) },
                                leadingIcon = if (unreadCount > 0) {
                                    {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.tertiary),
                                        )
                                    }
                                } else {
                                    null
                                },
                            )
                        }

                        if (visibleItems.isEmpty()) {
                            EmptyState(
                                icon = Icons.Filled.MarkEmailRead,
                                title = stringResource(R.string.notifications_empty_title),
                                body = stringResource(R.string.notifications_empty_body),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                            ) {
                                itemsIndexed(visibleItems, key = { _, n -> n.id }) { _, notification ->
                                    SwipeableNotificationRow(
                                        notification = notification,
                                        onClick = {
                                            if (notification.unread && markReadOnOpen) viewModel.markRead(notification)
                                            viewModel.targetFor(notification)?.let { target ->
                                                onOpenIssue(target.owner, target.repo, target.number, target.isPull)
                                            } ?: notification.repository?.fullName?.let { fullName ->
                                                val owner = fullName.substringBefore('/')
                                                val repo = fullName.substringAfter('/')
                                                onOpenRepo(owner, repo)
                                            }
                                        },
                                        onMarkRead = { viewModel.markRead(notification) },
                                    )
                                    Spacer(Modifier.height(Spacing.md))
                                }
                                if (ui.hasMore) {
                                    item(key = "load-more") {
                                        LaunchedEffect(ui.items.size) { viewModel.loadMore() }
                                        LoadMoreFooter(visible = ui.loadingMore)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeableNotificationRow(
    notification: Notification,
    onClick: () -> Unit,
    onMarkRead: () -> Unit,
) {
    if (!notification.unread) {
        NotificationRow(notification = notification, onClick = onClick)
        return
    }
    // Swiping a card to the side marks it read; the row springs back and
    // switches to its quieter read style.
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onMarkRead()
                false
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        modifier = Modifier.fillMaxWidth(),
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Filled.MarkEmailRead,
                    contentDescription = stringResource(R.string.notifications_mark_read),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = Spacing.xl),
                )
            }
        },
    ) {
        NotificationRow(notification = notification, onClick = onClick, onMarkRead = onMarkRead)
    }
}

@Composable
private fun NotificationRow(
    notification: Notification,
    onClick: () -> Unit,
    onMarkRead: (() -> Unit)? = null,
) {
    // Read/unread reads through three channels: card tone, the accent dot
    // and the type icon, so color is never the only signal.
    val container by animateColorAsState(
        targetValue = if (notification.unread) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(200),
        label = "notificationContainer",
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = container,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.width(14.dp)) {
                if (notification.unread) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary),
                    )
                }
            }
            Spacer(Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        notification.repository?.fullName ?: "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    relativeTimeString(
                        Formatters.relativeOrAbsolute(notification.updatedAt),
                    ).takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            it,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    notification.subject?.title ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                notification.subject?.type?.let { type ->
                    Spacer(Modifier.height(Spacing.xs))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                subjectIcon(type),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(11.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                subjectTypeLabel(type),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }
            onMarkRead?.let { markRead ->
                IconButton(onClick = markRead) {
                    Icon(
                        Icons.Filled.MarkEmailRead,
                        contentDescription = stringResource(R.string.notifications_mark_read),
                    )
                }
            }
        }
    }
}

/** Localized label for the raw GitHub subject type. */
@Composable
private fun subjectTypeLabel(type: String): String = when (type) {
    "PullRequest" -> stringResource(R.string.notification_type_pull_request)
    "Issue" -> stringResource(R.string.notification_type_issue)
    "CheckSuite", "CheckRun", "WorkflowRun" -> stringResource(R.string.notification_type_check)
    "Release" -> stringResource(R.string.notification_type_release)
    "Discussion" -> stringResource(R.string.notification_type_discussion)
    "Commit" -> stringResource(R.string.notification_type_commit)
    else -> stringResource(R.string.notification_type_event)
}

/** Distinct icon per subject type instead of one generic envelope. */
private fun subjectIcon(type: String): ImageVector = when (type) {
    "PullRequest" -> Icons.Filled.MergeType
    "Issue" -> Icons.Filled.BugReport
    "CheckSuite", "CheckRun", "WorkflowRun" -> Icons.Filled.CheckCircleOutline
    "Release" -> Icons.Filled.NewReleases
    "Discussion" -> Icons.Filled.Forum
    "Commit" -> Icons.Filled.Commit
    else -> Icons.Filled.Circle
}
