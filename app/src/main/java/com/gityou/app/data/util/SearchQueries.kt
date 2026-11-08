package com.gityou.app.data.util

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Builders for the GitHub search query strings. Kept as pure functions so
 * the exact query syntax stays unit-tested and the screens stay dumb.
 */
object SearchQueries {

    private val apiDate = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * "Trending this week": repositories pushed within the last [days] days
     * that already have at least [minStars] stars, most-starred first.
     * GitHub has no official trending endpoint; this approximation matches
     * what the mobile web shows for "actively popular" repositories.
     */
    fun trending(
        days: Int = 7,
        minStars: Int = 500,
        today: LocalDate = LocalDate.now(ZoneOffset.UTC),
    ): String {
        val since = today.minusDays(days.toLong()).format(apiDate)
        return "stars:>=$minStars pushed:>=$since"
    }

    /** Sort parameter that pairs with [trending] on the repositories endpoint. */
    const val TRENDING_SORT = "stars"
}
