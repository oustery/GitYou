package com.gityou.app

import com.gityou.app.data.remote.GitHubApi
import com.gityou.app.data.util.ApiErrors
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Contract tests against a real HTTP server (G-011). The unit-test layer used
 * to mock repositories, so the actual on-the-wire behaviour — status codes,
 * headers and JSON parsing — was never exercised. These tests pin the parts of
 * that contract the app depends on.
 */
class GitHubApiNetworkTest {

    private lateinit var server: MockWebServer
    private lateinit var api: GitHubApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GitHubApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `issue body is parsed and unknown fields are ignored`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "number": 7,
                  "title": "Fix the badge",
                  "state": "open",
                  "comments": 3,
                  "user": {"login": "octocat", "avatar_url": "https://example.com/a.png"},
                  "labels": [{"name": "bug", "color": "ff0000"}],
                  "something_new_github_added": true
                }
                """.trimIndent(),
            ),
        )

        val issue = api.issue("octocat", "Hello-World", 7)

        assertEquals(7, issue.number)
        assertEquals("Fix the badge", issue.title)
        assertEquals(3, issue.commentCount)
        assertEquals("octocat", issue.user?.login)
        assertEquals(listOf("bug"), issue.labels.map { it.name })
        assertEquals("/repos/octocat/Hello-World/issues/7", server.takeRequest().path)
    }

    @Test
    fun `search results expose the repository the row belongs to`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "total_count": 1,
                  "incomplete_results": false,
                  "items": [{
                    "number": 42,
                    "title": "Needs review",
                    "state": "open",
                    "repository_url": "https://api.github.com/repos/oustery/GitYou",
                    "pull_request": {"html_url": "https://github.com/oustery/GitYou/pull/42"}
                  }]
                }
                """.trimIndent(),
            ),
        )

        val result = api.searchIssues(query = "review-requested:@me is:pr state:open")

        assertEquals(1, result.items.size)
        // "My work" rows open the issue in place, which needs owner/name (G-010).
        assertEquals("oustery/GitYou", result.items.first().repositoryFullName)
        assertTrue(result.items.first().isPullRequest)
    }

    @Test
    fun `rate limit answers map to the rate limit key, not a generic error`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setHeader("X-RateLimit-Remaining", "0")
                .setHeader("X-RateLimit-Reset", "1893456000")
                .setBody("""{"message": "API rate limit exceeded"}"""),
        )

        val error = runCatching { api.issue("o", "r", 1) }.exceptionOrNull()

        assertTrue(error is HttpException)
        assertEquals(ApiErrors.KEY_RATE_LIMIT, ApiErrors.keyOf(error as HttpException))
    }

    @Test
    fun `secondary limit with retry-after is a rate limit`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setHeader("Retry-After", "60")
                .setBody("""{"message": "abuse detection"}"""),
        )

        val error = runCatching { api.issue("o", "r", 1) }.exceptionOrNull() as HttpException

        assertEquals(ApiErrors.KEY_RATE_LIMIT, ApiErrors.keyOf(error))
    }

    @Test
    fun `plain forbidden is not reported as a rate limit`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(403).setBody("""{"message": "Resource not accessible"}"""),
        )

        val error = runCatching { api.issue("o", "r", 1) }.exceptionOrNull() as HttpException

        assertFalse(ApiErrors.isRateLimit(error))
        assertEquals(ApiErrors.KEY_FORBIDDEN, ApiErrors.keyOf(error))
    }

    @Test
    fun `offline failure maps to the network key`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val error = runCatching { api.issue("o", "r", 1) }.exceptionOrNull()

        assertTrue(error is IOException)
        assertEquals(ApiErrors.KEY_NETWORK, ApiErrors.keyOf(error as IOException))
    }

    @Test
    fun `merge rejection surfaces the status code the repository maps`() = runTest {
        // 409 = head moved since the reviewed commit (G-014).
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"message": "Head branch was modified"}"""))

        val response = api.mergePullRequest(
            owner = "o",
            repo = "r",
            number = 1,
            body = com.gityou.app.data.remote.dto.MergePullRequestRequest(
                sha = "abc123",
                mergeMethod = "squash",
            ),
        )

        assertFalse(response.isSuccessful)
        assertEquals(409, response.code())
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/repos/o/r/pulls/1/merge", request.path)
        val body = request.body.readUtf8()
        assertTrue(body, body.contains("\"sha\":\"abc123\""))
        assertTrue(body, body.contains("\"merge_method\":\"squash\""))
    }
}
