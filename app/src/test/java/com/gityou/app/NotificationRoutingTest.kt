package com.gityou.app

import com.gityou.app.data.util.NotificationRouting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Notification routing matrix (G-021). The bug this guards: the trailing id of
 * `subject.url` used to be read as an issue number for every notification
 * type, so Release/Discussion/Commit taps navigated to a non-existent issue.
 */
class NotificationRoutingTest {

    private fun resolve(type: String?, url: String?) = NotificationRouting.resolve(type, url)

    @Test
    fun `issue notification opens the issue thread`() {
        val destination = resolve(
            "Issue",
            "https://api.github.com/repos/octocat/Hello-World/issues/1347",
        )
        val thread = destination as NotificationRouting.Destination.Thread
        assertEquals("octocat", thread.target.owner)
        assertEquals("Hello-World", thread.target.repo)
        assertEquals(1347, thread.target.number)
        assertEquals(false, thread.target.isPull)
    }

    @Test
    fun `pull request notification opens the pull thread`() {
        val destination = resolve(
            "PullRequest",
            "https://api.github.com/repos/oustery/GitYou/pulls/42",
        )
        val thread = destination as NotificationRouting.Destination.Thread
        assertEquals(42, thread.target.number)
        assertTrue(thread.target.isPull)
    }

    @Test
    fun `release notification does not treat the release id as an issue number`() {
        // Real shape: /releases/12345678 — opening issue 12345678 was the 404.
        val destination = resolve(
            "Release",
            "https://api.github.com/repos/oustery/GitYou/releases/12345678",
        )
        val repository = destination as NotificationRouting.Destination.Repository
        assertEquals("oustery", repository.owner)
        assertEquals("GitYou", repository.repo)
    }

    @Test
    fun `discussion commit and check suite land on the repository`() {
        listOf(
            "https://api.github.com/repos/o/r/discussions/9",
            "https://api.github.com/repos/o/r/commits/deadbeefcafe",
            "https://api.github.com/repos/o/r/check-suites/777",
            "https://api.github.com/repos/o/r/vulnerability-alerts/1",
        ).forEach { url ->
            assertTrue(
                "expected repository destination for $url",
                resolve("Commit", url) is NotificationRouting.Destination.Repository,
            )
        }
    }

    @Test
    fun `unknown type is conservative and never fabricates a thread`() {
        val destination = resolve("SomethingNew", "https://api.github.com/repos/o/r/mystery/5")
        assertTrue(destination is NotificationRouting.Destination.Repository)
    }

    @Test
    fun `issue without a numeric id falls back to the repository`() {
        val destination = resolve("Issue", "https://api.github.com/repos/o/r/issues")
        assertTrue(destination is NotificationRouting.Destination.Repository)
    }

    @Test
    fun `query string and trailing slash do not break the number`() {
        val thread = resolve(
            "Issue",
            "https://api.github.com/repos/o/r/issues/7?notification=abc#frag",
        ) as NotificationRouting.Destination.Thread
        assertEquals(7, thread.target.number)
    }

    @Test
    fun `html repository url is accepted when the api url is missing type info`() {
        val destination = resolve(null, "https://github.com/octocat/Hello-World/issues/3")
        val thread = destination as NotificationRouting.Destination.Thread
        assertEquals(3, thread.target.number)
    }

    @Test
    fun `reserved github paths are not repositories`() {
        assertNull(
            NotificationRouting.targetFor("Issue", "https://github.com/settings/tokens/1"),
        )
    }

    @Test
    fun `empty and foreign urls resolve to none`() {
        assertEquals(NotificationRouting.Destination.None, resolve("Issue", null))
        assertEquals(NotificationRouting.Destination.None, resolve("Issue", ""))
        assertEquals(
            NotificationRouting.Destination.None,
            resolve("Issue", "https://gitlab.com/o/r/issues/1"),
        )
    }
}
