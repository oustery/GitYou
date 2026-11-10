package com.gityou.app

import com.gityou.app.data.util.DeepLinkRouting
import com.gityou.app.ui.navigation.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * App Links and "Share to GitYou" (G-012). The contract: a github.com link
 * opens the matching in-app screen, anything else is left to the browser
 * instead of navigating somewhere wrong.
 */
class DeepLinkRoutingTest {

    private fun route(url: String?) = DeepLinkRouting.route(
        url = url,
        issueRoute = Route::issue,
        repoRoute = Route::repo,
        userRoute = Route::user,
    )

    @Test
    fun `issue link opens the issue detail screen`() {
        assertEquals(
            "issue/octocat/Hello-World/1347?isPull=false",
            route("https://github.com/octocat/Hello-World/issues/1347"),
        )
    }

    @Test
    fun `pull request link opens the pull request screen`() {
        assertEquals(
            "issue/oustery/GitYou/42?isPull=true",
            route("https://github.com/oustery/GitYou/pull/42"),
        )
    }

    @Test
    fun `repository and profile links open their own screens`() {
        assertEquals("repo/oustery/GitYou", route("https://github.com/oustery/GitYou"))
        assertEquals("user/oustery", route("https://github.com/oustery"))
    }

    @Test
    fun `links the app cannot render are not hijacked`() {
        // Claiming these would drop the user somewhere other than where the
        // link pointed; they must fall through to the browser.
        assertNull(route("https://github.com/oustery/GitYou/blob/main/README.md"))
        assertNull(route("https://github.com/oustery/GitYou/releases"))
        assertNull(route("https://github.com/settings/tokens"))
        assertNull(route("https://gitlab.com/oustery/GitYou"))
        assertNull(route(null))
    }

    @Test
    fun `shared text yields its github link`() {
        // Share sheets prepend the poster app's blurb to the URL.
        val shared = "Look at this! https://github.com/oustery/GitYou/issues/7 cc @octocat"
        assertEquals(
            "https://github.com/oustery/GitYou/issues/7",
            DeepLinkRouting.firstGitHubUrl(shared),
        )
        assertNull(DeepLinkRouting.firstGitHubUrl("no link here"))
        assertNull(DeepLinkRouting.firstGitHubUrl(""))
    }
}
