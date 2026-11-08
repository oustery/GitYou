package com.gityou.app.ui.navigation

/** All navigation routes with typed builders. */
object Route {

    /**
     * Percent-encodes a value destined for a route path/query slot. Git refs
     * (branches, tags) legally contain URI-hostile characters — `#`, `&`,
     * spaces — and an unencoded ref silently navigates to the wrong place:
     * `Uri.parse` cuts a `#` off as a fragment and splits on `&` (N-001).
     * Encodes per UTF-8 byte over the RFC 3986 unreserved set; kept pure-Kotlin
     * (instead of android.net.Uri.encode) so it is directly unit-testable.
     */
    internal fun encodeArg(value: String): String {
        val bytes = value.toByteArray(Charsets.UTF_8)
        return buildString(bytes.size) {
            for (b in bytes) {
                val v = b.toInt() and 0xFF
                if (v.isUnreserved()) {
                    append(v.toChar())
                } else {
                    append('%')
                    append(HEX[(v shr 4) and 0xF])
                    append(HEX[v and 0xF])
                }
            }
        }
    }

    /** RFC 3986 unreserved characters: kept literally, everything else is escaped. */
    private fun Int.isUnreserved(): Boolean = when (this) {
        in 'a'.code..'z'.code, in 'A'.code..'Z'.code, in '0'.code..'9'.code -> true
        '-'.code, '.'.code, '_'.code, '~'.code -> true
        else -> false
    }

    private const val HEX = "0123456789ABCDEF"

    const val HOME = "home"
    const val SEARCH = "search"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"

    const val REPO = "repo/{owner}/{name}"
    const val ISSUE = "issue/{owner}/{name}/{number}?isPull={isPull}"
    const val NEW_ISSUE = "newIssue/{owner}/{name}"
    const val COMMITS = "commits/{owner}/{name}?ref={ref}"
    const val COMMIT_DIFF = "commit/{owner}/{name}/{sha}"
    const val PULL_DIFF = "pullFiles/{owner}/{name}/{number}?isPull={isPull}"
    const val NEW_REPO = "newRepo"
    const val NEW_GIST = "newGist"
    const val USER = "user/{login}"
    const val RELEASES = "releases/{owner}/{name}"
    const val REPO_PEOPLE = "repoPeople/{owner}/{name}?kind={kind}"

    /** Routes that keep the bottom navigation bar visible. */
    val TOP_LEVEL = setOf(HOME, SEARCH, NOTIFICATIONS, PROFILE)

    fun repo(owner: String, name: String) = "repo/$owner/$name"
    fun issue(owner: String, name: String, number: Int, isPull: Boolean) =
        "issue/$owner/$name/$number?isPull=$isPull"

    fun newIssue(owner: String, name: String) = "newIssue/$owner/$name"

    /** Valid start tabs; unknown or missing persisted keys fall back to HOME. */
    fun defaultTabRoute(raw: String?): String = when (raw) {
        SEARCH, NOTIFICATIONS, PROFILE -> raw
        else -> HOME
    }

    fun commits(owner: String, name: String, ref: String? = null) =
        if (ref.isNullOrBlank()) "commits/$owner/$name" else "commits/$owner/$name?ref=${encodeArg(ref)}"

    fun commitDiff(owner: String, name: String, sha: String) = "commit/$owner/$name/$sha"
    fun pullDiff(owner: String, name: String, number: Int) =
        "pullFiles/$owner/$name/$number?isPull=true"

    fun user(login: String) = "user/$login"
    fun releases(owner: String, name: String) = "releases/$owner/$name"

    /** [kind] is "stargazers" or "forks" (see RepoPeopleKind). */
    fun repoPeople(owner: String, name: String, kind: String) =
        "repoPeople/$owner/$name?kind=$kind"
}
