package com.gityou.app.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether the data currently on screen came from the offline cache (G-013).
 *
 * The HTTP layer knows the answer (the fallback interceptor tags such
 * responses), but it has no way to tell the UI. This holder is the one channel
 * between them: the interceptor reports reads from the cache, a successful
 * network response clears the flag, and any screen can show "showing saved
 * data" instead of pretending everything is live.
 *
 * Deliberately not a timestamped database: it only answers "is what I see
 * possibly stale", which is exactly what the banner needs.
 */
@Singleton
class OfflineState @Inject constructor() {

    private val _offline = MutableStateFlow(false)
    val offline: StateFlow<Boolean> = _offline.asStateFlow()

    /** A response came from the cache because the network failed. */
    fun reportCacheRead() {
        _offline.value = true
    }

    /** A response came from the network: the screen is live again. */
    fun reportLive() {
        _offline.value = false
    }
}
