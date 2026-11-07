package com.gityou.app.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.gityou.app.R
import com.gityou.app.ui.components.EmptyState
import com.gityou.app.ui.components.LoadMoreFooter
import com.gityou.app.ui.components.RepoCard
import com.gityou.app.ui.components.StatBlock
import com.gityou.app.ui.components.StateScreen
import com.gityou.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onOpenRepo: (String, String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Consistent M3E hierarchy across the four top-level tabs (E-004).
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.nav_profile)) },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        StateScreen(state = state, onRetry = viewModel::refresh) { ui ->
            Column(modifier = Modifier.padding(padding)) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = ui.user.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape),
                            )
                            Spacer(Modifier.width(Spacing.lg))
                            Column {
                                Text(
                                    ui.user.name ?: ui.user.login,
                                    style = MaterialTheme.typography.headlineSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    ui.user.login,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        ui.user.bio?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(Spacing.md))
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(Spacing.lg))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatBlock(
                                icon = Icons.Filled.People,
                                value = ui.user.followers.toString(),
                                label = stringResource(R.string.profile_followers),
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(Spacing.md))
                            StatBlock(
                                icon = Icons.Filled.PersonAdd,
                                value = ui.user.following.toString(),
                                label = stringResource(R.string.profile_following),
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(Spacing.md))
                            StatBlock(
                                icon = Icons.Filled.Book,
                                value = ui.user.publicRepos.toString(),
                                label = stringResource(R.string.profile_repos),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                TabRow(selectedTabIndex = ui.selectedTab) {
                    Tab(
                        selected = ui.selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        text = { Text(stringResource(R.string.profile_tab_repositories)) },
                    )
                    Tab(
                        selected = ui.selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        text = { Text(stringResource(R.string.profile_tab_stars)) },
                    )
                }

                val items = if (ui.selectedTab == 0) ui.repos else ui.stars
                if (items.isEmpty()) {
                    EmptyProfileList(isStars = ui.selectedTab == 1)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
                    ) {
                        itemsIndexed(items, key = { index, repo ->
                            // Fallback keeps LazyColumn keys unique even if a
                            // DTO arrives with the default id = 0 (F-014).
                            if (repo.id != 0L) repo.id else "repo-$index-${repo.name}"
                        }) { _, repo ->
                            val ownerLogin = repo.owner?.login
                                ?: repo.fullName?.substringBefore('/') ?: ""
                            if (ownerLogin.isNotEmpty()) {
                                RepoCard(
                                    repo = repo,
                                    // Own repos: one repeated avatar adds nothing;
                                    // stars are other people's repos, keep avatars.
                                    showAvatar = ui.selectedTab == 1,
                                    onClick = { onOpenRepo(ownerLogin, repo.name) },
                                )
                                Spacer(Modifier.height(Spacing.md))
                            }
                        }
                        if (ui.selectedTab == 0 && ui.reposNextPage != null) {
                            item(key = "repos-more") {
                                LaunchedEffect(ui.repos.size, ui.reposLoadMoreFailed) {
                                    if (!ui.reposLoadMoreFailed) viewModel.loadReposMore()
                                }
                                LoadMoreFooter(
                                    visible = ui.loadingRepos,
                                    failed = ui.reposLoadMoreFailed,
                                    onRetry = viewModel::loadReposMore,
                                )
                            }
                        }
                        if (ui.selectedTab == 1 && ui.starsNextPage != null) {
                            item(key = "stars-more") {
                                LaunchedEffect(ui.stars.size, ui.starsLoadMoreFailed) {
                                    if (!ui.starsLoadMoreFailed) viewModel.loadStarsMore()
                                }
                                LoadMoreFooter(
                                    visible = ui.loadingStars,
                                    failed = ui.starsLoadMoreFailed,
                                    onRetry = viewModel::loadStarsMore,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyProfileList(isStars: Boolean) {
    val (title, body) = if (isStars) {
        stringResource(R.string.profile_empty_stars_title) to stringResource(R.string.profile_empty_stars_body)
    } else {
        stringResource(R.string.profile_empty_repos_title) to stringResource(R.string.profile_empty_repos_body)
    }
    EmptyState(
        icon = Icons.Filled.Book,
        title = title,
        body = body,
    )
}
