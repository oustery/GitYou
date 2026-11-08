package com.gityou.app

import com.gityou.app.data.util.SearchQueries
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The trending query syntax is an API contract with GitHub search. */
class SearchQueriesTest {

    @Test
    fun `trending query pins stars and recency window`() {
        val query = SearchQueries.trending(days = 7, minStars = 500, today = LocalDate.of(2026, 11, 8))
        assertEquals("stars:>=500 pushed:>=2026-11-01", query)
    }

    @Test
    fun `trending window rolls back across month boundaries`() {
        val query = SearchQueries.trending(days = 7, minStars = 1, today = LocalDate.of(2026, 3, 3))
        assertTrue(query.contains("pushed:>=2026-02-24"))
        assertTrue(query.contains("stars:>=1"))
    }

    @Test
    fun `trending sort targets the stars column`() {
        assertEquals("stars", SearchQueries.TRENDING_SORT)
    }
}
