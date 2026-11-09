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

    @Test
    fun `api repo url feeds the my-work rows`() {
        // search/issues returns repository_url only; the row needs "owner/name"
        // to open the issue (G-010).
        val ref = GitHubUrls.parseApiRepoUrl("https://api.github.com/repos/oustery/GitYou")
        assertEquals("oustery", ref?.owner)
        assertEquals("GitYou", ref?.repo)
        assertNull(GitHubUrls.parseApiRepoUrl("https://github.com/oustery/GitYou"))
    }

    @Test
    fun `markdown links route to in-app screens`() {
        // G-007: issue/PR links are the main navigation inside GitHub content.
        val issue = GitHubUrls.routeLink("https://github.com/octocat/Hello-World/issues/1347")
        assertEquals(
            GitHubUrls.LinkTarget.Thread(
                GitHubUrls.IssueRef("octocat", "Hello-World", 1347, isPull = false),
            ),
            issue,
        )
        val pull = GitHubUrls.routeLink("https://github.com/octocat/Hello-World/pull/42?diff=split")
        assertEquals(
            GitHubUrls.LinkTarget.Thread(
                GitHubUrls.IssueRef("octocat", "Hello-World", 42, isPull = true),
            ),
            pull,
        )
        assertEquals(
            GitHubUrls.LinkTarget.Repo("octocat", "Hello-World"),
            GitHubUrls.routeLink("https://github.com/octocat/Hello-World"),
        )
        assertEquals(
            GitHubUrls.LinkTarget.Repo("octocat", "Hello-World"),
            GitHubUrls.routeLink("https://github.com/octocat/Hello-World/"),
        )
        assertEquals(
            GitHubUrls.LinkTarget.User("octocat"),
            GitHubUrls.routeLink("https://github.com/octocat"),
        )
    }

    @Test
    fun `non-app markdown links stay external`() {
        // Deep pages the client does not render, and foreign hosts.
        assertNull(GitHubUrls.routeLink("https://github.com/octocat/Hello-World/blob/main/README.md"))
        assertNull(GitHubUrls.routeLink("https://github.com/settings/tokens"))
        assertNull(GitHubUrls.routeLink("https://github.com/notifications"))
        assertNull(GitHubUrls.routeLink("https://gitlab.com/octocat/Hello-World"))
        assertNull(GitHubUrls.routeLink("https://example.com"))
        assertNull(GitHubUrls.routeLink(null))
    }
}
