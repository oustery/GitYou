package com.gityou.app.data.remote

import com.gityou.app.data.remote.dto.Branch
import com.gityou.app.data.remote.dto.Comment
import com.gityou.app.data.remote.dto.CommitDetail
import com.gityou.app.data.remote.dto.CommitFile
import com.gityou.app.data.remote.dto.CommitListItem
import com.gityou.app.data.remote.dto.ContentItem
import com.gityou.app.data.remote.dto.CreateCommentRequest
import com.gityou.app.data.remote.dto.CreateGistRequest
import com.gityou.app.data.remote.dto.CreateIssueRequest
import com.gityou.app.data.remote.dto.CreateRepoRequest
import com.gityou.app.data.remote.dto.Gist
import com.gityou.app.data.remote.dto.Issue
import com.gityou.app.data.remote.dto.IssueLabelsRequest
import com.gityou.app.data.remote.dto.IssueStateRequest
import com.gityou.app.data.remote.dto.Label
import com.gityou.app.data.remote.dto.Notification
import com.gityou.app.data.remote.dto.PrReview
import com.gityou.app.data.remote.dto.PullRequest
import com.gityou.app.data.remote.dto.Reaction
import com.gityou.app.data.remote.dto.ReactionRequest
import com.gityou.app.data.remote.dto.Repo
import com.gityou.app.data.remote.dto.ReviewRequest
import com.gityou.app.data.remote.dto.SearchReposResponse
import com.gityou.app.data.remote.dto.UpdateCommentRequest
import com.gityou.app.data.remote.dto.User
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.Response

/** GitHub REST API v3, authenticated through [AuthInterceptor]. */
interface GitHubApi {

    // ---- User ---------------------------------------------------------------

    @GET("user")
    suspend fun authenticatedUser(): User

    @GET("user/repos")
    suspend fun myRepos(
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1,
    ): Response<List<Repo>>

    @GET("user/starred")
    suspend fun myStars(
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1,
    ): Response<List<Repo>>

    @GET("search/repositories")
    suspend fun searchRepos(
        @Query("q") query: String,
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1,
        @Query("sort") sort: String? = null,
        @Query("order") order: String = "desc",
    ): SearchReposResponse

    // ---- Repository ---------------------------------------------------------

    @GET("repos/{owner}/{repo}")
    suspend fun repo(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): Repo

    /** Branches available for the code browser. */
    @GET("repos/{owner}/{repo}/branches")
    suspend fun branches(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 100,
    ): List<Branch>

    /** Raw markdown of the README. */
    @GET("repos/{owner}/{repo}/readme")
    @Headers("Accept: application/vnd.github.raw+json")
    suspend fun readme(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): String

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun contents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Query("ref") ref: String?,
    ): List<ContentItem>

    /** A single file entry (dir listing returns a list instead). */
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun contentFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Query("ref") ref: String?,
    ): ContentItem

    @POST("repos/{owner}/{repo}/forks")
    suspend fun fork(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): Repo

    // ---- Stars --------------------------------------------------------------

    @GET("user/starred/{owner}/{repo}")
    suspend fun isStarred(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): Response<Unit>

    @PUT("user/starred/{owner}/{repo}")
    suspend fun star(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): Response<Unit>

    @DELETE("user/starred/{owner}/{repo}")
    suspend fun unstar(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): Response<Unit>

    // ---- Issues -------------------------------------------------------------

    @GET("repos/{owner}/{repo}/issues")
    suspend fun issues(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1,
    ): Response<List<Issue>>

    @GET("repos/{owner}/{repo}/issues/{number}")
    suspend fun issue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
    ): Issue

    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateIssueRequest,
    ): Issue

    @GET("repos/{owner}/{repo}/issues/{number}/comments")
    suspend fun issueComments(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Query("per_page") perPage: Int = 50,
        @Query("page") page: Int = 1,
    ): Response<List<Comment>>

    @POST("repos/{owner}/{repo}/issues/{number}/comments")
    suspend fun addComment(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Body body: CreateCommentRequest,
    ): Comment

    // ---- Pull requests ------------------------------------------------------

    @GET("repos/{owner}/{repo}/pulls")
    suspend fun pulls(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1,
    ): Response<List<PullRequest>>

    @GET("repos/{owner}/{repo}/pulls/{number}")
    suspend fun pullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
    ): PullRequest

    // ---- Commits and diffs --------------------------------------------------

    @GET("repos/{owner}/{repo}/commits")
    suspend fun commits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("sha") ref: String? = null,
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1,
    ): Response<List<CommitListItem>>

    @GET("repos/{owner}/{repo}/commits/{sha}")
    suspend fun commit(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("sha") sha: String,
    ): CommitDetail

    /** Changed files of a pull request, each with its unified diff patch. */
    @GET("repos/{owner}/{repo}/pulls/{number}/files")
    suspend fun pullFiles(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Query("per_page") perPage: Int = 100,
    ): List<CommitFile>

    // ---- Issue / PR management ----------------------------------------------

    @PATCH("repos/{owner}/{repo}/issues/{number}")
    suspend fun updateIssueState(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Body body: IssueStateRequest,
    ): Issue

    @PATCH("repos/{owner}/{repo}/issues/{number}")
    suspend fun updateIssueLabels(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Body body: IssueLabelsRequest,
    ): Issue

    @PATCH("repos/{owner}/{repo}/issues/comments/{id}")
    suspend fun updateComment(
        @Path("id") id: Long,
        @Body body: UpdateCommentRequest,
    ): Comment

    @DELETE("repos/{owner}/{repo}/issues/comments/{id}")
    suspend fun deleteComment(@Path("id") id: Long): Response<Unit>

    /** All labels defined in the repository (for the label picker). */
    @GET("repos/{owner}/{repo}/labels")
    suspend fun repoLabels(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 100,
    ): List<Label>

    @POST("repos/{owner}/{repo}/issues/{number}/labels")
    suspend fun addIssueLabels(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Body body: IssueLabelsRequest,
    ): List<Label>

    @DELETE("repos/{owner}/{repo}/issues/{number}/labels")
    suspend fun removeIssueLabels(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Body body: IssueLabelsRequest,
    ): List<Label>

    // ---- Pull request reviews and reactions ----------------------------------

    @GET("repos/{owner}/{repo}/pulls/{number}/reviews")
    suspend fun pullReviews(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
    ): List<PrReview>

    @POST("repos/{owner}/{repo}/pulls/{number}/reviews")
    suspend fun submitReview(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Body body: ReviewRequest,
    ): PrReview

    @POST("repos/{owner}/{repo}/issues/comments/{id}/reactions")
    suspend fun reactToComment(
        @Path("id") id: Long,
        @Body body: ReactionRequest,
    ): Reaction

    // ---- Creation ------------------------------------------------------------

    @POST("user/repos")
    suspend fun createRepo(@Body body: CreateRepoRequest): Repo

    @POST("gists")
    suspend fun createGist(@Body body: CreateGistRequest): Gist

    // ---- Notifications ------------------------------------------------------

    @GET("notifications")
    suspend fun notifications(
        @Query("all") all: Boolean = false,
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1,
    ): Response<List<Notification>>

    @PATCH("notifications/threads/{id}")
    suspend fun markThreadRead(
        @Path("id") id: String,
    ): Response<Unit>

    @PUT("notifications")
    suspend fun markAllNotificationsRead(): Response<Unit>
}
