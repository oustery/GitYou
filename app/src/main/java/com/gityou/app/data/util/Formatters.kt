package com.gityou.app.data.util

import java.time.Duration
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

/**
 * Coarse relative-time bucket for list rows. The number stays a plain value
 * here so unit tests run without Android; the UI layer maps buckets onto
 * localized plurals.
 */
sealed interface RelativeTime {
    data object Now : RelativeTime
    data class Minutes(val value: Int) : RelativeTime
    data class Hours(val value: Int) : RelativeTime
    data class Days(val value: Int) : RelativeTime
    data class Absolute(val text: String) : RelativeTime
}

/** Parses an API timestamp, null when missing or malformed. */
fun parseTimestamp(iso: String?): OffsetDateTime? {
    if (iso.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(iso)
    } catch (_: Exception) {
        null
    }
}

/**
 * "2 ч назад"-style bucket for a timestamp. Within a week relative wins,
 * beyond it the absolute medium date reads better; clock skew clamps to now.
 */
fun Formatters.relativeOrAbsolute(iso: String?, now: OffsetDateTime = OffsetDateTime.now()): RelativeTime {
    val date = parseTimestamp(iso) ?: return RelativeTime.Absolute(Formatters.date(iso) ?: "")
    val seconds = Duration.between(date, now).seconds
    return when {
        seconds < 60 -> RelativeTime.Now
        seconds < 3_600 -> RelativeTime.Minutes((seconds / 60).toInt())
        seconds < 86_400 -> RelativeTime.Hours((seconds / 3_600).toInt())
        seconds < 7 * 86_400 -> RelativeTime.Days((seconds / 86_400).toInt())
        else -> RelativeTime.Absolute(Formatters.date(iso) ?: "")
    }
}
