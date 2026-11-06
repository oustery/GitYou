package com.gityou.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * GitYou brand palette: indigo primary with a coral tertiary accent.
 * Light and dark schemes are built independently (not inverted) so every
 * role keeps a deliberate tonal relationship inside its own scheme.
 */

// ---- Indigo (primary) -------------------------------------------------------
private val Indigo40 = Color(0xFF4648BE)
private val Indigo80 = Color(0xFFBDC3FF)
private val Indigo90 = Color(0xFFDEE0FF)
private val Indigo95 = Color(0xFFEEF0FF)
private val Indigo10 = Color(0xFF00105C)
private val Indigo20 = Color(0xFF1B2078)
private val Indigo30 = Color(0xFF333AA6)

// ---- Muted slate-indigo (secondary) ----------------------------------------
private val Slate40 = Color(0xFF5B5D72)
private val Slate80 = Color(0xFFC4C5DD)
private val Slate90 = Color(0xFFE0E1F9)
private val Slate10 = Color(0xFF181A2C)
private val Slate20 = Color(0xFF2E3045)
private val Slate30 = Color(0xFF45485D)

// ---- Coral (tertiary) -------------------------------------------------------
private val Coral40 = Color(0xFFB54630)
private val Coral80 = Color(0xFFFFB4A5)
private val Coral90 = Color(0xFFFFDAD3)
private val Coral10 = Color(0xFF3E0502)
private val Coral20 = Color(0xFF5F170C)
private val Coral30 = Color(0xFF7D2C1C)

// ---- Error ------------------------------------------------------------------
private val Error40 = Color(0xFFBA1A1A)
private val Error80 = Color(0xFFFFB4AB)
private val Error90 = Color(0xFFFFDAD6)
private val Error10 = Color(0xFF410002)
private val Error20 = Color(0xFF690005)
private val Error30 = Color(0xFF93000A)

// ---- Neutrals (cool, slightly indigo-tinted) --------------------------------
private val Neutral10 = Color(0xFF1B1B21)
private val Neutral90 = Color(0xFFE4E1E9)
private val NeutralVariant30 = Color(0xFF46464F)
private val NeutralVariant50 = Color(0xFF767680)
private val NeutralVariant80 = Color(0xFFC7C5D0)
private val NeutralVariant90 = Color(0xFFE3E1EC)

val LightColors = lightColorScheme(
    primary = Indigo40,
    onPrimary = Color.White,
    primaryContainer = Indigo90,
    onPrimaryContainer = Indigo10,
    inversePrimary = Indigo80,
    secondary = Slate40,
    onSecondary = Color.White,
    secondaryContainer = Slate90,
    onSecondaryContainer = Slate10,
    tertiary = Coral40,
    onTertiary = Color.White,
    tertiaryContainer = Coral90,
    onTertiaryContainer = Coral10,
    background = Color(0xFFFBF8FE),
    onBackground = Neutral10,
    surface = Color(0xFFFBF8FE),
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    surfaceTint = Indigo40,
    inverseSurface = Color(0xFF303036),
    inverseOnSurface = Color(0xFFF2F0F7),
    error = Error40,
    onError = Color.White,
    errorContainer = Error90,
    onErrorContainer = Error10,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    scrim = Color.Black,
    surfaceBright = Color(0xFFFBF8FE),
    surfaceDim = Color(0xFFDBD9E0),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF5F3FA),
    surfaceContainer = Color(0xFFEFEDF5),
    surfaceContainerHigh = Color(0xFFE9E7EF),
    surfaceContainerHighest = Color(0xFFE4E2EA),
    primaryFixed = Indigo90,
    primaryFixedDim = Indigo80,
    onPrimaryFixed = Indigo10,
    onPrimaryFixedVariant = Indigo30,
    secondaryFixed = Slate90,
    secondaryFixedDim = Slate80,
    onSecondaryFixed = Slate10,
    onSecondaryFixedVariant = Slate30,
    tertiaryFixed = Coral90,
    tertiaryFixedDim = Coral80,
    onTertiaryFixed = Coral10,
    onTertiaryFixedVariant = Coral30,
)

val DarkColors = darkColorScheme(
    primary = Indigo80,
    onPrimary = Indigo20,
    primaryContainer = Indigo30,
    onPrimaryContainer = Indigo95,
    inversePrimary = Indigo40,
    secondary = Slate80,
    onSecondary = Slate20,
    secondaryContainer = Slate30,
    onSecondaryContainer = Slate90,
    tertiary = Coral80,
    onTertiary = Coral20,
    tertiaryContainer = Coral30,
    onTertiaryContainer = Coral90,
    background = Color(0xFF131318),
    onBackground = Neutral90,
    surface = Color(0xFF131318),
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    surfaceTint = Indigo80,
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
    primaryFixed = Indigo90,
    primaryFixedDim = Indigo80,
    onPrimaryFixed = Indigo10,
    onPrimaryFixedVariant = Indigo30,
    secondaryFixed = Slate90,
    secondaryFixedDim = Slate80,
    onSecondaryFixed = Slate10,
    onSecondaryFixedVariant = Slate30,
    tertiaryFixed = Coral90,
    tertiaryFixedDim = Coral80,
    onTertiaryFixed = Coral10,
    onTertiaryFixedVariant = Coral30,
)

// Accent used for state dots, unread markers and celebratory moments.
val UnreadAccentLight = Coral40
val UnreadAccentDark = Coral80
