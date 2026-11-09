package com.gityou.app.data.util

/**
 * Maps a notification `subject` onto an in-app destination.
 *
 * GitHub's notification `subject.url` always points at the API
 * (`api.github.com/repos/{owner}/{repo}/{kind}/{id}`), and the trailing id is
 * *not* an issue number for every type: releases, discussions, commits and
 * check suites carry their own ids. Reading that id unconditionally sent the
 * user to a non-existent issue, i.e. straight into a 404 (G-021).
 *
 * Pure Kotlin so the whole matrix is unit-tested without Android.
 */
object NotificationRouting {

    /** Fully resolved destination: a concrete issue/PR thread. */
    data class Target(
        val owner: String,
        val repo: String,
        val number: Int,
        val isPull: Boolean,
    )

    /** What the UI should do with a notification. */
    sealed interface Destination {
        /** A thread the app can render: issue detail or pull-request detail. */
        data class Thread(val target: Target) : Destination

        /** A repository notification with no in-app thread (release, commit…). */
        data class Repository(val owner: String, val repo: String) : Destination

        /** Nothing useful could be parsed; the row should not navigate. */
        data object None : Destination
    }

    /** Subject types that are rendered by the issue/PR detail screen. */
    private val THREAD_TYPES = setOf("Issue", "PullRequest")

    /** Types that legitimately belong to the repository, not to a thread. */
    private val REPOSITORY_TYPES = setOf(
        "Release", "Discussion", "Commit", "CheckSuite", "RepositoryVulnerabilityAlert",
        "RepositoryInvitation", "SecurityAlert",
    )

    fun resolve(subjectType: String?, subjectUrl: String?): Destination {
        if (subjectUrl.isNullOrBlank()) return Destination.None
        val repo = GitHubUrls.parseApiRepoUrl(subjectUrl)
            ?: GitHubUrls.parseRepoUrl(subjectUrl)
            ?: return Destination.None

        val number = trailingNumber(subjectUrl)
        val isPull = subjectType.equals("PullRequest", ignoreCase = true)

        if (subjectType != null && subjectType in REPOSITORY_TYPES) {
            return Destination.Repository(repo.owner, repo.repo)
        }
        // Only issue/PR threads have a number that the app can open. Unknown
        // types stay conservative: they reach the repository, never a 404.
        if (subjectType != null && subjectType !in THREAD_TYPES) {
            return Destination.Repository(repo.owner, repo.repo)
        }
        if (number == null) return Destination.Repository(repo.owner, repo.repo)
        return Destination.Thread(
            Target(owner = repo.owner, repo = repo.repo, number = number, isPull = isPull),
        )
    }

    /** Backwards-compatible helper for callers that only need a thread. */
    fun targetFor(subjectType: String?, subjectUrl: String?): Target? =
        (resolve(subjectType, subjectUrl) as? Destination.Thread)?.target

    /** The numeric id at the end of the URL path (query/fragment stripped). */
    private fun trailingNumber(url: String): Int? {
        val path = url.substringBefore('?').substringBefore('#').trimEnd('/')
        return path.substringAfterLast('/').toIntOrNull()
    }
}
