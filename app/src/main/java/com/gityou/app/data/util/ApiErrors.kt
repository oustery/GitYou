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

    fun isRateLimit(error: HttpException): Boolean =
        error.code() == 403 &&
            error.response()?.headers()?.get("X-RateLimit-Remaining") == "0"

    fun keyOf(throwable: Throwable): String = when (throwable) {
        is HttpException -> when (throwable.code()) {
            401 -> KEY_UNAUTHORIZED
            403 -> if (isRateLimit(throwable)) KEY_RATE_LIMIT else KEY_FORBIDDEN
            404 -> KEY_NOT_FOUND
            422 -> KEY_VALIDATION
            else -> KEY_HTTP
        }
        is IOException -> KEY_NETWORK
        else -> KEY_UNKNOWN
    }
}
