package com.gityou.app.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gityou.app.R
import com.gityou.app.data.remote.dto.Notification
import com.gityou.app.ui.components.EmptyState
import com.gityou.app.ui.components.LoadMoreFooter
import com.gityou.app.ui.components.StateScreen
import com.gityou.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onOpenIssue: (String, String, Int, Boolean) -> Unit,
    onOpenRepo: (String, String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

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
                    IconButton(onClick = viewModel::markAllRead) {
                        Icon(Icons.Filled.DoneAll, contentDescription = stringResource(R.string.notifications_mark_all))
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
                    ) {
                        itemsIndexed(ui.items, key = { _, n -> n.id }) { _, notification ->
                            NotificationRow(
                                notification = notification,
                                onClick = {
                                    if (notification.unread) viewModel.markRead(notification)
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

@Composable
private fun NotificationRow(
    notification: Notification,
    onClick: () -> Unit,
    onMarkRead: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (notification.unread) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
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
                Text(
                    notification.repository?.fullName ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
                        Text(
                            type,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            if (notification.unread) {
                IconButton(onClick = onMarkRead) {
                    Icon(
                        Icons.Filled.MarkEmailRead,
                        contentDescription = stringResource(R.string.notifications_mark_read),
                    )
                }
            }
        }
    }
}
