package com.gityou.app.ui.screens.issue

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gityou.app.R
import com.gityou.app.ui.components.errorMessageFor
import com.gityou.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
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
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.issue_new_submit))
                }
            }
        }
    }
}
