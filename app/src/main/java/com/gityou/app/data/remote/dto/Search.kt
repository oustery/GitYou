package com.gityou.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchUsersResponse(
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("incomplete_results") val incompleteResults: Boolean = false,
    @SerialName("items") val items: List<User> = emptyList(),
)

@Serializable
data class SearchIssuesResponse(
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("incomplete_results") val incompleteResults: Boolean = false,
    @SerialName("items") val items: List<Issue> = emptyList(),
)
