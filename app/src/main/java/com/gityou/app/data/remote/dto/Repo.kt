package com.gityou.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Repo(
    @SerialName("id") val id: Long = 0,
    @SerialName("name") val name: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("owner") val owner: User? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("language") val language: String? = null,
    @SerialName("stargazers_count") val stargazersCount: Int = 0,
    @SerialName("forks_count") val forksCount: Int = 0,
    @SerialName("open_issues_count") val openIssuesCount: Int = 0,
    @SerialName("watchers_count") val watchersCount: Int = 0,
    @SerialName("fork") val fork: Boolean = false,
    @SerialName("archived") val archived: Boolean = false,
    @SerialName("default_branch") val defaultBranch: String = "main",
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("pushed_at") val pushedAt: String? = null,
    @SerialName("license") val license: License? = null,
)

@Serializable
data class License(
    @SerialName("name") val name: String? = null,
    @SerialName("spdx_id") val spdxId: String? = null,
)

@Serializable
data class Branch(
    @SerialName("name") val name: String,
    @SerialName("protected") val isProtected: Boolean = false,
)

@Serializable
data class ContentItem(
    @SerialName("name") val name: String,
    @SerialName("path") val path: String,
    @SerialName("type") val type: String, // "file" | "dir"
    @SerialName("size") val size: Long = 0,
    @SerialName("content") val content: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
)

@Serializable
data class SearchReposResponse(
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("incomplete_results") val incompleteResults: Boolean = false,
    @SerialName("items") val items: List<Repo> = emptyList(),
)

/** A single page of a paginated list, extracted from the Link header. */
data class Page<T>(
    val items: List<T>,
    val nextPage: Int?,
) {
    val hasMore: Boolean get() = nextPage != null
}
