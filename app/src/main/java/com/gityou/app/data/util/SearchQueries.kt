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

    /**
     * The three questions a developer opens a GitHub client to answer
     * ("my work", G-010). Kept as pure builders so the exact syntax stays
     * unit-tested — GitHub search qualifiers are an API contract.
     */
    enum class MyWorkTab { ASSIGNED, CREATED, REVIEW_REQUESTED }

    fun myWorkQuery(tab: MyWorkTab): String = when (tab) {
        MyWorkTab.ASSIGNED -> "assignee:@me is:issue state:open"
        MyWorkTab.CREATED -> "author:@me is:issue state:open"
        // Review requests are pull requests only, and closed ones are noise.
        MyWorkTab.REVIEW_REQUESTED -> "review-requested:@me is:pr state:open"
    }

    /** Sort that makes ["my work"] actionable: freshest activity first. */
    const val MY_WORK_SORT = "updated"
}
