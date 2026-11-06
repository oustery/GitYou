package com.gityou.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gityou.app.R
import com.gityou.app.data.util.ApiErrors
import com.gityou.app.ui.theme.Spacing

/** Load/error/content triad used by every screen. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Error(val errorKey: String) : UiState<Nothing>
    data class Content<T>(val value: T) : UiState<T>
}

/** Localized user-facing message for an [ApiErrors] key. */
@Composable
fun errorMessageFor(errorKey: String): String = when (errorKey) {
    ApiErrors.KEY_UNAUTHORIZED -> stringResource(R.string.error_unauthorized)
    ApiErrors.KEY_RATE_LIMIT -> stringResource(R.string.error_rate_limit)
    ApiErrors.KEY_FORBIDDEN -> stringResource(R.string.error_forbidden)
    ApiErrors.KEY_NOT_FOUND -> stringResource(R.string.error_not_found)
    ApiErrors.KEY_VALIDATION -> stringResource(R.string.error_validation)
    ApiErrors.KEY_NETWORK -> stringResource(R.string.error_network)
    ApiErrors.KEY_HTTP -> stringResource(R.string.error_http)
    else -> stringResource(R.string.error_unknown)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FullScreenLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoadingIndicator()
    }
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(48.dp),
        )
        Text(
            message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.lg),
        )
        onRetry?.let {
            Button(onClick = it, modifier = Modifier.padding(top = Spacing.lg)) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(56.dp),
        )
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.lg),
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.sm),
        )
    }
}

/** Small uppercase section marker that organizes list sections. */
@Composable
fun EyebrowLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(bottom = Spacing.sm),
    )
}

@Composable
fun <T> StateScreen(
    state: UiState<T>,
    onRetry: () -> Unit,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        UiState.Loading -> FullScreenLoading()
        is UiState.Error -> ErrorState(
            message = errorMessageFor(state.errorKey),
            onRetry = onRetry,
        )
        is UiState.Content -> content(state.value)
    }
}
