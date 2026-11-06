package com.gityou.app.data.remote

import com.gityou.app.data.store.TokenStore
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds authentication and versioning headers to GitHub API requests.
 * Explicit per-request headers always win, so the OAuth endpoints can
 * keep their `Accept: application/json` and stay token-free.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()

        if (request.header("Accept") == null) {
            builder.header("Accept", "application/vnd.github+json")
        }
        builder.header("X-GitHub-Api-Version", "2022-11-28")

        if (request.header("Authorization") == null) {
            tokenStore.cachedToken()?.let { builder.header("Authorization", "Bearer $it") }
        }

        return chain.proceed(builder.build())
    }
}
