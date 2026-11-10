package com.gityou.app.data.util

import retrofit2.HttpException
import java.io.IOException

/**
 * Maps throwables to stable error keys. UI layer resolves the keys to
 * localized messages, ViewModels never build strings themselves.
 */
object ApiErrors {

    const val KEY_UNAUTHORIZED = "unauthorized"
    const val KEY_RATE_LIMIT = "rate_limit"
    const val KEY_FORBIDDEN = "forbidden"
    const val KEY_NOT_FOUND = "not_found"
    const val KEY_VALIDATION = "validation"
    const val KEY_HTTP = "http"
    const val KEY_NETWORK = "network"
    const val KEY_UNKNOWN = "unknown"

    /**
     * True for both primary exhaustion (403 + `X-RateLimit-Remaining: 0`) and
     * secondary/abuse limits (429, or 403 carrying `Retry-After`). Previously
     * only the primary case was recognised, so 429 fell through to a generic
     * "HTTP error" with no hint that waiting would fix it (G-005).
     */
    fun isRateLimit(error: HttpException): Boolean {
        val headers = error.response()?.headers()
        return when (error.code()) {
            429 -> true
            403 -> headers?.get("X-RateLimit-Remaining") == "0" ||
                headers?.get("Retry-After") != null
            else -> false
        }
    }

    fun keyOf(throwable: Throwable): String = when (throwable) {
        is HttpException -> when (throwable.code()) {
            401 -> KEY_UNAUTHORIZED
            403, 429 -> if (isRateLimit(throwable)) KEY_RATE_LIMIT else KEY_FORBIDDEN
            404 -> KEY_NOT_FOUND
            422 -> KEY_VALIDATION
            else -> KEY_HTTP
        }
        is IOException -> KEY_NETWORK
        else -> KEY_UNKNOWN
    }
}
