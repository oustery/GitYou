package com.gityou.app.ui.screens.creation

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gityou.app.R
import com.gityou.app.ui.theme.Spacing

/** Form for `POST /gists`: description, one file, visibility. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewGistScreen(
    onBack: () -> Unit,
    viewModel: NewGistViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_gist_title)) },
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
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            OutlinedTextField(
                value = ui.description,
                onValueChange = { value -> viewModel.patch { it.copy(description = value) } },
                label = { Text(stringResource(R.string.new_gist_description)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ui.filename,
                onValueChange = { value -> viewModel.patch { it.copy(filename = value) } },
                label = { Text(stringResource(R.string.new_gist_filename)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ui.content,
                onValueChange = { value -> viewModel.patch { it.copy(content = value) } },
                label = { Text(stringResource(R.string.new_gist_content)) },
                minLines = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.new_gist_public),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = ui.public,
                    onCheckedChange = { value -> viewModel.patch { it.copy(public = value) } },
                )
            }
            if (ui.failed) {
                Text(
                    stringResource(R.string.error_generic_action),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = {
                    viewModel.submit { url ->
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        if (url.isNotBlank() && clipboard != null) {
                            clipboard.setPrimaryClip(ClipData.newPlainText("gist", url))
                        }
                        onBack()
                    }
                },
                enabled = !ui.submitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (ui.submitting) {
                    CircularWavyProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(Spacing.sm))
                }
                Text(stringResource(R.string.new_gist_submit))
            }
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}
