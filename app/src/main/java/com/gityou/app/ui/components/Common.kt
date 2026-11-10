package com.gityou.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import com.gityou.app.R
import com.gityou.app.data.util.ApiErrors
import com.gityou.app.ui.theme.Spacing

/** Load/error/content triad used by every screen. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Error(val errorKey: String) : UiState<Nothing>
    data class Content<T>(val value: T) : UiState<T>
}

/**
 * Opens an external URL and never crashes the app: devices without a web
 * handler (Android TV, bare emulators) throw ActivityNotFoundException from
 * plain `startActivity(ACTION_VIEW)` (N-003). Same policy as ApkInstaller.
 */
fun openUrl(context: Context, url: String?) {
    if (url.isNullOrBlank()) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        // No browser on this device: nothing sensible to route to.
    }
}

/** Single key → resource mapping; [errorMessageFor] and snackbars both use it. */
@StringRes
fun errorKeyRes(errorKey: String): Int = when (errorKey) {
    ApiErrors.KEY_UNAUTHORIZED -> R.string.error_unauthorized
    ApiErrors.KEY_RATE_LIMIT -> R.string.error_rate_limit
    ApiErrors.KEY_FORBIDDEN -> R.string.error_forbidden
    ApiErrors.KEY_NOT_FOUND -> R.string.error_not_found
    ApiErrors.KEY_VALIDATION -> R.string.error_validation
    ApiErrors.KEY_NETWORK -> R.string.error_network
    ApiErrors.KEY_HTTP -> R.string.error_http
    else -> R.string.error_unknown
}

/** Localized user-facing message for an [ApiErrors] key. */
@Composable
fun errorMessageFor(errorKey: String): String = stringResource(errorKeyRes(errorKey))

/** Whether tactile feedback is on; the user setting is provided at the app root. */
val LocalHapticsEnabled = staticCompositionLocalOf { true }

private val NoHaptics = object : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}

/**
 * Haptic feedback that respects the user setting: returns the platform
 * feedback object or a silent stub, so call sites stay unconditional.
 */
@Composable
fun appHaptics(): HapticFeedback {
    val enabled = LocalHapticsEnabled.current
    val platform = LocalHapticFeedback.current
    return if (enabled) platform else NoHaptics
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FullScreenLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoadingIndicator()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        // Expressive shape container: states play inside the shape system
        // instead of floating as a bare glyph (E-016).
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(MaterialShapes.Cookie12Sided.toShape())
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
            )
        }
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(MaterialShapes.Cookie12Sided.toShape())
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
            )
        }
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
/**
 * Section label. Marked as a heading so TalkBack can jump between sections
 * instead of reading the whole screen linearly (G-006).
 */
@Composable
fun EyebrowLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .semantics { heading() }
            .padding(bottom = Spacing.sm),
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
