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

    /** A repository coordinate parsed out of an `html_url`-style URL. */
    data class RepoRef(val owner: String, val repo: String)

    private val issueRegex = Regex(
        "^https://github\\.com/([^/]+)/([^/]+)/(issues|pull)/(\\d+)",
    )

    private val apiRepoRegex = Regex(
        "^https://api\\.github\\.com/repos/([^/]+)/([^/?#]+)",
    )

    private val repoRegex = Regex(
        "^https://github\\.com/([^/?#]+)/([^/?#]+)(?:[/?#]|$)",
    )

    /**
     * Repository coordinates from a notification `subject.url`, which is an
     * `api.github.com/repos/{owner}/{repo}/...` URL
     * ("https://api.github.com/repos/octocat/Hello-World/issues/1347").
     */
    fun parseApiRepoUrl(url: String?): RepoRef? {
        if (url.isNullOrBlank()) return null
        val match = apiRepoRegex.find(url.trim()) ?: return null
        val (_, owner, repo) = match.groupValues
        return RepoRef(owner, repo)
    }

    /**
     * Repository coordinates from a public page URL
     * ("https://github.com/octocat/Hello-World/issues/1347"). Reserved
     * top-level paths (settings, notifications, ...) are not repositories and
     * yield null.
     */
    fun parseRepoUrl(url: String?): RepoRef? {
        if (url.isNullOrBlank()) return null
        val match = repoRegex.find(url.trim()) ?: return null
        val (_, owner, repo) = match.groupValues
        if (owner.lowercase() in RESERVED_OWNER_PATHS) return null
        return RepoRef(owner, repo)
    }

    private val RESERVED_OWNER_PATHS = setOf(
        "settings", "notifications", "explore", "marketplace", "pulls",
        "issues", "topics", "sponsors", "orgs", "apps", "login", "search",
        "about", "pricing", "features", "enterprise", "collections",
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
