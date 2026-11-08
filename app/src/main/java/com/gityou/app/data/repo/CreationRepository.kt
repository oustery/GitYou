package com.gityou.app.data.repo

import com.gityou.app.data.remote.GitHubApi
import com.gityou.app.data.remote.dto.CreateGistRequest
import com.gityou.app.data.remote.dto.CreateRepoRequest
import com.gityou.app.data.remote.dto.Gist
import com.gityou.app.data.remote.dto.GistFileInput
import com.gityou.app.data.remote.dto.Repo
import javax.inject.Inject
import javax.inject.Singleton

/** Content creation flows started outside a repository: new repo and new gist. */
@Singleton
class CreationRepository @Inject constructor(
    private val api: GitHubApi,
) {

    suspend fun createRepo(
        name: String,
        description: String?,
        private: Boolean,
        autoInit: Boolean,
    ): Repo = api.createRepo(
        CreateRepoRequest(
            name = name,
            description = description?.takeIf { it.isNotBlank() },
            private = private,
            autoInit = autoInit,
        ),
    )

    suspend fun createGist(
        description: String?,
        public: Boolean,
        filename: String,
        content: String,
    ): Gist = api.createGist(
        CreateGistRequest(
            description = description?.takeIf { it.isNotBlank() },
            public = public,
            files = mapOf(filename to GistFileInput(content)),
        ),
    )
}
