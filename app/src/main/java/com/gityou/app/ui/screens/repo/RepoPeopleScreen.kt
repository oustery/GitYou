package com.gityou.app.ui.screens.repo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gityou.app.R
import com.gityou.app.data.remote.dto.Repo
import com.gityou.app.data.remote.dto.User
import com.gityou.app.ui.components.LoadMoreFooter
import com.gityou.app.ui.components.RepoCard
import com.gityou.app.ui.components.StateScreen
import com.gityou.app.ui.components.UserRow
import com.gityou.app.ui.theme.Spacing

/** Stargazers or forks of one repository; rows open the user or the fork. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoPeopleScreen(
    onBack: () -> Unit,
    onOpenRepo: (String, String) -> Unit,
    onOpenUser: (String) -> Unit,
    viewModel: RepoPeopleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title = when (viewModel.kind) {
        RepoPeopleKind.STARGAZERS -> stringResource(R.string.people_title_stargazers)
        RepoPeopleKind.FORKS -> stringResource(R.string.forks_title)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${viewModel.owner}/${viewModel.repoName}",
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
            LazyColumn(
                contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
                modifier = Modifier.padding(padding),
            ) {
                when (viewModel.kind) {
                    RepoPeopleKind.STARGAZERS -> items(
                        ui.items.filterIsInstance<User>(),
                        key = { it.id.toString() + "-" + it.login },
                    ) { user ->
                        UserRow(user = user, onClick = { onOpenUser(user.login) })
                        Spacer(Modifier.height(Spacing.md))
                    }
                    RepoPeopleKind.FORKS -> items(
                        ui.items.filterIsInstance<Repo>(),
                        key = { "${it.id}-${it.fullName}" },
                    ) { fork ->
                        val forkOwner = fork.owner?.login
                            ?: fork.fullName?.substringBefore('/') ?: return@items
                        RepoCard(repo = fork, onClick = { onOpenRepo(forkOwner, fork.name) })
                        Spacer(Modifier.height(Spacing.md))
                    }
                }
                item(key = "load-more") {
                    LaunchedEffect(ui.items.size, ui.loadMoreFailed) {
                        if (ui.hasMore && !ui.loadMoreFailed) viewModel.loadMore()
                    }
                    LoadMoreFooter(
                        visible = ui.loadingMore,
                        failed = ui.loadMoreFailed,
                        onRetry = viewModel::loadMore,
                    )
                }
            }
        }
    }
}
