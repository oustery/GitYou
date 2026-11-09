package com.gityou.app.data.remote.dto

import com.gityou.app.data.util.GitHubUrls
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Issue(
    @SerialName("id") val id: Long = 0,
    @SerialName("number") val number: Int,
    @SerialName("title") val title: String,
    @SerialName("state") val state: String, // "open" | "closed"
    @SerialName("body") val body: String? = null,
    @SerialName("user") val user: User? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("comments") val commentCount: Int = 0,
    @SerialName("labels") val labels: List<Label> = emptyList(),
    @SerialName("pull_request") val pullRequest: PullRequestRef? = null,
    /**
     * Present in `search/issues` results only.
     * "https://api.github.com/repos/octocat/Hello-World".
     */
    @SerialName("repository_url") val repositoryUrl: String? = null,
) {
    val isPullRequest: Boolean get() = pullRequest != null

    /** "owner/name" for search hits, so a row can open its repository (G-010). */
    val repositoryFullName: String?
        get() = GitHubUrls.parseApiRepoUrl(repositoryUrl)?.let { "${it.owner}/${it.repo}" }
}

@Serializable
data class PullRequestRef(
    @SerialName("url") val url: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
)

@Serializable
data class Label(
    @SerialName("name") val name: String,
    @SerialName("color") val color: String? = null,
)

@Serializable
data class Comment(
    @SerialName("id") val id: Long = 0,
    @SerialName("user") val user: User? = null,
    @SerialName("body") val body: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("reactions") val reactions: ReactionsSummary? = null,
)

/** Aggregated reaction counters that GitHub embeds into issue bodies and comments. */
@Serializable
data class ReactionsSummary(
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("+1") val plusOne: Int = 0,
    @SerialName("-1") val minusOne: Int = 0,
    @SerialName("laugh") val laugh: Int = 0,
    @SerialName("hooray") val hooray: Int = 0,
    @SerialName("confused") val confused: Int = 0,
    @SerialName("heart") val heart: Int = 0,
    @SerialName("rocket") val rocket: Int = 0,
    @SerialName("eyes") val eyes: Int = 0,
)

@Serializable
data class CreateIssueRequest(
    @SerialName("title") val title: String,
    @SerialName("body") val body: String? = null,
)

@Serializable
data class CreateCommentRequest(
    @SerialName("body") val body: String,
)

/** Body for `PATCH /issues/{number}` — changing only the state. */
@Serializable
data class IssueStateRequest(
    @SerialName("state") val state: String, // "open" | "closed"
)

/** Body for `PATCH /issues/{number}` — replacing the label set. */
@Serializable
data class IssueLabelsRequest(
    @SerialName("labels") val labels: List<String>,
)

/**
 * Body for `PUT /pulls/{number}/merge`. [sha] pins the head commit so a merge
 * cannot silently include commits the user never saw (G-014).
 */
@Serializable
data class MergePullRequestRequest(
    @SerialName("commit_title") val commitTitle: String? = null,
    @SerialName("commit_message") val commitMessage: String? = null,
    @SerialName("sha") val sha: String? = null,
    @SerialName("merge_method") val mergeMethod: String = "merge",
)

/** Body for `PATCH /issues/comments/{id}`. */
@Serializable
data class UpdateCommentRequest(
    @SerialName("body") val body: String,
)

@Serializable
data class BranchRef(
    @SerialName("ref") val ref: String? = null,
    @SerialName("label") val label: String? = null,
    @SerialName("sha") val sha: String? = null,
)

@Serializable
data class PullRequest(
    @SerialName("id") val id: Long = 0,
    @SerialName("number") val number: Int,
    @SerialName("title") val title: String,
    @SerialName("state") val state: String, // "open" | "closed"
    @SerialName("body") val body: String? = null,
    @SerialName("user") val user: User? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("comments") val commentCount: Int = 0,
    @SerialName("labels") val labels: List<Label> = emptyList(),
    @SerialName("draft") val draft: Boolean = false,
    @SerialName("merged") val merged: Boolean = false,
    @SerialName("additions") val additions: Int = 0,
    @SerialName("deletions") val deletions: Int = 0,
    @SerialName("changed_files") val changedFiles: Int = 0,
    @SerialName("head") val head: BranchRef? = null,
    @SerialName("base") val base: BranchRef? = null,
    /**
     * `true` / `false` once GitHub finished computing, `null` while unknown
     * ("unknown" is returned as null by the API and means "ask again").
     */
    @SerialName("mergeable") val mergeable: Boolean? = null,
    @SerialName("mergeable_state") val mergeableState: String? = null,
    @SerialName("review_comments") val reviewComments: Int = 0,
    @SerialName("commits") val commitCount: Int = 0,
)
