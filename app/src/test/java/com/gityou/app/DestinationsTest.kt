package com.gityou.app

import com.gityou.app.ui.navigation.Route
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Route builders are the boundary between user data (branch names, tags) and
 * the navigation graph: URI-hostile characters must survive the round trip
 * through the route string (N-001).
 */
class DestinationsTest {

    @Test
    fun `ref with a fragment character stays inside the query value`() {
        val route = Route.commits("octocat", "demo", ref = "bugfix/#42")
        assertEquals("commits/octocat/demo?ref=bugfix%2F%2342", route)
    }

    @Test
    fun `ref with ampersand and spaces is fully percent-encoded`() {
        val route = Route.commits("octocat", "demo", ref = "release& demo 2")
        assertEquals("commits/octocat/demo?ref=release%26%20demo%202", route)
    }

    @Test
    fun `branch names with slashes round-trip through percent-encoding`() {
        // "/" is not an unreserved character, so it is escaped; Navigation
        // decodes query-parameter values back before they reach the VM.
        assertEquals(
            "commits/octocat/demo?ref=feature%2Flogin-flow",
            Route.commits("octocat", "demo", ref = "feature/login-flow"),
        )
    }

    @Test
    fun `null or blank ref omits the query entirely`() {
        assertEquals("commits/octocat/demo", Route.commits("octocat", "demo", ref = null))
        assertEquals("commits/octocat/demo", Route.commits("octocat", "demo", ref = ""))
    }

    @Test
    fun `non-ASCII branch names encode as UTF-8 percent triplets`() {
        // "ветка" in UTF-8: D0 B2 D0 B5 D1 82 D0 BA D0 B0
        assertEquals(
            "commits/octocat/demo?ref=%D0%B2%D0%B5%D1%82%D0%BA%D0%B0",
            Route.commits("octocat", "demo", ref = "ветка"),
        )
    }

    @Test
    fun `unreserved characters are never encoded`() {
        assertEquals("-._~0aZ9", Route.encodeArg("-._~0aZ9"))
    }
}
