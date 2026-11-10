package com.gityou.app.data.repo

import com.gityou.app.data.remote.GitHubApi
import com.gityou.app.data.remote.dto.CommitDetail
import com.gityou.app.data.remote.dto.CommitFile
import com.gityou.app.data.remote.dto.CommitListItem
import com.gityou.app.data.remote.dto.ContentItem
import com.gityou.app.data.remote.dto.Issue
import com.gityou.app.data.remote.dto.Page
import com.gityou.app.data.remote.dto.PullRequest
import com.gityou.app.data.remote.dto.Release
import com.gityou.app.data.remote.dto.Repo
import com.gityou.app.data.remote.dto.Tag
import com.gityou.app.data.remote.dto.User
import com.gityou.app.data.util.LinkHeaderParser
import com.gityou.app.data.util.SearchQueries
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/** Repositories, search, README, code tree and star/fork actions. */
@Singleton
class ReposRepository @Inject constructor(
    private val api: GitHubApi,
) {

    /** The file cannot be shown as text; [Reason] maps to a localized message. */
    class FilePreviewException(val reason: Reason) : Exception(reason.name) {
        enum class Reason { TOO_LARGE, BINARY, UNSUPPORTED }
    }

    private companion object {
        /** GitHub inlines content only for files up to 1 MB. */
        const val MAX_INLINE_BYTES = 1_048_576L

        /** NUL-byte scan window for the binary heuristic. */
        const val BINARY_SCAN_BYTES = 8_192

        /**
         * Guard against a pathological Link header on the PR files endpoint:
         * 100 pages is 10 000 files, far beyond any reviewer-facing diff.
         */
        const val MAX_PULL_FILE_PAGES = 100
    }

    suspend fun myRepos(page: Int): Page<Repo> = LinkHeaderParser.pageFromResponse(api.myRepos(page = page))

    suspend fun myStars(page: Int): Page<Repo> = LinkHeaderParser.pageFromResponse(api.myStars(page = page))

    suspend fun search(query: String, page: Int, sort: String? = null): Page<Repo> {
        val result = api.searchRepos(query = query, page = page, sort = sort)
        val nextPage = if (result.items.isNotEmpty()) page + 1 else null
        return Page(items = result.items, nextPage = nextPage)
    }

    suspend fun repo(owner: String, name: String): Repo = api.repo(owner, name)

    suspend fun branches(owner: String, name: String): List<String> =
        api.branches(owner, name).map { it.name }

    suspend fun readme(owner: String, name: String): String = api.readme(owner, name)

    suspend fun contents(owner: String, name: String, path: String, ref: String?): List<ContentItem> =
        api.contents(owner, name, path, ref)

    /** Fetches one file with its base64 content decoded to text. */
    suspend fun fileContent(owner: String, name: String, path: String, ref: String?): String {
        val item = api.contentFile(owner, name, path, ref)
        val encoded = item.content ?: throw FilePreviewException(
            // GitHub inline-encodes only files up to ~1 MB; anything bigger
            // (and exotic entries like submodules) arrives without content.
            if (item.size > MAX_INLINE_BYTES) {
                FilePreviewException.Reason.TOO_LARGE
            } else {
                FilePreviewException.Reason.UNSUPPORTED
            },
        )
        val bytes = java.util.Base64.getMimeDecoder().decode(encoded.replace("\n", "").replace("\r", ""))
        // Cheap binary heuristic: a NUL byte in the first 8 KB means the
        // UTF-8 render would be mojibake, not a readable preview (F-021).
        val scanEnd = minOf(bytes.size, BINARY_SCAN_BYTES)
        for (i in 0 until scanEnd) {
            if (bytes[i] == 0.toByte()) throw FilePreviewException(FilePreviewException.Reason.BINARY)
        }
        return String(bytes, Charsets.UTF_8)
    }

    /** Server-side filters for the repository issue list (G-009). */
    data class IssueQuery(
        val state: String = "open",
        val labels: List<String> = emptyList(),
        val assignee: String? = null,
        val creator: String? = null,
        val sort: String = "created",
        val direction: String = "desc",
    ) {
        /** What "active filters" means to the user: anything beyond defaults. */
        val hasFilters: Boolean
            get() = labels.isNotEmpty() || assignee != null || creator != null ||
                sort != "created" || direction != "desc"
    }

    suspend fun issues(owner: String, name: String, query: IssueQuery, page: Int): Page<Issue> {
        val pageData = LinkHeaderParser.pageFromResponse(
            api.issues(
                owner = owner,
                repo = name,
                state = query.state,
                labels = query.labels.takeIf { it.isNotEmpty() }?.joinToString(","),
                assignee = query.assignee,
                creator = query.creator,
                sort = query.sort,
                direction = query.direction,
                page = page,
            ),
        )
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

    suspend fun commits(owner: String, name: String, ref: String?, page: Int): Page<CommitListItem> =
        LinkHeaderParser.pageFromResponse(api.commits(owner, name, ref = ref, page = page))

    suspend fun commit(owner: String, name: String, sha: String): CommitDetail =
        api.commit(owner, name, sha)

    /**
     * All changed files of a pull request, Link-header driven (G-003). GitHub
     * serves at most 100 files per page; truncating silently made the diff
     * viewer show an incomplete change set.
     */
    suspend fun pullFiles(owner: String, name: String, number: Int): List<CommitFile> {
        val all = mutableListOf<CommitFile>()
        var page = 1
        while (page <= MAX_PULL_FILE_PAGES) {
            val response = api.pullFiles(owner, name, number, page = page)
            if (!response.isSuccessful) throw HttpException(response)
            all += response.body().orEmpty()
            val next = LinkHeaderParser.nextPageNumber(response.headers()["Link"])
            if (next == null || next <= page) break
            page = next
        }
        return all
    }

    // ---- People around a repository ------------------------------------------

    suspend fun stargazers(owner: String, name: String, page: Int): Page<User> =
        LinkHeaderParser.pageFromResponse(api.stargazers(owner, name, page = page))

    suspend fun forks(owner: String, name: String, page: Int): Page<Repo> =
        LinkHeaderParser.pageFromResponse(api.repoForks(owner, name, page = page))

    // ---- Releases and tags ----------------------------------------------------

    suspend fun releases(owner: String, name: String, page: Int): Page<Release> =
        LinkHeaderParser.pageFromResponse(api.releases(owner, name, page = page))

    suspend fun tags(owner: String, name: String, page: Int): Page<Tag> =
        LinkHeaderParser.pageFromResponse(api.tags(owner, name, page = page))

    // ---- Search ---------------------------------------------------------------

    suspend fun searchUsers(query: String, page: Int): Page<User> {
        val result = api.searchUsers(query = query, page = page)
        val nextPage = if (result.items.isNotEmpty()) page + 1 else null
        return Page(items = result.items, nextPage = nextPage)
    }

    suspend fun searchIssues(query: String, page: Int): Page<Issue> {
        val result = api.searchIssues(query = query, page = page)
        val nextPage = if (result.items.isNotEmpty()) page + 1 else null
        return Page(items = result.items, nextPage = nextPage)
    }

    /**
     * One tab of the "My work" hub (G-010): issues assigned to me, created by
     * me, or pull requests waiting for my review.
     */
    suspend fun myWork(tab: SearchQueries.MyWorkTab, page: Int): Page<Issue> {
        val result = api.searchIssues(
            query = SearchQueries.myWorkQuery(tab),
            page = page,
            sort = SearchQueries.MY_WORK_SORT,
        )
        val nextPage = if (result.items.isNotEmpty()) page + 1 else null
        return Page(items = result.items, nextPage = nextPage)
    }

    /** "Trending this week" approximation; see [SearchQueries.trending]. */
    suspend fun trending(page: Int): Page<Repo> {
        val result = api.searchRepos(
            query = SearchQueries.trending(),
            page = page,
            sort = SearchQueries.TRENDING_SORT,
        )
        val nextPage = if (result.items.isNotEmpty()) page + 1 else null
        return Page(items = result.items, nextPage = nextPage)
    }
}
