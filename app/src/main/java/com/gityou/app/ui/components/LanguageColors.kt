package com.gityou.app.ui.components

import androidx.compose.ui.graphics.Color

/** Colors for common GitHub languages (mirrors linguist colors, falls back to null). */
object LanguageColors {

    private val colors = mapOf(
        "kotlin" to Color(0xFFA97BFF),
        "java" to Color(0xFFB07219),
        "python" to Color(0xFF3572A5),
        "javascript" to Color(0xFFF1E05A),
        "typescript" to Color(0xFF3178C6),
        "go" to Color(0xFF00ADD8),
        "rust" to Color(0xFFDEA584),
        "c" to Color(0xFF555555),
        "c++" to Color(0xFFF34B7D),
        "c#" to Color(0xFF178600),
        "swift" to Color(0xFFF05138),
        "dart" to Color(0xFF00B4AB),
        "ruby" to Color(0xFF701516),
        "php" to Color(0xFF4F5D95),
        "html" to Color(0xFFE34C26),
        "css" to Color(0xFF563D7C),
        "shell" to Color(0xFF89E051),
        "scala" to Color(0xFFC22340),
        "lua" to Color(0xFF000080),
        "vue" to Color(0xFF41B883),
    )

    fun colorFor(language: String?): Color? = language?.lowercase()?.let { colors[it] }
}
