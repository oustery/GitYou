package com.gityou.app.data.util

/**
 * Parsers for the public github.com URLs embedded in API payloads
 * (issue `html_url`, release asset `browser_download_url`).
 */
object GitHubUrls {

    /** Coordinates of an issue or pull request inside its repository. */
    data class IssueRef(
        val owner: String,
        val repo: String,
        val number: Int,
        val isPull: Boolean,
    )

    private val issueRegex = Regex(
        "^https://github\\.com/([^/]+)/([^/]+)/(issues|pull)/(\\d+)",
    )

    /**
     * "https://github.com/octocat/repo/pull/42" ->
     * IssueRef("octocat", "repo", 42, isPull = true). Null if the URL is not
     * a recognizable issue/PR page.
     */
    fun parseIssueUrl(url: String?): IssueRef? {
        if (url.isNullOrBlank()) return null
        val match = issueRegex.find(url.trim()) ?: return null
        val (_, owner, repo, kind, number) = match.groupValues
        val n = number.toIntOrNull() ?: return null
        return IssueRef(owner = owner, repo = repo, number = n, isPull = kind == "pull")
    }
}
