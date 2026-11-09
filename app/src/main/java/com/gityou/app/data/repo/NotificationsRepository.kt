package com.gityou.app.data.repo

import com.gityou.app.data.remote.GitHubApi
import com.gityou.app.data.remote.dto.Notification
import com.gityou.app.data.remote.dto.Page
import com.gityou.app.data.util.LinkHeaderParser
import com.gityou.app.data.util.safeCall
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/** Notifications feed and read state. */
@Singleton
class NotificationsRepository @Inject constructor(
    private val api: GitHubApi,
) {

    suspend fun notifications(page: Int): Page<Notification> =
        LinkHeaderParser.pageFromResponse(api.notifications(page = page))

    /**
     * Total number of unread threads, or null on failure.
     *
     * This used to count only the first page (≤50), so the badge was a floor
     * rather than the real number (G-004). GitHub caps `per_page` at 50 for
     * notifications, therefore the walk is Link-header driven and bounded.
     */
    suspend fun unreadCount(): Int? = safeCall {
        var total = 0
        var page = 1
        while (page <= MAX_UNREAD_PAGES) {
            val parsed = LinkHeaderParser.pageFromResponse(
                api.notifications(perPage = 50, page = page),
            )
            total += parsed.items.count { it.unread }
            val next = parsed.nextPage
            if (next == null || next <= page) break
            page = next
        }
        total
    }.getOrNull()

    suspend fun markThreadRead(id: String) {
        val response = api.markThreadRead(id)
        if (!response.isSuccessful) throw HttpException(response)
    }

    suspend fun markAllRead() {
        val response = api.markAllNotificationsRead()
        if (!response.isSuccessful) throw HttpException(response)
    }

    private companion object {
        /** 50 × 100 pages — far beyond any real inbox, guards a broken Link header. */
        const val MAX_UNREAD_PAGES = 100
    }
}
