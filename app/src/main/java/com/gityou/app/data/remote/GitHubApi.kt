package com.gityou.app.data.remote

import com.gityou.app.data.remote.dto.Comment
import com.gityou.app.data.remote.dto.ContentItem
import com.gityou.app.data.remote.dto.CreateCommentRequest
import com.gityou.app.data.remote.dto.CreateIssueRequest
import com.gityou.app.data.remote.dto.Issue
import com.gityou.app.data.remote.dto.Notification
import com.gityou.app.data.remote.dto.PullRequest
import com.gityou.app.data.remote.dto.Repo
import com.gityou.app.data.remote.dto.SearchReposResponse
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
    ): SearchReposResponse

    // ---- Repository ---------------------------------------------------------

    @GET("repos/{owner}/{repo}")
    suspend fun repo(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): Repo

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
    ): List<Comment>

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
