package com.gityou.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A published repository release (`GET /repos/{o}/{r}/releases`). */
@Serializable
data class Release(
    @SerialName("id") val id: Long = 0,
    @SerialName("tag_name") val tagName: String,
    @SerialName("name") val name: String? = null,
    @SerialName("body") val body: String? = null,
    @SerialName("draft") val draft: Boolean = false,
    @SerialName("prerelease") val preRelease: Boolean = false,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("author") val author: User? = null,
    @SerialName("assets") val assets: List<ReleaseAsset> = emptyList(),
) {
    val title: String get() = name?.takeIf { it.isNotBlank() } ?: tagName
}

/** A downloadable binary attached to a release. */
@Serializable
data class ReleaseAsset(
    @SerialName("id") val id: Long = 0,
    @SerialName("name") val name: String,
    @SerialName("size") val size: Long = 0,
    @SerialName("download_count") val downloadCount: Int = 0,
    @SerialName("browser_download_url") val downloadUrl: String? = null,
)

/** A lightweight git tag (`GET /repos/{o}/{r}/tags`). */
@Serializable
data class Tag(
    @SerialName("name") val name: String,
    @SerialName("commit") val commit: TagCommit? = null,
)

@Serializable
data class TagCommit(
    @SerialName("sha") val sha: String? = null,
    @SerialName("url") val url: String? = null,
)
