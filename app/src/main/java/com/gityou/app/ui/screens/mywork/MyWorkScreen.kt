package com.gityou.app.ui.screens.mywork

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.gityou.app.R
import com.gityou.app.data.util.SearchQueries
import com.gityou.app.ui.components.EmptyState
import com.gityou.app.ui.components.IssueRow
import com.gityou.app.ui.components.LoadMoreFooter
import com.gityou.app.ui.components.StateScreen
import com.gityou.app.ui.components.UiState
import com.gityou.app.ui.components.appHaptics
import com.gityou.app.ui.theme.Spacing

/**
 * "My work" hub (G-010): the three questions a developer opens a GitHub client
 * to answer — what is assigned to me, what did I create, what waits for my
 * review. Each tab is an `search/issues` query with its own pagination.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MyWorkScreen(
    onBack: () -> Unit,
    onOpenIssue: (String, String, Int, Boolean) -> Unit,
    viewModel: MyWorkViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = appHaptics()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_work_title)) },
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
        StateScreen(state = state, onRetry = viewModel::retry) { current ->
            val visible = current.current()
            // No separate "refreshing" flag: the tab keeps its list visible
            // while the first-page request is in flight (see MyWorkViewModel).
            PullToRefreshBox(
                isRefreshing = false,
                onRefresh = viewModel::refresh,
                modifier = Modifier.padding(padding),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        MyWorkTab.entries.forEach { tab ->
                            FilterChip(
                                selected = current.tab == tab.query,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.selectTab(tab.query)
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                            )
                        }
                    }

                    if (visible.items.isEmpty()) {
                        EmptyState(
                            icon = emptyIcon(current.tab),
                            title = stringResource(emptyTitle(current.tab)),
                            body = stringResource(emptyBody(current.tab)),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = Spacing.lg,
                                vertical = Spacing.sm,
                            ),
                        ) {
                            items(visible.items, key = { "${it.id}-${it.number}" }) { issue ->
                                val fullName = issue.repositoryFullName
                                IssueRow(
                                    issue = issue,
                                    onClick = {
                                        if (fullName != null) {
                                            onOpenIssue(
                                                fullName.substringBefore('/'),
                                                fullName.substringAfter('/'),
                                                issue.number,
                                                issue.isPullRequest,
                                            )
                                        }
                                    },
                                )
                                Spacer(Modifier.height(Spacing.md))
                            }
                            if (visible.nextPage != null) {
                                item(key = "load-more") {
                                    LaunchedEffect(visible.items.size, visible.loadMoreFailed) {
                                        if (!visible.loadMoreFailed) viewModel.loadMore()
                                    }
                                    LoadMoreFooter(
                                        visible = visible.loadingMore,
                                        failed = visible.loadMoreFailed,
                                        onRetry = viewModel::loadMore,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Chip model: the query tab plus its localized label. */
private enum class MyWorkTab(val query: SearchQueries.MyWorkTab, val labelRes: Int) {
    ASSIGNED(SearchQueries.MyWorkTab.ASSIGNED, R.string.my_work_tab_assigned),
    CREATED(SearchQueries.MyWorkTab.CREATED, R.string.my_work_tab_created),
    REVIEW(SearchQueries.MyWorkTab.REVIEW_REQUESTED, R.string.my_work_tab_review),
}

private fun emptyIcon(tab: SearchQueries.MyWorkTab) = when (tab) {
    SearchQueries.MyWorkTab.REVIEW_REQUESTED -> Icons.Outlined.RateReview
    SearchQueries.MyWorkTab.CREATED -> Icons.Outlined.TaskAlt
    else -> Icons.Outlined.BugReport
}

private fun emptyTitle(tab: SearchQueries.MyWorkTab): Int = when (tab) {
    SearchQueries.MyWorkTab.ASSIGNED -> R.string.my_work_empty_assigned_title
    SearchQueries.MyWorkTab.CREATED -> R.string.my_work_empty_created_title
    SearchQueries.MyWorkTab.REVIEW_REQUESTED -> R.string.my_work_empty_review_title
}

private fun emptyBody(tab: SearchQueries.MyWorkTab): Int = when (tab) {
    SearchQueries.MyWorkTab.ASSIGNED -> R.string.my_work_empty_assigned_body
    SearchQueries.MyWorkTab.CREATED -> R.string.my_work_empty_created_body
    SearchQueries.MyWorkTab.REVIEW_REQUESTED -> R.string.my_work_empty_review_body
}
