package com.gityou.app.data.repo

import com.gityou.app.data.remote.GitHubApi
import com.gityou.app.data.remote.dto.ContentItem
import com.gityou.app.data.remote.dto.Issue
import com.gityou.app.data.remote.dto.Page
import com.gityou.app.data.remote.dto.PullRequest
import com.gityou.app.data.remote.dto.Repo
import com.gityou.app.data.util.LinkHeaderParser
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/** Repositories, search, README, code tree and star/fork actions. */
@Singleton
class ReposRepository @Inject constructor(
    private val api: GitHubApi,
) {

    suspend fun myRepos(page: Int): Page<Repo> = LinkHeaderParser.pageFromResponse(api.myRepos(page = page))

    suspend fun myStars(page: Int): Page<Repo> = LinkHeaderParser.pageFromResponse(api.myStars(page = page))

    suspend fun search(query: String, page: Int): Page<Repo> {
        val result = api.searchRepos(query = query, page = page)
        val nextPage = if (result.items.isNotEmpty()) page + 1 else null
        return Page(items = result.items, nextPage = nextPage)
    }

    suspend fun repo(owner: String, name: String): Repo = api.repo(owner, name)

    suspend fun readme(owner: String, name: String): String = api.readme(owner, name)

    suspend fun contents(owner: String, name: String, path: String, ref: String?): List<ContentItem> =
        api.contents(owner, name, path, ref)

    /** Fetches one file with its base64 content decoded to text. */
    suspend fun fileContent(owner: String, name: String, path: String, ref: String?): String {
        val item = api.contentFile(owner, name, path, ref)
        val encoded = item.content ?: return ""
        val decoded = java.util.Base64.getMimeDecoder().decode(encoded.replace("\n", "").replace("\r", ""))
        return String(decoded, Charsets.UTF_8)
    }

    suspend fun issues(owner: String, name: String, state: String, page: Int): Page<Issue> {
        val pageData = LinkHeaderParser.pageFromResponse(api.issues(owner, name, state, page = page))
        // The issues endpoint also returns pull requests; keep the list clean.
        return pageData.copy(items = pageData.items.filterNot { it.isPullRequest })
    }

    suspend fun pulls(owner: String, name: String, state: String, page: Int): Page<PullRequest> =
        LinkHeaderParser.pageFromResponse(api.pulls(owner, name, state, page = page))

    suspend fun isStarred(owner: String, name: String): Boolean =
        api.isStarred(owner, name).code() == 204

    suspend fun star(owner: String, name: String) {
        val response = api.star(owner, name)
        if (!response.isSuccessful) throw HttpException(response)
    }

    suspend fun unstar(owner: String, name: String) {
        val response = api.unstar(owner, name)
        if (!response.isSuccessful) throw HttpException(response)
    }

    suspend fun fork(owner: String, name: String): Repo = api.fork(owner, name)
}
