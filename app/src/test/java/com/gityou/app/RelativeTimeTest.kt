package com.gityou.app

import com.gityou.app.data.util.Formatters
import com.gityou.app.data.util.RelativeTime
import com.gityou.app.data.util.relativeOrAbsolute
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelativeTimeTest {

    private val now: OffsetDateTime = OffsetDateTime.of(2026, 11, 6, 12, 0, 0, 0, ZoneOffset.UTC)

    private fun secondsAgo(seconds: Long): OffsetDateTime = now.minusSeconds(seconds)

    @Test
    fun `under a minute is now`() {
        assertEquals(RelativeTime.Now, Formatters.relativeOrAbsolute(secondsAgo(30).toString(), now))
    }

    @Test
    fun `future timestamps clamp to now`() {
        assertEquals(RelativeTime.Now, Formatters.relativeOrAbsolute(now.plusSeconds(120).toString(), now))
    }

    @Test
    fun `minutes bucket up to an hour`() {
        assertEquals(RelativeTime.Minutes(2), Formatters.relativeOrAbsolute(secondsAgo(120).toString(), now))
        assertEquals(RelativeTime.Minutes(59), Formatters.relativeOrAbsolute(secondsAgo(3_540).toString(), now))
    }

    @Test
    fun `hours bucket up to a day`() {
        assertEquals(RelativeTime.Hours(1), Formatters.relativeOrAbsolute(secondsAgo(3_600).toString(), now))
        assertEquals(RelativeTime.Hours(23), Formatters.relativeOrAbsolute(secondsAgo(86_340).toString(), now))
    }

    @Test
    fun `days bucket up to a week`() {
        assertEquals(RelativeTime.Days(1), Formatters.relativeOrAbsolute(secondsAgo(86_400).toString(), now))
        assertEquals(RelativeTime.Days(6), Formatters.relativeOrAbsolute(secondsAgo(6 * 86_400).toString(), now))
    }

    @Test
    fun `beyond a week falls back to absolute date`() {
        val time = Formatters.relativeOrAbsolute(secondsAgo(30 * 86_400).toString(), now)
        assertTrue(time is RelativeTime.Absolute)
        assertTrue((time as RelativeTime.Absolute).text.isNotBlank())
    }

    @Test
    fun `null or malformed input yields absolute empty`() {
        assertEquals(RelativeTime.Absolute(""), Formatters.relativeOrAbsolute(null, now))
        assertEquals(RelativeTime.Absolute(""), Formatters.relativeOrAbsolute("not-a-date", now))
    }
}
