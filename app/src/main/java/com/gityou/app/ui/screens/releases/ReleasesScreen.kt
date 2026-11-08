package com.gityou.app.ui.screens.releases

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gityou.app.R
import com.gityou.app.data.remote.dto.Release
import com.gityou.app.data.remote.dto.Tag
import com.gityou.app.data.util.Formatters
import com.gityou.app.ui.components.EmptyState
import com.gityou.app.ui.components.EyebrowLabel
import com.gityou.app.ui.components.LoadMoreFooter
import com.gityou.app.ui.components.MarkdownText
import com.gityou.app.ui.components.StateScreen
import com.gityou.app.ui.components.openUrl
import com.gityou.app.ui.theme.Spacing

/** Releases (changelog + assets) and tags of one repository. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleasesScreen(
    onBack: () -> Unit,
    onOpenCommits: (String) -> Unit,
    viewModel: ReleasesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.releases_title),
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
            Column(modifier = Modifier.padding(padding)) {
                TabRow(selectedTabIndex = ui.selectedTab) {
                    Tab(
                        selected = ui.selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        text = { Text(stringResource(R.string.releases_tab_releases)) },
                    )
                    Tab(
                        selected = ui.selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        text = { Text(stringResource(R.string.releases_tab_tags)) },
                    )
                }

                if (ui.selectedTab == 0) {
                    ReleasesList(ui = ui, onLoadMore = viewModel::loadMore)
                } else {
                    TagsList(ui = ui, onLoadMore = viewModel::loadMore, onTagCommits = onOpenCommits)
                }
            }
        }
    }
}

@Composable
private fun ReleasesList(ui: ReleasesUi, onLoadMore: () -> Unit) {
    if (ui.releases.isEmpty() && !ui.loadingReleases && !ui.releasesLoadMoreFailed) {
        EmptyState(
            icon = Icons.Filled.NewReleases,
            title = stringResource(R.string.releases_empty_title),
            body = stringResource(R.string.releases_empty_body),
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        items(ui.releases, key = { it.id }) { release ->
            ReleaseCard(release)
            Spacer(Modifier.height(Spacing.md))
        }
        item(key = "load-more") {
            LaunchedEffect(ui.releases.size, ui.releasesLoadMoreFailed) {
                if (ui.releasesNextPage != null && !ui.releasesLoadMoreFailed) onLoadMore()
            }
            LoadMoreFooter(
                visible = ui.loadingReleases,
                failed = ui.releasesLoadMoreFailed,
                onRetry = onLoadMore,
            )
        }
    }
}

@Composable
private fun TagsList(ui: ReleasesUi, onLoadMore: () -> Unit, onTagCommits: (String) -> Unit) {
    if (ui.tags.isEmpty() && !ui.loadingTags && !ui.tagsLoadMoreFailed) {
        EmptyState(
            icon = Icons.Filled.Tag,
            title = stringResource(R.string.releases_empty_title),
            body = stringResource(R.string.releases_empty_body),
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        items(ui.tags, key = { it.name }) { tag ->
            TagRow(tag = tag, onClick = { tag.commit?.sha?.let(onTagCommits) })
            Spacer(Modifier.height(Spacing.md))
        }
        item(key = "load-more") {
            LaunchedEffect(ui.tags.size, ui.tagsLoadMoreFailed) {
                if (ui.tagsNextPage != null && !ui.tagsLoadMoreFailed) onLoadMore()
            }
            LoadMoreFooter(
                visible = ui.loadingTags,
                failed = ui.tagsLoadMoreFailed,
                onRetry = onLoadMore,
            )
        }
    }
}

@Composable
private fun TagRow(tag: Tag, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Tag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(Spacing.lg))
            Text(
                tag.name,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            tag.commit?.sha?.let {
                Text(
                    it.take(7),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReleaseCard(release: Release) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.NewReleases,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    release.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(Spacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    release.tagName,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                )
                Formatters.date(release.publishedAt)?.let {
                    Text(
                        "  ·  $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (release.preRelease) {
                    Text(
                        "  ·  " + stringResource(R.string.release_prerelease),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            release.body?.takeIf { it.isNotBlank() }?.let { body ->
                Spacer(Modifier.height(Spacing.md))
                MarkdownText(markdown = body, textStyle = MaterialTheme.typography.bodyMedium)
            }

            if (release.assets.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.md))
                EyebrowLabel(stringResource(R.string.release_assets))
                release.assets.forEach { asset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                asset.downloadUrl?.let { url -> openUrl(context, url) }
                            }
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            asset.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            Formatters.bytes(asset.size) + " · " + Formatters.count(asset.downloadCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
