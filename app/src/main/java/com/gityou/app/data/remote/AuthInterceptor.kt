package com.gityou.app.data.remote

import com.gityou.app.data.store.TokenStore
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds authentication and versioning headers to GitHub API requests.
 *
 * The token is attached **only** to `api.github.com` requests: this client is
 * also reachable for the `github.com/login` OAuth endpoints (which must stay
 * token-free) and historically leaked the bearer token to any host it was
 * pointed at (F-005). Explicit per-request headers always win, so the OAuth
 * endpoints keep their `Accept: application/json`.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()

        val isApiHost = request.url.host == API_HOST

        if (isApiHost) {
            if (request.header("Accept") == null) {
                builder.header("Accept", "application/vnd.github+json")
            }
            builder.header("X-GitHub-Api-Version", API_VERSION)

            if (request.header("Authorization") == null) {
                tokenStore.cachedToken()?.let { builder.header("Authorization", "Bearer $it") }
            }
        }

        return chain.proceed(builder.build())
    }

    private companion object {
        const val API_HOST = "api.github.com"
        const val API_VERSION = "2022-11-28"
    }
}
