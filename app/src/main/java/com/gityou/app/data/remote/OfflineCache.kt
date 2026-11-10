package com.gityou.app.data.remote

import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient

/**
 * Offline reading (G-013).
 *
 * The app showed a full-screen error as soon as the network went away, even
 * for a repository the user had just been browsing. A mobile session routinely
 * loses connectivity (metro, lift, plane), so "previously seen content stays
 * readable" is the difference between a usable client and a blank screen.
 *
 * Implemented as a plain HTTP disk cache instead of a database: GitHub answers
 * GETs with `Cache-Control: private, max-age=60` and an ETag, so OkHttp can
 * revalidate cheaply and serve the stored copy when the network fails. That
 * covers every screen at once without duplicating the DTO model into Room.
 */
object OfflineCache {

    private const val CACHE_DIR = "http_cache"

    /** Answers actually written to disk; 50 MB is weeks of normal browsing. */
    private const val CACHE_BYTES = 50L * 1024 * 1024

    /**
     * Only successful GETs are cached. Errors are excluded on purpose: serving
     * a stored 404 as "offline data" would be worse than showing the error.
     */
    fun cache(context: Context): Cache = Cache(
        directory = File(context.cacheDir, CACHE_DIR),
        maxSize = CACHE_BYTES,
    )

    /**
     * When a request cannot reach the network, fall back to the stored (even
     * expired) response and mark it so the UI can say "offline, showing saved
     * data" instead of pretending it is fresh.
     */
    fun offlineFallback(offlineState: OfflineState): Interceptor = Interceptor { chain ->
        val request = chain.request()
        try {
            val live = chain.proceed(request)
            if (live.isSuccessful) offlineState.reportLive()
            live
        } catch (network: java.io.IOException) {
            // `only-if-cached` + `max-stale` accepts any stored copy.
            val cachedRequest = request.newBuilder()
                .cacheControl(
                    okhttp3.CacheControl.Builder()
                        .onlyIfCached()
                        .maxStale(Int.MAX_VALUE, TimeUnit.SECONDS)
                        .build(),
                )
                .build()
            val response = chain.proceed(cachedRequest)
            if (response.code == 504) {
                // Nothing cached: report the original network failure instead
                // of a misleading "unsatisfiable request".
                response.close()
                throw network
            }
            offlineState.reportCacheRead()
            response.newBuilder()
                .header(HEADER_STALE, response.header("Date").orEmpty())
                .build()
        }
    }

    /** Presence of this header means "served from cache while offline". */
    const val HEADER_STALE = "X-GitYou-Stale"

    /**
     * Shared cache configuration, applied to the authenticated client only:
     * markdown images and APK downloads have their own lifetimes and must not
     * evict repository data.
     */
    fun configure(
        context: Context,
        client: OkHttpClient,
        offlineState: OfflineState,
    ): OkHttpClient =
        client.newBuilder()
            .cache(cache(context))
            .addInterceptor(offlineFallback(offlineState))
            .build()
}
