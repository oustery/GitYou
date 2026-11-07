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
}
