package com.gityou.app.data.util

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Locale-aware formatting helpers for API values. */
object Formatters {

    private val dateRegex = Regex("""(\d{4})-(\d{2})-(\d{2})""")

    /** "2025-04-13T07:12:33Z" -> "13 Apr 2025" (localized). Null if unparsable. */
    fun date(iso: String?): String? {
        if (iso.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(iso).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        } catch (_: Exception) {
            dateRegex.find(iso)?.let { "${it.groupValues[3]} ${monthShort(it.groupValues[2].toInt())} ${it.groupValues[1]}" }
        }
    }

    /** 1234 -> "1.2k", 1234567 -> "1.2M". */
    fun count(value: Int): String = when {
        value >= 1_000_000 -> "%.1fM".format(value / 1_000_000.0)
        value >= 10_000 -> "${value / 1000}k"
        value >= 1_000 -> "%.1fk".format(value / 1000.0)
        else -> value.toString()
    }

    /** Human-readable file size, localized for RU and EN: 249 -> "249 Б"/"249 B". */
    fun bytes(value: Long): String {
        val ru = Locale.getDefault().language == "ru"
        val unit = when {
            value < 1024 -> if (ru) "Б" else "B"
            value < 1024 * 1024 -> if (ru) "КБ" else "KB"
            else -> if (ru) "МБ" else "MB"
        }
        val scaled = when {
            value < 1024 -> value.toDouble()
            value < 1024 * 1024 -> value / 1024.0
            else -> value / 1024.0 / 1024.0
        }
        return if (scaled % 1.0 == 0.0 && value < 1024) {
            "${scaled.toInt()} $unit"
        } else {
            "%.1f $unit".format(scaled)
        }
    }

    private fun monthShort(month: Int): String = when (month) {
        1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
        7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; else -> "Dec"
    }
}
