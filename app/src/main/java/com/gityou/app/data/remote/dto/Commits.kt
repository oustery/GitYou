package com.gityou.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Entry of the repository commit list (`GET /repos/{o}/{r}/commits`). */
@Serializable
data class CommitListItem(
    @SerialName("sha") val sha: String,
    @SerialName("commit") val commit: CommitInfo? = null,
    @SerialName("author") val author: User? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
) {
    val message: String get() = commit?.message.orEmpty()
    val firstLine: String get() = message.substringBefore('\n')
}

/** Author/committer block of a commit (name + date, no avatar). */
@Serializable
data class CommitUser(
    @SerialName("name") val name: String = "",
    @SerialName("email") val email: String? = null,
    @SerialName("date") val date: String? = null,
)

@Serializable
data class CommitInfo(
    @SerialName("message") val message: String = "",
    @SerialName("author") val author: CommitUser? = null,
    @SerialName("committer") val committer: CommitUser? = null,
)

/** One changed file of a commit or pull request, with its unified diff patch. */
@Serializable
data class CommitFile(
    @SerialName("filename") val filename: String,
    @SerialName("status") val status: String? = null, // added | removed | modified | renamed
    @SerialName("additions") val additions: Int = 0,
    @SerialName("deletions") val deletions: Int = 0,
    @SerialName("changes") val changes: Int = 0,
    @SerialName("patch") val patch: String? = null,
    @SerialName("previous_filename") val previousFilename: String? = null,
)

@Serializable
data class DiffStats(
    @SerialName("additions") val additions: Int = 0,
    @SerialName("deletions") val deletions: Int = 0,
    @SerialName("total") val total: Int = 0,
)

/** Full commit with the list of changed files (`GET /commits/{sha}`). */
@Serializable
data class CommitDetail(
    @SerialName("sha") val sha: String,
    @SerialName("commit") val commit: CommitInfo? = null,
    @SerialName("author") val author: User? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("stats") val stats: DiffStats? = null,
    @SerialName("files") val files: List<CommitFile> = emptyList(),
) {
    val message: String get() = commit?.message.orEmpty()
    val firstLine: String get() = message.substringBefore('\n')
}

/** One submitted pull-request review. */
@Serializable
data class PrReview(
    @SerialName("id") val id: Long = 0,
    @SerialName("user") val user: User? = null,
    @SerialName("state") val state: String? = null, // APPROVED | CHANGES_REQUESTED | COMMENTED
    @SerialName("body") val body: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
)

/** Body for submitting a review: event APPROVE / REQUEST_CHANGES / COMMENT. */
@Serializable
data class ReviewRequest(
    @SerialName("event") val event: String,
    @SerialName("body") val body: String? = null,
)

/** Body for posting a reaction on an issue/PR conversation comment. */
@Serializable
data class ReactionRequest(
    @SerialName("content") val content: String,
)

@Serializable
data class Reaction(
    @SerialName("id") val id: Long = 0,
    @SerialName("user") val user: User? = null,
    @SerialName("content") val content: String = "",
)
