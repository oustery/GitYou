package com.gityou.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gityou.app.R
import com.gityou.app.ui.theme.Spacing

/**
 * Wavy pagination footer shown while the next page is loading.
 * Indeterminate because no meaningful fraction exists yet.
 *
 * When the previous page failed ([failed] = true) the spinner is replaced by
 * an explicit retry button, so a single network error can never wedge the
 * list forever (F-013).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadMoreFooter(
    visible: Boolean,
    failed: Boolean = false,
    onRetry: (() -> Unit)? = null,
) {
    if (!visible && !failed) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        if (failed) {
            TextButton(onClick = { onRetry?.invoke() }) {
                Text(
                    stringResource(R.string.action_retry),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            LinearWavyProgressIndicator()
        }
    }
}
