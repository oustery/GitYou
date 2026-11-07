package com.gityou.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/*
 * One shape family with controlled tension: 12 / 20 / 32 / 48 dp.
 * Sharp corners are reserved for editorial accents, not scattered around.
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(48.dp),
)
