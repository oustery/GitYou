package com.gityou.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    @SerialName("id") val id: String,
    @SerialName("unread") val unread: Boolean = false,
    @SerialName("reason") val reason: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("repository") val repository: NotificationRepo? = null,
    @SerialName("subject") val subject: Subject? = null,
)

@Serializable
data class NotificationRepo(
    @SerialName("id") val id: Long = 0,
    @SerialName("name") val name: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("owner") val owner: User? = null,
)

@Serializable
data class Subject(
    @SerialName("title") val title: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("latest_comment_url") val latestCommentUrl: String? = null,
    @SerialName("type") val type: String? = null, // Issue | PullRequest | Release | ...
)
