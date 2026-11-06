package com.gityou.app.data.repo

import com.gityou.app.data.remote.GitHubApi
import com.gityou.app.data.remote.dto.Comment
import com.gityou.app.data.remote.dto.CreateCommentRequest
import com.gityou.app.data.remote.dto.CreateIssueRequest
import com.gityou.app.data.remote.dto.Issue
import com.gityou.app.data.remote.dto.PullRequest
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

    suspend fun comments(owner: String, name: String, number: Int): List<Comment> =
        api.issueComments(owner, name, number)

    suspend fun createIssue(owner: String, name: String, title: String, body: String?): Issue =
        api.createIssue(owner, name, CreateIssueRequest(title = title, body = body))

    suspend fun addComment(owner: String, name: String, number: Int, body: String): Comment =
        api.addComment(owner, name, number, CreateCommentRequest(body))
}
