package com.gityou.app.data.repo

import com.gityou.app.data.remote.GitHubApi
import com.gityou.app.data.remote.dto.Comment
import com.gityou.app.data.remote.dto.CreateCommentRequest
import com.gityou.app.data.remote.dto.CreateIssueRequest
import com.gityou.app.data.remote.dto.Issue
import com.gityou.app.data.remote.dto.IssueLabelsRequest
import com.gityou.app.data.remote.dto.IssueStateRequest
import com.gityou.app.data.remote.dto.Label
import com.gityou.app.data.remote.dto.MergePullRequestRequest
import com.gityou.app.data.remote.dto.PrReview
import com.gityou.app.data.remote.dto.PullRequest
import com.gityou.app.data.remote.dto.Reaction
import com.gityou.app.data.remote.dto.ReactionRequest
import com.gityou.app.data.remote.dto.ReviewRequest
import com.gityou.app.data.remote.dto.UpdateCommentRequest
import com.gityou.app.data.util.LinkHeaderParser
import com.gityou.app.data.util.safeCall
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/** Issue and pull request details, comments and creation. */
@Singleton
class IssuesRepository @Inject constructor(
    private val api: GitHubApi,
) {

    suspend fun issue(owner: String, name: String, number: Int): Issue =
        api.issue(owner, name, number)

    suspend fun pullRequest(owner: String, name: String, number: Int): PullRequest =
        api.pullRequest(owner, name, number)

    /**
     * Fetches ALL comment pages (Link-header driven, F-020), oldest first.
     * The hard page cap only guards against a pathological/malformed Link
     * header, never against ordinary long discussions.
     */
    suspend fun comments(owner: String, name: String, number: Int): List<Comment> {
        val all = mutableListOf<Comment>()
        var page = 1
        while (page <= MAX_COMMENT_PAGES) {
            val response = api.issueComments(owner, name, number, page = page)
            if (!response.isSuccessful) throw HttpException(response)
            all += response.body().orEmpty()
            val next = LinkHeaderParser.nextPageNumber(response.headers()["Link"])
            if (next == null || next <= page) break
            page = next
        }
        return all
    }

    suspend fun createIssue(owner: String, name: String, title: String, body: String?): Issue =
        api.createIssue(owner, name, CreateIssueRequest(title = title, body = body))

    suspend fun addComment(owner: String, name: String, number: Int, body: String): Comment =
        api.addComment(owner, name, number, CreateCommentRequest(body))

    // ---- Management: state, labels, comment editing -------------------------

    suspend fun setIssueState(owner: String, name: String, number: Int, state: String): Issue =
        api.updateIssueState(owner, name, number, IssueStateRequest(state))

    suspend fun replaceIssueLabels(
        owner: String,
        name: String,
        number: Int,
        labels: List<String>,
    ): List<Label> =
        api.updateIssueLabels(owner, name, number, IssueLabelsRequest(labels)).labels

    suspend fun editComment(id: Long, body: String): Comment =
        api.updateComment(id, UpdateCommentRequest(body))

    suspend fun deleteComment(id: Long) {
        val response = api.deleteComment(id)
        if (!response.isSuccessful) throw HttpException(response)
    }

    suspend fun repoLabels(owner: String, name: String): List<Label> =
        api.repoLabels(owner, name)

    suspend fun addLabels(owner: String, name: String, number: Int, labels: List<String>): List<Label> =
        api.addIssueLabels(owner, name, number, IssueLabelsRequest(labels))

    suspend fun removeLabels(owner: String, name: String, number: Int, labels: List<String>): List<Label> =
        api.removeIssueLabels(owner, name, number, IssueLabelsRequest(labels))

    // ---- Reviews and reactions ----------------------------------------------

    suspend fun reviews(owner: String, name: String, number: Int): List<PrReview> =
        api.pullReviews(owner, name, number)

    suspend fun submitReview(owner: String, name: String, number: Int, event: String, body: String?): PrReview =
        api.submitReview(owner, name, number, ReviewRequest(event = event, body = body))

    /**
     * Merges a pull request. [headSha] pins the commit the user reviewed;
     * GitHub answers 409 when the branch moved since (G-014).
     */
    suspend fun merge(
        owner: String,
        name: String,
        number: Int,
        method: String,
        headSha: String?,
    ): Result<Unit> = safeCall {
        val response = api.mergePullRequest(
            owner,
            name,
            number,
            MergePullRequestRequest(
                sha = headSha,
                mergeMethod = method,
            ),
        )
        when {
            response.isSuccessful -> Unit
            response.code() == 409 -> throw MergeConflictException()
            // 405 "merge not allowed" and 422 "not mergeable" (protection,
            // required checks, draft) both mean: the user cannot merge now.
            response.code() == 405 || response.code() == 422 -> throw MergeNotAllowedException()
            else -> throw HttpException(response)
        }
    }

    /** The head branch moved (or a merge is already in flight). */
    class MergeConflictException : Exception("merge conflict")

    /** Branch protection / required checks / permissions refuse the merge. */
    class MergeNotAllowedException : Exception("merge not allowed")

    suspend fun react(commentId: Long, content: String): Reaction =
        api.reactToComment(commentId, ReactionRequest(content))

    private companion object {
        const val MAX_COMMENT_PAGES = 100
    }
}
