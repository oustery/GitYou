package com.gityou.app.ui.navigation

/** All navigation routes with typed builders. */
object Route {
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
        if (ref.isNullOrBlank()) "commits/$owner/$name" else "commits/$owner/$name?ref=$ref"

    fun commitDiff(owner: String, name: String, sha: String) = "commit/$owner/$name/$sha"
    fun pullDiff(owner: String, name: String, number: Int) =
        "pullFiles/$owner/$name/$number?isPull=true"

    fun user(login: String) = "user/$login"
    fun releases(owner: String, name: String) = "releases/$owner/$name"

    /** [kind] is "stargazers" or "forks" (see RepoPeopleKind). */
    fun repoPeople(owner: String, name: String, kind: String) =
        "repoPeople/$owner/$name?kind=$kind"
}
