package com.gityou.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * GitYou color system.
 *
 * Every accent palette is a full tonal family: primary (brand) + tertiary
 * (celebratory contrast) with hand-picked tonal steps for light and dark.
 * Secondary, error and neutral roles are shared between accents so the
 * surface language of the app stays recognizable no matter the palette.
 * Light and dark schemes are built independently, never inverted.
 */

/** User-selectable accent palettes. Values are persisted in DataStore by key. */
enum class AccentPalette(val key: String) {
    INDIGO("indigo"),
    OCEAN("ocean"),
    FOREST("forest"),
    SUNRISE("sunrise");

    companion object {
        fun fromKey(raw: String?): AccentPalette =
            entries.firstOrNull { it.key == raw } ?: INDIGO
    }
}

/** Tonal tokens of one accent: brand (primary) + contrast (tertiary). */
private data class AccentTokens(
    val primary10: Color,
    val primary20: Color,
    val primary30: Color,
    val primary40: Color,
    val primary80: Color,
    val primary90: Color,
    val primary95: Color,
    val tertiary10: Color,
    val tertiary20: Color,
    val tertiary30: Color,
    val tertiary40: Color,
    val tertiary80: Color,
    val tertiary90: Color,
    val tertiary95: Color,
)

// ---- Indigo / coral (default brand) -----------------------------------------
private val Indigo = AccentTokens(
    primary10 = Color(0xFF00105C), primary20 = Color(0xFF1B2078), primary30 = Color(0xFF333AA6),
    primary40 = Color(0xFF4648BE), primary80 = Color(0xFFBDC3FF), primary90 = Color(0xFFDEE0FF),
    primary95 = Color(0xFFEEF0FF),
    tertiary10 = Color(0xFF3E0502), tertiary20 = Color(0xFF5F170C), tertiary30 = Color(0xFF7D2C1C),
    tertiary40 = Color(0xFFB54630), tertiary80 = Color(0xFFFFB4A5), tertiary90 = Color(0xFFFFDAD3),
    tertiary95 = Color(0xFFFFEDE6),
)

// ---- Ocean / amber ------------------------------------------------------------
private val Ocean = AccentTokens(
    primary10 = Color(0xFF002021), primary20 = Color(0xFF003739), primary30 = Color(0xFF004F51),
    primary40 = Color(0xFF006A6C), primary80 = Color(0xFF4CDADC), primary90 = Color(0xFFC5F8F8),
    primary95 = Color(0xFFDEFDFB),
    tertiary10 = Color(0xFF2A1700), tertiary20 = Color(0xFF472A00), tertiary30 = Color(0xFF663E00),
    tertiary40 = Color(0xFF875500), tertiary80 = Color(0xFFFFB959), tertiary90 = Color(0xFFFFDDB4),
    tertiary95 = Color(0xFFFFEED3),
)

// ---- Forest / sky blue ---------------------------------------------------------
private val Forest = AccentTokens(
    primary10 = Color(0xFF0C2101), primary20 = Color(0xFF1F3704), primary30 = Color(0xFF354E18),
    primary40 = Color(0xFF4D661F), primary80 = Color(0xFFADD477), primary90 = Color(0xFFCCEE93),
    primary95 = Color(0xFFE6FFC6),
    tertiary10 = Color(0xFF001B3D), tertiary20 = Color(0xFF002E63), tertiary30 = Color(0xFF08458E),
    tertiary40 = Color(0xFF2E5DAE), tertiary80 = Color(0xFFB1C5FF), tertiary90 = Color(0xFFD9E2FF),
    tertiary95 = Color(0xFFEEF1FF),
)

// ---- Sunrise / violet -----------------------------------------------------------
private val Sunrise = AccentTokens(
    primary10 = Color(0xFF351000), primary20 = Color(0xFF551F00), primary30 = Color(0xFF772F00),
    primary40 = Color(0xFF9A4600), primary80 = Color(0xFFFFB77C), primary90 = Color(0xFFFFDCC2),
    primary95 = Color(0xFFFFEEDD),
    tertiary10 = Color(0xFF261433), tertiary20 = Color(0xFF3D2050), tertiary30 = Color(0xFF553769),
    tertiary40 = Color(0xFF6E4E83), tertiary80 = Color(0xFFD8B9EC), tertiary90 = Color(0xFFF5D9FF),
    tertiary95 = Color(0xFFFCEBFF),
)

private fun tokensFor(palette: AccentPalette): AccentTokens = when (palette) {
    AccentPalette.INDIGO -> Indigo
    AccentPalette.OCEAN -> Ocean
    AccentPalette.FOREST -> Forest
    AccentPalette.SUNRISE -> Sunrise
}

// ---- Shared roles -------------------------------------------------------------
private val Slate40 = Color(0xFF5B5D72)
private val Slate80 = Color(0xFFC4C5DD)
private val Slate90 = Color(0xFFE0E1F9)
private val Slate10 = Color(0xFF181A2C)
private val Slate20 = Color(0xFF2E3045)
private val Slate30 = Color(0xFF45485D)

private val Error40 = Color(0xFFBA1A1A)
private val Error80 = Color(0xFFFFB4AB)
private val Error90 = Color(0xFFFFDAD6)
private val Error10 = Color(0xFF410002)
private val Error20 = Color(0xFF690005)
private val Error30 = Color(0xFF93000A)

private val Neutral10 = Color(0xFF1B1B21)
private val Neutral90 = Color(0xFFE4E1E9)
private val NeutralVariant30 = Color(0xFF46464F)
private val NeutralVariant50 = Color(0xFF767680)
private val NeutralVariant80 = Color(0xFFC7C5D0)
private val NeutralVariant90 = Color(0xFFE3E1EC)

