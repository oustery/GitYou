package com.gityou.app.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gityou.app.R
import com.gityou.app.data.util.GitHubUrls
import com.gityou.app.ui.components.EmptyState
import com.gityou.app.ui.components.EyebrowLabel
import com.gityou.app.ui.components.IssueRow
import com.gityou.app.ui.components.LoadMoreFooter
import com.gityou.app.ui.components.RepoCard
import com.gityou.app.ui.components.StateScreen
import com.gityou.app.ui.components.UserRow
import com.gityou.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenRepo: (String, String) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenIssue: (String, String, Int, Boolean) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val type by viewModel.type.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.search_title)) })
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Filled pill-style search field: quieter than an outlined field
            // and consistent with mobile GitHub search patterns. The custom
            // inline pill is a deliberate choice; merged semantics make
            // TalkBack read it as one control with its hint label (E-019).
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                    .semantics(mergeDescendants = true) {},
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(Spacing.md))
                    BasicTextField(
                        value = query,
                        onValueChange = viewModel::onQueryChanged,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.onQueryChanged(query) }),
                        decorationBox = { inner ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (query.isEmpty()) {
                                    Text(
                                        stringResource(
                                            when (type) {
                                                SearchType.REPOS -> R.string.search_field_label
                                                SearchType.USERS -> R.string.search_field_label_users
                                                SearchType.ISSUES -> R.string.search_field_label_issues
                                            },
                                        ),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                inner()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 14.dp),
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChanged("") }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.action_clear),
                            )
                        }
                    }
                }
            }

            // What is being searched; the empty repo tab shows trending.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                listOf(
                    SearchType.REPOS to stringResource(R.string.search_type_repos),
                    SearchType.USERS to stringResource(R.string.search_type_users),
                    SearchType.ISSUES to stringResource(R.string.search_type_issues),
                ).forEach { (option, label) ->
                    FilterChip(
                        selected = type == option,
                        onClick = { viewModel.setType(option) },
                        label = { Text(label, maxLines = 1) },
                    )
                }
            }

            // Sort chips only make sense for repository search.
            if (type == SearchType.REPOS && query.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    listOf(
                        SearchSort.BEST to stringResource(R.string.search_sort_best),
                        SearchSort.STARS to stringResource(R.string.search_sort_stars),
                        SearchSort.UPDATED to stringResource(R.string.search_sort_updated),
                    ).forEach { (option, label) ->
                        FilterChip(
                            selected = sort == option,
                            onClick = { viewModel.setSort(option) },
                            label = { Text(label, maxLines = 1) },
                        )
                    }
                }
            }

            StateScreen(state = results, onRetry = viewModel::retrySearch) { ui ->
                if (ui.type == SearchType.REPOS && ui.trendingMode) {
                    TrendingList(ui = ui, onOpenRepo = onOpenRepo, onLoadMore = viewModel::loadMore)
                } else if (!ui.searched) {
                    EmptyState(
                        icon = Icons.Outlined.SearchOff,
                        title = stringResource(R.string.search_empty_title),
                        body = stringResource(R.string.search_empty_body),
                    )
                } else {
                    ResultsList(ui, onOpenRepo, onOpenUser, onOpenIssue, viewModel::loadMore)
                }
            }
        }
    }
}

@Composable
private fun TrendingList(
    ui: SearchUi,
    onOpenRepo: (String, String) -> Unit,
    onLoadMore: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = Spacing.md)) {
        item(key = "trending-header") {
            EyebrowLabel(
                stringResource(R.string.trending_title),
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
        }
        itemsIndexed(ui.repos, key = { index, repo ->
            if (repo.id != 0L) repo.id else "trending-$index-${repo.name}"
        }) { _, repo ->
            val ownerLogin = repo.owner?.login
                ?: repo.fullName?.substringBefore('/') ?: ""
            if (ownerLogin.isNotEmpty()) {
                RepoCard(
                    repo = repo,
                    onClick = { onOpenRepo(ownerLogin, repo.name) },
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                )
            }
        }
        if (ui.hasMore) {
            item(key = "load-more") {
                LaunchedEffect(ui.repos.size, ui.loadMoreFailed) {
                    if (!ui.loadMoreFailed) onLoadMore()
                }
                LoadMoreFooter(
                    visible = ui.loadingMore,
                    failed = ui.loadMoreFailed,
                    onRetry = onLoadMore,
                )
            }
        }
    }
}

@Composable
private fun ResultsList(
    ui: SearchUi,
    onOpenRepo: (String, String) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenIssue: (String, String, Int, Boolean) -> Unit,
    onLoadMore: () -> Unit,
) {
    val empty = when (ui.type) {
        SearchType.REPOS -> ui.repos.isEmpty()
        SearchType.USERS -> ui.users.isEmpty()
        SearchType.ISSUES -> ui.issues.isEmpty()
    }
    if (empty) {
        EmptyState(
            icon = Icons.Outlined.SearchOff,
            title = stringResource(R.string.search_no_results_title),
            body = stringResource(R.string.search_no_results_body, ui.query),
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        when (ui.type) {
            SearchType.REPOS -> itemsIndexed(ui.repos, key = { index, repo ->
                if (repo.id != 0L) repo.id else "repo-$index-${repo.name}"
            }) { _, repo ->
                val ownerLogin = repo.owner?.login
                    ?: repo.fullName?.substringBefore('/') ?: ""
                if (ownerLogin.isNotEmpty()) {
                    RepoCard(repo = repo, onClick = { onOpenRepo(ownerLogin, repo.name) })
                    Spacer(Modifier.height(Spacing.md))
                }
            }
            SearchType.USERS -> itemsIndexed(ui.users, key = { _, user ->
                if (user.id != 0L) user.id else "user-${user.login}"
            }) { _, user ->
                UserRow(user = user, onClick = { onOpenUser(user.login) })
                Spacer(Modifier.height(Spacing.md))
            }
            SearchType.ISSUES -> itemsIndexed(ui.issues, key = { index, issue ->
                if (issue.id != 0L) issue.id else "issue-$index-${issue.number}"
            }) { _, issue ->
                // The issue/PR page URL carries its repository coordinates.
                val ref = GitHubUrls.parseIssueUrl(issue.htmlUrl)
                if (ref != null) {
                    IssueRow(
                        issue = issue,
                        onClick = { onOpenIssue(ref.owner, ref.repo, ref.number, ref.isPull) },
                    )
                    Spacer(Modifier.height(Spacing.md))
                }
            }
        }
        if (ui.hasMore) {
            item(key = "load-more") {
                LaunchedEffect(
                    ui.repos.size,
                    ui.users.size,
                    ui.issues.size,
                    ui.loadMoreFailed,
                ) {
                    if (!ui.loadMoreFailed) onLoadMore()
                }
                LoadMoreFooter(
                    visible = ui.loadingMore,
                    failed = ui.loadMoreFailed,
                    onRetry = onLoadMore,
                )
            }
        }
    }
}
