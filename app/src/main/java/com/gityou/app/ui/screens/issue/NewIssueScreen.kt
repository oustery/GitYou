package com.gityou.app.ui.screens.issue

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gityou.app.R
import com.gityou.app.ui.components.MarkdownText
import com.gityou.app.ui.components.errorMessageFor
import com.gityou.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewIssueScreen(
    owner: String,
    repoName: String,
    onBack: () -> Unit,
    onCreated: (Int) -> Unit,
    viewModel: NewIssueViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(state.createdNumber) {
        state.createdNumber?.let { onCreated(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.issue_new_title)) },
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
        ) {
            Text(
                stringResource(R.string.issue_new_repo_line, owner, repoName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.issue_new_field_title)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg),
            )
            if (selectedTab == 0) {
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text(stringResource(R.string.issue_new_field_body)) },
                    supportingText = { Text(stringResource(R.string.issue_new_markdown_hint)) },
                    minLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.lg),
                )
            }

            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg),
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.issue_new_tab_write)) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.issue_new_tab_preview)) },
                )
            }

            if (selectedTab == 1) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.lg),
                ) {
                    if (body.isBlank()) {
                        Text(
                            stringResource(R.string.issue_new_preview_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(Spacing.lg),
                        )
                    } else {
                        MarkdownText(
                            markdown = body,
                            modifier = Modifier.padding(Spacing.lg),
                        )
                    }
                }
            }

            if (state.phase is NewIssuePhase.Error) {
                Text(
                    errorMessageFor((state.phase as NewIssuePhase.Error).errorKey),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = Spacing.md),
                )
            }
            Button(
                onClick = { viewModel.createIssue(title, body) },
                enabled = title.isNotBlank() && state.phase != NewIssuePhase.Submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xl),
            ) {
                if (state.phase == NewIssuePhase.Submitting) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Text(stringResource(R.string.issue_new_submit))
                }
            }
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}
