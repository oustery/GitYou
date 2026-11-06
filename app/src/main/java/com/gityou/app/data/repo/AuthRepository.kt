package com.gityou.app.data.repo

import com.gityou.app.data.remote.AuthApi
import com.gityou.app.data.remote.DeviceFlow
import com.gityou.app.data.remote.GitHubApi
import com.gityou.app.data.remote.dto.AccessTokenResponse
import com.gityou.app.data.remote.dto.DeviceCodeResponse
import com.gityou.app.data.remote.dto.User
import com.gityou.app.data.store.TokenStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication: OAuth Device Flow as the primary path and a personal
 * access token as an always-available fallback.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val gitHubApi: GitHubApi,
    private val tokenStore: TokenStore,
) {

    suspend fun clientId(): String = tokenStore.clientId()

    suspend fun saveClientId(clientId: String) = tokenStore.saveClientId(clientId)

    suspend fun beginDeviceFlow(clientId: String): DeviceCodeResponse =
        authApi.deviceCode(clientId = clientId, scope = SCOPES)

    suspend fun pollForToken(clientId: String, deviceCode: String, intervalSeconds: Int): AccessTokenResponse =
        authApi.accessToken(
            clientId = clientId,
            deviceCode = deviceCode,
            grantType = DeviceFlow.GRANT_TYPE,
        )

    /**
     * Validates a personal access token against the API and persists it.
     * On failure the token is not kept, so the app stays in signed-out state.
     */
    suspend fun signInWithPat(token: String): User {
        tokenStore.saveToken(token)
        return try {
            gitHubApi.authenticatedUser()
        } catch (t: Throwable) {
            tokenStore.clearToken()
            throw t
        }
    }

    suspend fun signOut() = tokenStore.clearToken()

    /** Fetches the user for the currently stored token. Throws on failure. */
    suspend fun currentUser(): User = gitHubApi.authenticatedUser()

    companion object {
        const val SCOPES = "repo read:user notifications"
    }
}
