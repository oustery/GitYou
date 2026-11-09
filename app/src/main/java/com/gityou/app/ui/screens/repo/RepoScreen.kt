package com.gityou.app.ui.screens.repo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.gityou.app.R
import com.gityou.app.data.remote.dto.ContentItem
import com.gityou.app.data.util.CodeLanguages
import com.gityou.app.data.util.Formatters
import com.gityou.app.ui.components.EyebrowLabel
import com.gityou.app.ui.components.EmptyState
import com.gityou.app.ui.components.IssueRow
import com.gityou.app.ui.components.LanguageDot
import com.gityou.app.ui.components.LanguageColors
import com.gityou.app.ui.components.LoadMoreFooter
import com.gityou.app.ui.components.MarkdownText
import com.gityou.app.ui.components.MorphingStarButton
import com.gityou.app.ui.components.PullRow
import com.gityou.app.ui.components.RoundedActionButton
import com.gityou.app.ui.components.StatBlock
import com.gityou.app.ui.components.StateScreen
import com.gityou.app.ui.components.appHaptics
import com.gityou.app.ui.components.openUrl
import com.gityou.app.ui.components.UiState
import com.gityou.app.ui.components.errorMessageFor
import com.gityou.app.ui.theme.Spacing
import kotlinx.coroutines.launch

private val tabTitles = listOf(
    R.string.repo_tab_about,
    R.string.repo_tab_code,
    R.string.repo_tab_issues,
    R.string.repo_tab_pulls,
)