private const val LIGHT_BACKGROUND = 0xFFFBF8FE
private const val LIGHT_SURFACE_DIM = 0xFFDBD9E0

/** Light scheme with the selected accent palette. */
fun lightColors(palette: AccentPalette): ColorScheme {
    val t = tokensFor(palette)
    return lightColorScheme(
        primary = t.primary40,
        onPrimary = Color.White,
        primaryContainer = t.primary90,
        onPrimaryContainer = t.primary10,
        inversePrimary = t.primary80,
        secondary = Slate40,
        onSecondary = Color.White,
        secondaryContainer = Slate90,
        onSecondaryContainer = Slate10,
        tertiary = t.tertiary40,
        onTertiary = Color.White,
        tertiaryContainer = t.tertiary90,
        onTertiaryContainer = t.tertiary10,
        background = Color(LIGHT_BACKGROUND),
        onBackground = Neutral10,
        surface = Color(LIGHT_BACKGROUND),
        onSurface = Neutral10,
        surfaceVariant = NeutralVariant90,
        onSurfaceVariant = NeutralVariant30,
        surfaceTint = t.primary40,
        inverseSurface = Color(0xFF303036),
        inverseOnSurface = Color(0xFFF2F0F7),
        error = Error40,
        onError = Color.White,
        errorContainer = Error90,
        onErrorContainer = Error10,
        outline = NeutralVariant50,
        outlineVariant = NeutralVariant80,
        scrim = Color.Black,
        surfaceBright = Color(LIGHT_BACKGROUND),
        surfaceDim = Color(LIGHT_SURFACE_DIM),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF5F3FA),
        surfaceContainer = Color(0xFFEFEDF5),
        surfaceContainerHigh = Color(0xFFE9E7EF),
        surfaceContainerHighest = Color(0xFFE4E2EA),
        primaryFixed = t.primary90,
        primaryFixedDim = t.primary80,
        onPrimaryFixed = t.primary10,
        onPrimaryFixedVariant = t.primary30,
        secondaryFixed = Slate90,
        secondaryFixedDim = Slate80,
        onSecondaryFixed = Slate10,
        onSecondaryFixedVariant = Slate30,
        tertiaryFixed = t.tertiary90,
        tertiaryFixedDim = t.tertiary80,
        onTertiaryFixed = t.tertiary10,
        onTertiaryFixedVariant = t.tertiary30,
    )
}

/** Dark scheme with the selected accent palette, optionally pure black (AMOLED). */
fun darkColors(palette: AccentPalette, amoled: Boolean = false): ColorScheme {
    val t = tokensFor(palette)
    val base = darkColorScheme(
        primary = t.primary80,
        onPrimary = t.primary20,
        primaryContainer = t.primary30,
        onPrimaryContainer = t.primary95,
        inversePrimary = t.primary40,
        secondary = Slate80,
        onSecondary = Slate20,
        secondaryContainer = Slate30,
        onSecondaryContainer = Slate90,
        tertiary = t.tertiary80,
        onTertiary = t.tertiary20,
        tertiaryContainer = t.tertiary30,
        onTertiaryContainer = t.tertiary90,
        background = Color(0xFF131318),
        onBackground = Neutral90,
        surface = Color(0xFF131318),
        onSurface = Neutral90,
        surfaceVariant = NeutralVariant30,
        onSurfaceVariant = NeutralVariant80,
        surfaceTint = t.primary80,
        inverseSurface = Neutral90,
        inverseOnSurface = Color(0xFF303036),
        error = Error80,
        onError = Error20,
        errorContainer = Error30,
        onErrorContainer = Error90,
        outline = Color(0xFF91909A),
        outlineVariant = NeutralVariant30,
        scrim = Color.Black,
        surfaceBright = Color(0xFF3A3A40),
        surfaceDim = Color(0xFF131318),
        surfaceContainerLowest = Color(0xFF0E0E13),
        surfaceContainerLow = Color(0xFF1B1B20),
        surfaceContainer = Color(0xFF1F1F24),
        surfaceContainerHigh = Color(0xFF29292F),
        surfaceContainerHighest = Color(0xFF34343A),
        primaryFixed = t.primary90,
        primaryFixedDim = t.primary80,
        onPrimaryFixed = t.primary10,
        onPrimaryFixedVariant = t.primary30,
        secondaryFixed = Slate90,
        secondaryFixedDim = Slate80,
        onSecondaryFixed = Slate10,
        onSecondaryFixedVariant = Slate30,
        tertiaryFixed = t.tertiary90,
        tertiaryFixedDim = t.tertiary80,
        onTertiaryFixed = t.tertiary10,
        onTertiaryFixedVariant = t.tertiary30,
    )
    if (!amoled) return base
    // True black surfaces for OLED panels: saves power and maximizes contrast.
    return base.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF0C0C10),
        surfaceContainer = Color(0xFF121218),
        surfaceContainerHigh = Color(0xFF1B1B21),
        surfaceContainerHighest = Color(0xFF26262D),
        surfaceBright = Color(0xFF2C2C33),
        inverseOnSurface = Color(0xFF26262D),
    )
}

/** Brand color of a palette for previews in the settings screen. */
fun accentPreviewColor(palette: AccentPalette): Color = tokensFor(palette).primary40

// Accent used for state dots, unread markers and celebratory moments.
val UnreadAccentLight = Color(0xFFB54630)
val UnreadAccentDark = Color(0xFFFFB4A5)
