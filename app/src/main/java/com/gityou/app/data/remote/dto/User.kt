package com.gityou.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("login") val login: String,
    @SerialName("id") val id: Long = 0,
    @SerialName("name") val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("company") val company: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("blog") val blog: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("followers") val followers: Int = 0,
    @SerialName("following") val following: Int = 0,
    @SerialName("public_repos") val publicRepos: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
)
