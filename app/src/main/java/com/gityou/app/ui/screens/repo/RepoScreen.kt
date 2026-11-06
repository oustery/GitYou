package com.gityou.app.ui.screens.repo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gityou.app.R
import com.gityou.app.data.remote.dto.ContentItem
import com.gityou.app.data.util.Formatters
import com.gityou.app.ui.components.CircleActionButton
import com.gityou.app.ui.components.EyebrowLabel
import com.gityou.app.ui.components.EmptyState
import com.gityou.app.ui.components.IssueRow
import com.gityou.app.ui.components.LanguageDot
import com.gityou.app.ui.components.LanguageColors
import com.gityou.app.ui.components.LoadMoreFooter
import com.gityou.app.ui.components.MarkdownText
import com.gityou.app.ui.components.MorphingStarButton
import com.gityou.app.ui.components.PullRow
import com.gityou.app.ui.components.StatBlock
import com.gityou.app.ui.components.StateScreen
import com.gityou.app.ui.components.errorMessageFor
import com.gityou.app.ui.theme.Spacing
import kotlinx.coroutines.launch

private val tabTitles = listOf(
    R.string.repo_tab_about,
    R.string.repo_tab_code,
    R.string.repo_tab_issues,
    R.string.repo_tab_pulls,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoScreen(
    owner: String,
    repoName: String,
    onBack: () -> Unit,
    onOpenIssue: (Int, Boolean) -> Unit,
    onNewIssue: () -> Unit,
    viewModel: RepoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(context.getString(it))
            viewModel.consumeActionMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "$owner/$repoName",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val selectedTab = (state as? com.gityou.app.ui.components.UiState.Content<*>)?.value
                ?.let { (it as? RepoDetailUi)?.selectedTab }
            if (selectedTab == 2) {
                FloatingActionButton(onClick = onNewIssue) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.issue_new_title))
                }
            }
        },
    ) { padding ->
        StateScreen(state = state, onRetry = viewModel::load) { ui ->
            Column(modifier = Modifier.padding(padding)) {
                TabRow(selectedTabIndex = ui.selectedTab) {
                    tabTitles.forEachIndexed { index, titleRes ->
                        Tab(
                            selected = ui.selectedTab == index,
                            onClick = { viewModel.selectTab(index) },
                            text = { Text(stringResource(titleRes)) },
                        )
                    }
                }
                when (ui.selectedTab) {
                    0 -> AboutTab(ui, viewModel, owner, repoName)
                    1 -> CodeTab(ui, viewModel)
                    2 -> IssuesTab(ui, viewModel, onOpenIssue)
                    else -> PullsTab(ui, viewModel, onOpenIssue)
                }
            }

            ui.fileContent?.let { file ->
                ModalBottomSheet(onDismissRequest = viewModel::dismissFile) {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    val scope = rememberCoroutineScope()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg)
                            .navigationBarsPadding()
                            .padding(bottom = Spacing.md),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                file.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = {
                                clipboard?.setPrimaryClip(ClipData.newPlainText(file.name, file.text))
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.copied_to_clipboard))
                                }
                            }) {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = stringResource(R.string.action_copy),
                                )
                            }
                            IconButton(onClick = viewModel::dismissFile) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.action_close),
                                )
                            }
                        }
                        Spacer(Modifier.height(Spacing.md))
                        SelectionContainer {
                            Text(
                                file.text,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .heightIn(max = 480.dp)
                                    .verticalScroll(rememberScrollState()),
                            )
                        }
                        Spacer(Modifier.height(Spacing.md))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- About tab

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AboutTab(
    ui: RepoDetailUi,
    viewModel: RepoViewModel,
    owner: String,
    repoName: String,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
    ) {
        ui.repo.description?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(Spacing.lg))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            MorphingStarButton(
                isStarred = ui.isStarred,
                isLoading = ui.starBusy,
                onToggle = viewModel::toggleStar,
            )
            CircleActionButton(
                icon = Icons.Filled.CallSplit,
                contentDescription = stringResource(R.string.repo_fork_action),
                onClick = viewModel::fork,
            )
            CircleActionButton(
                icon = Icons.Filled.OpenInNew,
                contentDescription = stringResource(R.string.repo_open_in_browser),
                onClick = {
                    ui.repo.htmlUrl?.let { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                },
            )
        }

        Spacer(Modifier.height(Spacing.lg))

        Row(modifier = Modifier.fillMaxWidth()) {
            StatBlock(
                icon = Icons.Filled.Star,
                value = Formatters.count(ui.repo.stargazersCount),
                label = stringResource(R.string.repo_stat_stars),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Spacing.md))
            StatBlock(
                icon = Icons.Filled.CallSplit,
                value = Formatters.count(ui.repo.forksCount),
                label = stringResource(R.string.repo_stat_forks),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Spacing.md))
            StatBlock(
                icon = Icons.Filled.BugReport,
                value = Formatters.count(ui.repo.openIssuesCount),
                label = stringResource(R.string.repo_stat_issues),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(Spacing.lg))

        Row(verticalAlignment = Alignment.CenterVertically) {
            LanguageDot(LanguageColors.colorFor(ui.repo.language))
            Spacer(Modifier.width(6.dp))
            Text(
                ui.repo.language ?: stringResource(R.string.repo_language_unknown),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ui.repo.license?.spdxId?.takeIf { it.isNotBlank() && it != "NOASSERTION" }?.let { license ->
            Spacer(Modifier.height(Spacing.sm))
            Text(
                stringResource(R.string.repo_license_label, license),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Formatters.date(ui.repo.pushedAt)?.let {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                stringResource(R.string.repo_updated_label, it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Spacing.xl))
        EyebrowLabel(stringResource(R.string.repo_readme_section))

        when (val readme = ui.readme) {
            is com.gityou.app.ui.components.UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xl),
                    contentAlignment = Alignment.Center,
                ) {
                    LinearWavyProgressIndicator()
                }
            }
            is com.gityou.app.ui.components.UiState.Error -> {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(start = Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            errorMessageFor(readme.errorKey),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = viewModel::loadReadme) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
            }
            is com.gityou.app.ui.components.UiState.Content -> MarkdownText(
                markdown = readme.value,
                textStyle = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

// ---------------------------------------------------------------- Code tab

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CodeTab(
    ui: RepoDetailUi,
    viewModel: RepoViewModel,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BranchSelector(ui = ui, viewModel = viewModel)
            TextButton(onClick = { viewModel.loadContents("") }) {
                Text(stringResource(R.string.repo_code_root))
            }
            if (ui.currentPath.isNotBlank()) {
                var accumulated = ""
                ui.currentPath.split('/').forEachIndexed { index, segment ->
                    accumulated = if (index == 0) segment else "$accumulated/$segment"
                    Text(
                        "/",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { viewModel.loadContents(accumulated) }) {
                        Text(segment, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        if (ui.contentsLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xl),
                contentAlignment = Alignment.Center,
            ) {
                LinearWavyProgressIndicator()
            }
        } else if (ui.contents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.xl),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.repo_code_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val sorted = remember(ui.contents) {
                ui.contents.sortedWith(
                    compareByDescending<ContentItem> { it.type == "dir" }
                        .thenBy { it.name.lowercase() },
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
            ) {
                items(sorted, key = { it.path }) { item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(Spacing.lg)
                                .fillMaxWidth()
                                .clickable {
                                    if (item.type == "dir") {
                                        viewModel.loadContents(item.path)
                                    } else {
                                        viewModel.openFile(item)
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (item.type == "dir") Icons.Filled.Folder else Icons.Filled.InsertDriveFile,
                                contentDescription = null,
                                tint = if (item.type == "dir") {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(Spacing.lg))
                            Column {
                                Text(item.name, style = MaterialTheme.typography.titleSmall)
                                if (item.type == "file" && item.size > 0) {
                                    Text(
                                        Formatters.bytes(item.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

// ---------------------------------------------------------------- Branch

/** Dropdown to switch the branch the code browser reads. */
@Composable
private fun BranchSelector(
    ui: RepoDetailUi,
    viewModel: RepoViewModel,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilledTonalButton(onClick = { expanded = true }) {
            Icon(
                Icons.Filled.CallSplit,
                contentDescription = stringResource(R.string.repo_branch_select),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(
                ui.activeBranch,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp),
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val options = ui.branches.ifEmpty { listOf(ui.activeBranch) }
            options.forEach { branch ->
                DropdownMenuItem(
                    text = {
                        Text(
                            branch,
                            fontWeight = if (branch == ui.activeBranch) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            },
                        )
                    },
                    onClick = {
                        expanded = false
                        viewModel.selectBranch(branch)
                    },
                )
            }
        }
    }
}

// ------------------------------------------------------- Issues / PRs tabs

@Composable
private fun IssuesTab(
    ui: RepoDetailUi,
    viewModel: RepoViewModel,
    onOpenIssue: (Int, Boolean) -> Unit,
) {
    ListWithFilter(
        filter = ui.issues.filter,
        onFilter = viewModel::setIssueFilter,
        items = ui.issues.items,
        loadingMore = ui.issues.loadingMore,
        hasMore = ui.issues.nextPage != null,
        onLoadMore = viewModel::loadIssuesMore,
        rowContent = { issue -> IssueRow(issue = issue, onClick = { onOpenIssue(issue.number, false) }) },
        emptyIcon = Icons.Outlined.BugReport,
        emptyTitle = stringResource(R.string.repo_issues_empty_title),
        emptyBody = stringResource(R.string.repo_issues_empty_body),
    )
}

@Composable
private fun PullsTab(
    ui: RepoDetailUi,
    viewModel: RepoViewModel,
    onOpenIssue: (Int, Boolean) -> Unit,
) {
    ListWithFilter(
        filter = ui.pulls.filter,
        onFilter = viewModel::setPullFilter,
        items = ui.pulls.items,
        loadingMore = ui.pulls.loadingMore,
        hasMore = ui.pulls.nextPage != null,
        onLoadMore = viewModel::loadPullsMore,
        rowContent = { pull -> PullRow(pull = pull, onClick = { onOpenIssue(pull.number, true) }) },
        emptyIcon = Icons.Outlined.MergeType,
        emptyTitle = stringResource(R.string.repo_pulls_empty_title),
        emptyBody = stringResource(R.string.repo_pulls_empty_body),
    )
}

@Composable
private fun <T> ListWithFilter(
    filter: String,
    onFilter: (String) -> Unit,
    items: List<T>,
    loadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    rowContent: @Composable (T) -> Unit,
    emptyIcon: androidx.compose.ui.graphics.vector.ImageVector,
    emptyTitle: String,
    emptyBody: String,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            FilterChip(
                selected = filter == "open",
                onClick = { onFilter("open") },
                label = { Text(stringResource(R.string.filter_open)) },
            )
            FilterChip(
                selected = filter == "closed",
                onClick = { onFilter("closed") },
                label = { Text(stringResource(R.string.filter_closed)) },
            )
        }

        if (items.isEmpty()) {
            EmptyState(
                icon = emptyIcon,
                title = emptyTitle,
                body = emptyBody,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
            ) {
                items(items.size) { index ->
                    rowContent(items[index])
                    Spacer(Modifier.height(Spacing.md))
                }
                if (hasMore) {
                    item(key = "load-more") {
                        LaunchedEffect(items.size) { onLoadMore() }
                        LoadMoreFooter(visible = loadingMore)
                    }
                }
            }
        }
    }
}
