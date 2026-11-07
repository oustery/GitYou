package com.gityou.app.ui.screens.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gityou.app.R
import com.gityou.app.ui.components.EmptyState
import com.gityou.app.ui.components.EyebrowLabel
import com.gityou.app.ui.components.LoadMoreFooter
import com.gityou.app.ui.components.RepoCard
import com.gityou.app.ui.components.StateScreen
import com.gityou.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenRepo: (String, String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
    ) { padding ->
        StateScreen(state = state, onRetry = viewModel::refresh) { home ->
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.padding(padding),
            ) {
                if (home.repos.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.Book,
                        title = stringResource(R.string.home_empty_title),
                        body = stringResource(R.string.home_empty_body),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = Spacing.lg,
                            vertical = Spacing.md,
                        ),
                    ) {
                        item {
                            EyebrowLabel(stringResource(R.string.home_repos_section))
                        }
                        itemsIndexed(home.repos, key = { index, repo ->
                            // Fallback keeps LazyColumn keys unique even if a
                            // DTO arrives with the default id = 0 (F-014).
                            if (repo.id != 0L) repo.id else "repo-$index-${repo.name}"
                        }) { _, repo ->
                            val ownerLogin = repo.owner?.login
                                ?: repo.fullName?.substringBefore('/') ?: ""
                            if (ownerLogin.isNotEmpty()) {
                                RepoCard(
                                    repo = repo,
                                    showAvatar = false,
                                    onClick = { onOpenRepo(ownerLogin, repo.name) },
                                )
                                Spacer(Modifier.height(Spacing.md))
                            }
                        }
                        if (home.hasMore) {
                            item(key = "load-more") {
                                LaunchedEffect(home.repos.size, home.loadMoreFailed) {
                                    if (!home.loadMoreFailed) viewModel.loadMore()
                                }
                                LoadMoreFooter(
                                    visible = home.loadingMore,
                                    failed = home.loadMoreFailed,
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
