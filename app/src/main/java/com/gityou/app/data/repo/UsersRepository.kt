package com.gityou.app.data.repo

import com.gityou.app.data.remote.GitHubApi
import com.gityou.app.data.remote.dto.Page
import com.gityou.app.data.remote.dto.Repo
import com.gityou.app.data.remote.dto.User
import com.gityou.app.data.util.LinkHeaderParser
import javax.inject.Inject
import javax.inject.Singleton

/** Public user profiles and their lists (repos, stars, followers). */
@Singleton
class UsersRepository @Inject constructor(
    private val api: GitHubApi,
) {

    suspend fun profile(login: String): User = api.user(login)

    suspend fun repos(login: String, page: Int): Page<Repo> =
        LinkHeaderParser.pageFromResponse(api.userRepos(login, page = page))

    suspend fun stars(login: String, page: Int): Page<Repo> =
        LinkHeaderParser.pageFromResponse(api.userStars(login, page = page))

    suspend fun followers(login: String, page: Int): Page<User> =
        LinkHeaderParser.pageFromResponse(api.followers(login, page = page))

    suspend fun following(login: String, page: Int): Page<User> =
        LinkHeaderParser.pageFromResponse(api.following(login, page = page))
}
