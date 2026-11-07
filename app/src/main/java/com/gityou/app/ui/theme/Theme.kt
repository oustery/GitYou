package com.gityou.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** User-selectable app-wide text scale applied through LocalDensity. */
enum class TextScale(val key: String, val factor: Float) {
    SMALL("small", 0.9f),
    NORMAL("normal", 1.0f),
    LARGE("large", 1.15f),
    EXTRA_LARGE("extra_large", 1.3f);

    companion object {
        fun fromKey(raw: String?): TextScale =
            entries.firstOrNull { it.key == raw } ?: NORMAL
    }
}

/**
 * GitYou theme.
 *
 * - Branded fallback palettes on all API levels with four selectable accents.
 * - Optional true-black surfaces for OLED panels in dark mode.
 * - Material You dynamic color on Android 12+ behind a user setting.
 * - Light and dark schemes are designed independently, never inverted.
 * - Motion switches between the expressive scheme and the standard one so
 *   users who prefer calm interfaces can turn expressive springs off.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GitYouTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    useDynamicColor: Boolean = false,
    accent: AccentPalette = AccentPalette.INDIGO,
    amoled: Boolean = false,
    expressiveMotion: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) darkColors(accent, amoled) else lightColors(accent)
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        motionScheme = if (expressiveMotion) MotionScheme.expressive() else MotionScheme.standard(),
        content = content,
    )
}
