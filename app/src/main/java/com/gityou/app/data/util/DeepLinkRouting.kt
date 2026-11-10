package com.gityou.app.data.util

/**
 * Turns an inbound `ACTION_VIEW` (App Link) or `ACTION_SEND` payload into a
 * navigation route (G-012).
 *
 * The activity stays dumb: it hands the intent data here and navigates to
 * whatever [route] returns. Links the app cannot render produce null, so the
 * activity leaves the intent to the browser instead of opening the wrong
 * screen — the same rule markdown links follow (G-007).
 *
 * Pure Kotlin: the whole mapping is unit-tested without Android.
 */
object DeepLinkRouting {

    /**
     * Route for a `github.com` URL, or null when the app has no matching
     * screen. [Route] is a string here because building it belongs to the
     * navigation package; callers pass their builders in.
     */
    fun route(
        url: String?,
        issueRoute: (owner: String, repo: String, number: Int, isPull: Boolean) -> String,
        repoRoute: (owner: String, repo: String) -> String,
        userRoute: (login: String) -> String,
    ): String? = when (val target = GitHubUrls.routeLink(url)) {
        is GitHubUrls.LinkTarget.Thread ->
            issueRoute(
                target.ref.owner,
                target.ref.repo,
                target.ref.number,
                target.ref.isPull,
            )
        is GitHubUrls.LinkTarget.Repo -> repoRoute(target.owner, target.repo)
        is GitHubUrls.LinkTarget.User -> userRoute(target.login)
        null -> null
    }

    /**
     * First `github.com` URL inside shared text, if any. Share sheets deliver
     * the poster app's blurb plus the link, so the whole text must be scanned
     * rather than parsed as a URL.
     */
    fun firstGitHubUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        return URL_IN_TEXT.find(text)?.value
    }

    private val URL_IN_TEXT = Regex("""https?://(?:www\.)?github\.com/[^\s<>"']+""")
}
