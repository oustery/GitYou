package com.gityou.app.data.util

import kotlin.coroutines.cancellation.CancellationException

/**
 * Single wrapper for fallible suspend calls: failures become [Result.failure],
 * while [CancellationException] always rethrows so structured concurrency is
 * never broken (a cancelled screen must stop mutating its state).
 *
 * Review rule: ViewModels must not hand-roll `catch (t: Throwable)` /
 * `runCatching` around suspend calls — use this helper instead.
 */
suspend fun <T> safeCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (t: Throwable) {
    Result.failure(t)
}
