package com.gityou.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body for `POST /user/repos`. */
@Serializable
data class CreateRepoRequest(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("private") val private: Boolean = false,
    @SerialName("auto_init") val autoInit: Boolean = false,
)

/** One file of a gist being created. */
@Serializable
data class GistFileInput(
    @SerialName("content") val content: String,
)

/** Body for `POST /gists`. */
@Serializable
data class CreateGistRequest(
    @SerialName("description") val description: String? = null,
    @SerialName("public") val public: Boolean = false,
    @SerialName("files") val files: Map<String, GistFileInput>,
)

@Serializable
data class GistFile(
    @SerialName("filename") val filename: String = "",
    @SerialName("raw_url") val rawUrl: String? = null,
    @SerialName("content") val content: String? = null,
)

/** Created gist response (only the fields the app uses). */
@Serializable
data class Gist(
    @SerialName("id") val id: String = "",
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("public") val public: Boolean = false,
)
