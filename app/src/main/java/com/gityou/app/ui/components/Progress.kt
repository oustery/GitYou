package com.gityou.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.gityou.app.ui.theme.Spacing

/**
 * Wavy pagination footer shown while the next page is loading.
 * Indeterminate because no meaningful fraction exists yet.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadMoreFooter(visible: Boolean) {
    if (!visible) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        LinearWavyProgressIndicator()
    }
}
