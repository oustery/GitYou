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
     * Any in-app destination a github.com link can point at (G-007): a
     * repository, an issue/PR thread, or a user profile. Anything else
     * (`/blob/...`, `/releases`, gists, external hosts) stays in the browser.
     */
    sealed interface LinkTarget {
        data class Repo(val owner: String, val repo: String) : LinkTarget
        data class Thread(val ref: IssueRef) : LinkTarget
        data class User(val login: String) : LinkTarget
    }

    /** Path segments under github.com that are not a user profile. */
    private val RESERVED_LOGINS = RESERVED_OWNER_PATHS + setOf("orgs", "users", "gists", "login")

    private val pathRegex = Regex("^https://github\\.com/([^/?#]+)(?:/([^/?#]+))?")

    /**
     * Maps a link found in rendered markdown to an in-app screen (G-007).
     * Links to issues and PRs are how discussion threads reference each other,
     * so sending the user to the browser breaks the reading flow. Everything
     * the app cannot render (`/blob/...`, `/releases`, `/settings`, gists and
     * any non-GitHub host) returns null and stays external.
     */
    fun routeLink(url: String?): LinkTarget? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim().substringBefore('#').substringBefore('?')
        parseIssueUrl(trimmed)?.let { return LinkTarget.Thread(it) }
        val match = pathRegex.find(trimmed) ?: return null
        val owner = match.groupValues[1]
        val second = match.groupValues[2]
        if (owner.lowercase() in RESERVED_LOGINS) return null
        if (second.isEmpty()) return LinkTarget.User(owner)
        // Deeper paths (/blob/, /tree/, /releases/, /actions) need a view the
        // app does not have; opening the repository instead would silently
        // drop the user's destination, so they stay in the browser.
        val rest = trimmed.removePrefix(match.value).trim('/')
        if (rest.isNotEmpty()) return null
        return LinkTarget.Repo(owner, second)
    }

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
