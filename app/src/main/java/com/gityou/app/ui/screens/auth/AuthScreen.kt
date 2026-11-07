package com.gityou.app.ui.screens.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gityou.app.R
import com.gityou.app.data.util.ApiErrors
import com.gityou.app.ui.components.errorMessageFor
import com.gityou.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val deviceFlow by viewModel.deviceFlow.collectAsStateWithLifecycle()
    val patState by viewModel.patState.collectAsStateWithLifecycle()
    val clientId by viewModel.clientId.collectAsStateWithLifecycle()
    val clientIdSaved by viewModel.clientIdSaved.collectAsStateWithLifecycle()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl, vertical = Spacing.hero),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(modifier = Modifier.padding(Spacing.lg)) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        Text(
            stringResource(R.string.auth_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.sm),
        )
        Spacer(Modifier.height(Spacing.xxl))

        // ---- OAuth Device Flow ----
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Text(stringResource(R.string.auth_device_flow_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.auth_device_flow_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )

                if (clientId.isBlank() || !clientIdSaved) {
                    Spacer(Modifier.height(Spacing.md))
                    OutlinedTextField(
                        value = clientId,
                        onValueChange = viewModel::onClientIdChanged,
                        label = { Text(stringResource(R.string.auth_client_id_label)) },
                        supportingText = { Text(stringResource(R.string.auth_client_id_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        onClick = viewModel::saveClientId,
                        enabled = clientId.isNotBlank(),
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }

                when (deviceFlow.phase) {
                    DeviceFlowPhase.IDLE -> {
                        Spacer(Modifier.height(Spacing.md))
                        Button(
                            onClick = viewModel::startDeviceFlow,
                            enabled = clientIdSaved && clientId.isNotBlank(),
                        ) {
                            Text(stringResource(R.string.auth_device_flow_button))
                        }
                    }
                    DeviceFlowPhase.WAITING -> {
                        Spacer(Modifier.height(Spacing.lg))
                        Text(
                            stringResource(R.string.auth_enter_code_at, deviceFlow.verificationUri),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.md),
                        ) {
                            Text(
                                deviceFlow.userCode,
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                textAlign = TextAlign.Center,
                                letterSpacing = 4.sp,
                                modifier = Modifier.padding(vertical = Spacing.lg),
                            )
                        }
                        LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            stringResource(R.string.auth_waiting_confirmation),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.md),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            FilledTonalButton(onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(deviceFlow.verificationUri)),
                                )
                            }) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = null)
                                Spacer(Modifier.width(Spacing.sm))
                                Text(stringResource(R.string.auth_open_verification))
                            }
                            TextButton(onClick = viewModel::cancelDeviceFlow) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        }
                    }
                    DeviceFlowPhase.EXPIRED -> {
                        Spacer(Modifier.height(Spacing.md))
                        Text(stringResource(R.string.auth_code_expired), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(Spacing.sm))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            Button(onClick = viewModel::startDeviceFlow) {
                                Text(stringResource(R.string.auth_try_again))
                            }
                            TextButton(onClick = viewModel::cancelDeviceFlow) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        }
                    }
                    DeviceFlowPhase.DENIED -> {
                        Spacer(Modifier.height(Spacing.md))
                        Text(stringResource(R.string.auth_code_denied), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(Spacing.sm))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            Button(onClick = viewModel::startDeviceFlow) {
                                Text(stringResource(R.string.auth_try_again))
                            }
                            TextButton(onClick = viewModel::cancelDeviceFlow) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        }
                    }
                    DeviceFlowPhase.ERROR -> {
                        Spacer(Modifier.height(Spacing.md))
                        Text(
                            if (deviceFlow.errorKey == "missing_client_id") {
                                stringResource(R.string.auth_missing_client_id)
                            } else {
                                errorMessageFor(deviceFlow.errorKey)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            if (deviceFlow.errorKey != "missing_client_id") {
                                Button(onClick = viewModel::startDeviceFlow) {
                                    Text(stringResource(R.string.auth_try_again))
                                }
                            }
                            TextButton(onClick = viewModel::cancelDeviceFlow) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        // ---- Personal access token fallback ----
        PatCard(
            patState = patState,
            onSignIn = viewModel::signInWithPat,
        )
    }
}

@Composable
private fun PatCard(
    patState: PatState,
    onSignIn: (String) -> Unit,
) {
    // Plain remember, NOT rememberSaveable: a token must never land in the
    // saved instance state bundle, where the system (and logs) can read it (F-024).
    var token by remember { mutableStateOf("") }
    var showToken by rememberSaveable { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(stringResource(R.string.auth_pat_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.auth_pat_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.md),
            )
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text(stringResource(R.string.auth_pat_label)) },
                singleLine = true,
                isError = patState.phase == PatPhase.ERROR,
                visualTransformation = if (showToken) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { showToken = !showToken }) {
                        Icon(
                            if (showToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(
                                if (showToken) R.string.auth_hide_token else R.string.auth_show_token,
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (patState.phase == PatPhase.ERROR) {
                Text(
                    errorMessageFor(patState.errorKey),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }
            if (patState.errorKey == ApiErrors.KEY_UNAUTHORIZED) {
                Text(
                    stringResource(R.string.auth_pat_scopes_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
            Button(
                onClick = { onSignIn(token) },
                enabled = token.isNotBlank() && patState.phase != PatPhase.SUBMITTING,
                modifier = Modifier
                    .padding(top = Spacing.md)
                    .align(Alignment.End),
            ) {
                Text(stringResource(R.string.auth_pat_button))
            }
        }
    }
}
