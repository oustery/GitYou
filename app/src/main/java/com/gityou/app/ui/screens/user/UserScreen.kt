package com.gityou.app.ui.screens.user

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.gityou.app.data.remote.dto.Repo
import com.gityou.app.data.remote.dto.User
import com.gityou.app.data.util.Formatters
import com.gityou.app.ui.components.LoadMoreFooter
import com.gityou.app.ui.components.RepoCard
import com.gityou.app.ui.components.StateScreen
import com.gityou.app.ui.components.UserRow
import com.gityou.app.ui.theme.Spacing

/**
 * Public profile of any user: avatar, bio, stats and four lists
 * (repositories, stars, followers, following). Reached by tapping any
 * login or avatar across the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(
    onBack: () -> Unit,
    onOpenRepo: (String, String) -> Unit,
    onOpenUser: (String) -> Unit,
    viewModel: UserViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.user_title),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            viewModel.login,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
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
        StateScreen(state = state, onRetry = viewModel::refresh) { ui ->
            Column(modifier = Modifier.padding(padding)) {
                ProfileHeader(ui.user, viewModel.login)

                val tabTitles = listOf(
                    stringResource(R.string.user_tab_repos),
                    stringResource(R.string.user_tab_stars),
                    stringResource(R.string.user_tab_followers),
                    stringResource(R.string.user_tab_following),
                )
                TabRow(selectedTabIndex = ui.selectedTab.ordinal) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = ui.selectedTab.ordinal == index,
                            onClick = { viewModel.selectTab(UserTab.entries[index]) },
                            text = { Text(title, maxLines = 1) },
                        )
                    }
                }

                val list = ui.list(ui.selectedTab)
                when (ui.selectedTab) {
                    UserTab.REPOS, UserTab.STARS -> RepoList(list, onOpenRepo, viewModel::loadMore)
                    UserTab.FOLLOWERS, UserTab.FOLLOWING -> PeopleList(list, onOpenUser, viewModel::loadMore)
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(user: User, login: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
        )
        Spacer(Modifier.width(Spacing.lg))
        Column(Modifier.weight(1f)) {
            Text(
                user.name?.takeIf { it.isNotBlank() } ?: login,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (user.name != null && user.name != login) {
                Text(
                    login,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    Formatters.count(user.followers) + " · " +
                        stringResource(R.string.user_followers_short) + " · " +
                        Formatters.count(user.publicRepos) + " " +
                        stringResource(R.string.user_repos_short),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    user.bio?.takeIf { it.isNotBlank() }?.let {
        Text(
            it,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.lg),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Spacing.sm))
    }
}

@Composable
private fun RepoList(list: UserListState, onOpenRepo: (String, String) -> Unit, onLoadMore: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        items(list.items.filterIsInstance<Repo>(), key = { "${it.id}-${it.fullName}" }) { repo ->
            val owner = repo.owner?.login ?: repo.fullName?.substringBefore('/') ?: return@items
            RepoCard(repo = repo, onClick = { onOpenRepo(owner, repo.name) })
            Spacer(Modifier.height(Spacing.md))
        }
        item(key = "load-more") {
            AutoLoadMore(list, onLoadMore)
        }
    }
}

@Composable
private fun PeopleList(list: UserListState, onOpenUser: (String) -> Unit, onLoadMore: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        items(
            list.items.filterIsInstance<User>(),
            key = { it.id.toString() + "-" + it.login },
        ) { user ->
            UserRow(user = user, onClick = { onOpenUser(user.login) })
            Spacer(Modifier.height(Spacing.md))
        }
        item(key = "load-more") {
            AutoLoadMore(list, onLoadMore)
        }
    }
}

/** Auto-fetches the next page while it succeeds; after a failure the footer
 * turns into a retry button (same pattern as the search screen). */
@Composable
private fun AutoLoadMore(list: UserListState, onLoadMore: () -> Unit) {
    LaunchedEffect(list.items.size, list.loadMoreFailed) {
        if (list.nextPage != null && !list.loadMoreFailed) onLoadMore()
    }
    LoadMoreFooter(
        visible = list.loading,
        failed = list.loadMoreFailed,
        onRetry = onLoadMore,
    )
}
