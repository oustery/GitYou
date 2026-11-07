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

    /** Best-effort count of unread items on the first page; null on failure. */
    suspend fun unreadEstimate(): Int? =
        safeCall {
            LinkHeaderParser.pageFromResponse(api.notifications(perPage = 50))
                .items
                .count { it.unread }
        }.getOrNull()

    suspend fun markThreadRead(id: String) {
        val response = api.markThreadRead(id)
        if (!response.isSuccessful) throw HttpException(response)
    }

    suspend fun markAllRead() {
        val response = api.markAllNotificationsRead()
        if (!response.isSuccessful) throw HttpException(response)
    }
}