/** Full-file rendering in one TextView is slow; cap the preview politely. */
private const val FILE_PREVIEW_MAX_LINES = 2000

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RepoScreen(
    owner: String,
    repoName: String,
    onBack: () -> Unit,
    onOpenIssue: (Int, Boolean) -> Unit,
    onNewIssue: () -> Unit,
    onOpenCommits: (String?) -> Unit,
    onOpenUser: (String) -> Unit = {},
    onOpenReleases: () -> Unit = {},
    onOpenRepoPeople: (String) -> Unit = {},
    viewModel: RepoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptics = appHaptics()

    // M3E hierarchy: a medium title (owner/repo) that collapses on scroll (E-004).
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(context.getString(it))
            viewModel.consumeActionMessage()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        "$owner/$repoName",
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
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val selectedTab = (state as? UiState.Content<RepoDetailUi>)?.value?.selectedTab
            if (selectedTab == 2) {
                // A labeled action: the text removes the guesswork about what
                // the round button does.
                ExtendedFloatingActionButton(
                    onClick = onNewIssue,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.fab_new_issue)) },
                )
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
                // Spatial tab transition: content slides towards its side of
                // the screen with a quiet spring from the active MotionScheme;
                // reading order stays stable (E-005).
                val tabSpring = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                val tabFadeIn = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                AnimatedContent(
                    targetState = ui.selectedTab,
                    transitionSpec = {
                        val forward = targetState > initialState
                        val dir = if (forward) 1 else -1
                        (fadeIn(tabFadeIn) + slideInHorizontally(tabSpring) { dir * it / 12 }) togetherWith
                            (fadeOut(tabFadeIn) + slideOutHorizontally(tabSpring) { -dir * it / 12 })
                    },
                    label = "repoTabContent",
                ) { tab ->
                    when (tab) {
                        0 -> AboutTab(
                            ui = ui,
                            viewModel = viewModel,
                            onOpenCommits = onOpenCommits,
                            onOpenUser = onOpenUser,
                            onOpenReleases = onOpenReleases,
                            onOpenRepoPeople = onOpenRepoPeople,
                        )
                        1 -> CodeTab(ui, viewModel)
                        2 -> IssuesTab(ui, viewModel, onOpenIssue)
                        else -> PullsTab(ui, viewModel, onOpenIssue)
                    }
                }
            }

            ui.fileContent?.let { file ->
                // Partially expanded first: the peek shows the file header;
                // dragging up reveals the preview (E-017).
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
                ModalBottomSheet(
                    onDismissRequest = viewModel::dismissFile,
                    sheetState = sheetState,
                ) {
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    file.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (file.size > 0) {
                                    Text(
                                        Formatters.bytes(file.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            IconButton(onClick = {
                                clipboard?.setPrimaryClip(ClipData.newPlainText(file.name, file.text))
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.copied_to_clipboard))
                                }
                            }) {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = stringResource(R.string.action_copy),
                                )
                            }
                            IconButton(onClick = {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TITLE, file.name)
                                    putExtra(Intent.EXTRA_TEXT, file.text)
                                }
                                context.startActivity(Intent.createChooser(send, null))
                            }) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = stringResource(R.string.file_share),
                                )
                            }
                            IconButton(onClick = {
                                val safeBranch = Uri.encode(ui.activeBranch)
                                val safePath = file.path.split('/').joinToString("/") { Uri.encode(it) }
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/$owner/$repoName/blob/$safeBranch/$safePath"),
                                    ),
                                )
                            }) {
                                Icon(
                                    Icons.Filled.OpenInNew,
                                    contentDescription = stringResource(R.string.file_open_in_browser),
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
                        val truncatedNote = stringResource(R.string.file_truncated, FILE_PREVIEW_MAX_LINES)
                        val cappedLines = remember(file.text) { file.text.split('\n') }
                        if (CodeLanguages.isMarkdown(file.name)) {
                            // A README-like file renders as formatted markdown.
                            MarkdownText(
                                markdown = cappedLines.take(FILE_PREVIEW_MAX_LINES).joinToString("\n") +
                                    if (cappedLines.size > FILE_PREVIEW_MAX_LINES) "\n\n$truncatedNote" else "",
                                textStyle = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .heightIn(max = 480.dp)
                                    .verticalScroll(rememberScrollState()),
                            )
                        } else {
                            // Code goes through the markdown pipeline as a fenced
                            // block so Prism syntax highlighting is reused; the
                            // preprocessor is bypassed to keep sources intact.
                            val fenced = remember(file.name, cappedLines, truncatedNote) {
                                val capped = cappedLines.take(FILE_PREVIEW_MAX_LINES).joinToString("\n")
                                val suffix = if (cappedLines.size > FILE_PREVIEW_MAX_LINES) {
                                    "\n\n$truncatedNote"
                                } else {
                                    ""
                                }
                                CodeLanguages.wrapInCodeFence(CodeLanguages.languageOf(file.name), capped) + suffix
                            }
                            MarkdownText(
                                markdown = fenced,
                                preprocess = false,
                                selectable = true,
                                textStyle = MaterialTheme.typography.bodySmall,
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
    onOpenCommits: (String?) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenReleases: () -> Unit,
    onOpenRepoPeople: (String) -> Unit,
) {
    val context = LocalContext.current
    val haptics = appHaptics()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
    ) {
        // Owner row: any login in the app is a doorway to the user profile.
        ui.repo.owner?.login?.let { ownerLogin ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onOpenUser(ownerLogin) }
                    .padding(vertical = Spacing.sm),
            ) {
                AsyncImage(
                    model = ui.repo.owner?.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape),
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(ownerLogin, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(Spacing.md))
        }

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
            RoundedActionButton(
                icon = Icons.Filled.CallSplit,
                contentDescription = stringResource(R.string.repo_fork_action),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.fork()
                },
            )
            RoundedActionButton(
                icon = Icons.Filled.History,
                contentDescription = stringResource(R.string.commits_title),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenCommits(ui.activeBranch.takeIf { it.isNotBlank() })
                },
            )
            RoundedActionButton(
                icon = Icons.Filled.NewReleases,
                contentDescription = stringResource(R.string.releases_title),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenReleases()
                },
            )
            RoundedActionButton(
                icon = Icons.Filled.OpenInNew,
                contentDescription = stringResource(R.string.repo_open_in_browser),
                onClick = {
                    ui.repo.htmlUrl?.let { url -> openUrl(context, url) }
                },
            )
        }

        Spacer(Modifier.height(Spacing.lg))

        // Stars and forks open their people lists; issues stay informational
        // because the Issues tab already lists them.
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenRepoPeople("stargazers")
                    },
            ) {
                StatBlock(
                    icon = Icons.Filled.Star,
                    value = Formatters.count(ui.repo.stargazersCount),
                    label = stringResource(R.string.repo_stat_stars),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenRepoPeople("forks")
                    },
            ) {
                StatBlock(
                    icon = Icons.Filled.CallSplit,
                    value = Formatters.count(ui.repo.forksCount),
                    label = stringResource(R.string.repo_stat_forks),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(Spacing.md))
            StatBlock(
                icon = Icons.Filled.BugReport,
                value = Formatters.count(ui.repo.openIssuesCount),
                label = stringResource(R.string.repo_stat_issues),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(Spacing.lg))

        // Meta facts on one quiet line: language, license, last push.
        val metaFragments = buildList {
            add(ui.repo.language ?: stringResource(R.string.repo_language_unknown))
            ui.repo.license?.spdxId?.takeIf { it.isNotBlank() && it != "NOASSERTION" }?.let {
                add(stringResource(R.string.repo_license_label, it))
            }
            Formatters.date(ui.repo.pushedAt)?.let { add(stringResource(R.string.repo_updated_label, it)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            LanguageDot(LanguageColors.colorFor(ui.repo.language))
            Spacer(Modifier.width(6.dp))
            Text(
                metaFragments.joinToString(separator = "  ·  "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Spacing.xl))
        EyebrowLabel(stringResource(R.string.repo_readme_section))

        when (val readme = ui.readme) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xl),
                    contentAlignment = Alignment.Center,
                ) {
                    LinearWavyProgressIndicator()
                }
            }
            is UiState.Error -> {
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
            is UiState.Content -> MarkdownText(
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
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = Spacing.md, vertical = Spacing.md)
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
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(Spacing.md))
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

/**
 * Issue triage controls (G-009): state chips, "assigned to me", label chips
 * from the repository palette, sort order and an in-list search box. The
 * filters go to the server so a repository with hundreds of issues stays
 * navigable; search narrows the loaded page without a request.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IssueFilters(ui: RepoDetailUi, viewModel: RepoViewModel) {
    val issues = ui.issues

    LaunchedEffect(Unit) { viewModel.loadLabels() }

    Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = issues.filter == "open",
                onClick = { viewModel.setIssueFilter("open") },
                label = { Text(stringResource(R.string.filter_open)) },
            )
            FilterChip(
                selected = issues.filter == "closed",
                onClick = { viewModel.setIssueFilter("closed") },
                label = { Text(stringResource(R.string.filter_closed)) },
            )
            FilterChip(
                selected = issues.assignee == "@me",
                onClick = viewModel::toggleAssignedToMe,
                label = { Text(stringResource(R.string.issue_filter_assigned_me)) },
            )
            FilterChip(
                selected = issues.sort == "comments",
                onClick = {
                    viewModel.setIssueSort(if (issues.sort == "comments") "created" else "comments")
                },
                label = { Text(stringResource(R.string.issue_filter_most_commented)) },
            )
        }

        OutlinedTextField(
            value = issues.search,
            onValueChange = viewModel::setIssueSearch,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
            placeholder = { Text(stringResource(R.string.issue_filter_search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (issues.search.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setIssueSearch("") }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.issue_filter_search_clear),
                        )
                    }
                }
            },
        )

        if (ui.labels.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                ui.labels.forEach { label ->
                    FilterChip(
                        selected = label.name in issues.labels,
                        onClick = { viewModel.toggleIssueLabel(label.name) },
                        label = { Text(label.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        }

        if (issues.hasFilters) {
            TextButton(onClick = viewModel::clearIssueFilters) {
                Text(stringResource(R.string.filters_clear))
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
    IssueFilters(ui = ui, viewModel = viewModel)
    ListWithFilter(
        filter = ui.issues.filter,
        onFilter = viewModel::setIssueFilter,
        items = ui.issues.visibleItems,
        loadingMore = ui.issues.loadingMore,
        loadMoreFailed = ui.issues.loadMoreFailed,
        hasMore = ui.issues.nextPage != null,
        onLoadMore = viewModel::loadIssuesMore,
        showStateChips = false,
        rowContent = { issue -> IssueRow(issue = issue, onClick = { onOpenIssue(issue.number, false) }) },
        emptyIcon = Icons.Outlined.BugReport,
        emptyTitle = if (ui.issues.hasFilters) {
            stringResource(R.string.issues_filter_empty_title)
        } else {
            stringResource(R.string.repo_issues_empty_title)
        },
        emptyBody = if (ui.issues.hasFilters) {
            stringResource(R.string.issues_filter_empty_body)
        } else {
            stringResource(R.string.repo_issues_empty_body)
        },
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
        loadMoreFailed = ui.pulls.loadMoreFailed,
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
    loadMoreFailed: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    rowContent: @Composable (T) -> Unit,
    emptyIcon: androidx.compose.ui.graphics.vector.ImageVector,
    emptyTitle: String,
    emptyBody: String,
    showStateChips: Boolean = true,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (showStateChips) {
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
                        LaunchedEffect(items.size, loadMoreFailed) {
                            if (!loadMoreFailed) onLoadMore()
                        }
                        LoadMoreFooter(
                            visible = loadingMore,
                            failed = loadMoreFailed,
                            onRetry = onLoadMore,
                        )
                    }
                }
            }
        }
    }
}
