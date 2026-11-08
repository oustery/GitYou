package com.gityou.app

import com.gityou.app.data.util.GitHubUrls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Issue/PR URLs from the search API must map back to app navigation. */
class GitHubUrlsTest {

    @Test
    fun `parses an issue url`() {
        val ref = GitHubUrls.parseIssueUrl("https://github.com/octocat/Hello-World/issues/1347")
        assertEquals("octocat", ref?.owner)
        assertEquals("Hello-World", ref?.repo)
        assertEquals(1347, ref?.number)
        assertEquals(false, ref?.isPull)
    }

    @Test
    fun `parses a pull request url`() {
        val ref = GitHubUrls.parseIssueUrl("https://github.com/oustery/GitYou/pull/42")
        assertEquals("oustery", ref?.owner)
        assertEquals("GitYou", ref?.repo)
        assertEquals(42, ref?.number)
        assertEquals(true, ref?.isPull)
    }

    @Test
    fun `ignores non-issue github pages`() {
        assertNull(GitHubUrls.parseIssueUrl("https://github.com/octocat/Hello-World"))
        assertNull(GitHubUrls.parseIssueUrl("https://github.com/octocat/Hello-World/blob/main/README.md"))
        assertNull(GitHubUrls.parseIssueUrl("https://github.com/octocat/Hello-World/commits/main"))
    }

    @Test
    fun `ignores foreign hosts and garbage`() {
        assertNull(GitHubUrls.parseIssueUrl("https://gitlab.com/octocat/Hello-World/issues/1"))
        assertNull(GitHubUrls.parseIssueUrl("not a url"))
        assertNull(GitHubUrls.parseIssueUrl(""))
        assertNull(GitHubUrls.parseIssueUrl(null))
    }
}
