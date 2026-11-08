package com.gityou.app.ui.screens.commits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gityou.app.R
import com.gityou.app.data.remote.dto.CommitListItem
import com.gityou.app.data.util.Formatters
import com.gityou.app.ui.components.LoadMoreFooter
import com.gityou.app.ui.components.StateScreen
import com.gityou.app.ui.theme.Spacing

/** Commit history of the repository; tap a row for the full diff. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommitsScreen(
    owner: String,
    repoName: String,
    onBack: () -> Unit,
    onOpenCommit: (String) -> Unit,
    onOpenUser: (String) -> Unit = {},
    viewModel: CommitsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.commits_title),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "$owner/$repoName",
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
                items(ui.commits, key = { it.sha }) { commit ->
                    CommitRow(commit, onClick = { onOpenCommit(commit.sha) }, onOpenUser = onOpenUser)
                }
                item(key = "load-more") {
                    // Auto-fetch the next page while the footer is visible
                    // (the footer also doubles as the retry button, F-013).
                    LaunchedEffect(ui.commits.size, ui.loadMoreFailed) {
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

@Composable
private fun CommitRow(commit: CommitListItem, onClick: () -> Unit, onOpenUser: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md)) {
            Text(
                commit.firstLine,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val authorLogin = commit.author?.login
                val authorText = authorLogin
                    ?: commit.commit?.author?.name.orEmpty()
                Text(
                    authorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (authorLogin != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .then(
                            if (authorLogin != null) {
                                Modifier.clickable { onOpenUser(authorLogin) }
                            } else {
                                Modifier
                            },
                        ),
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    Formatters.date(commit.commit?.author?.date).orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    commit.sha.take(7),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
