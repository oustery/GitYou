package com.gityou.app.data.remote

import com.gityou.app.data.remote.dto.AccessTokenResponse

/** What the device-flow polling loop should do with the latest response. */
sealed interface PollDecision {
    /** Keep polling after [intervalSeconds]. */
    data class Continue(val intervalSeconds: Int) : PollDecision

    /** The user never confirmed the code in time. */
    data object Expired : PollDecision

    /** The user explicitly denied the request. */
    data object Denied : PollDecision

    /** Any other error reported by the endpoint. */
    data class Failed(val error: String) : PollDecision

    /** We are authenticated. */
    data class Success(val token: String) : PollDecision
}

object DeviceFlow {
    const val GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code"
    const val DEFAULT_INTERVAL_SECONDS = 5

    /**
     * Pure mapping of an access-token response to a polling decision,
     * kept out of the ViewModel so it can be unit-tested directly.
     */
    fun decide(response: AccessTokenResponse, currentIntervalSeconds: Int): PollDecision {
        val token = response.accessToken
        if (!token.isNullOrBlank()) return PollDecision.Success(token)
        return when (response.error) {
            "authorization_pending" -> PollDecision.Continue(currentIntervalSeconds)
            "slow_down" -> PollDecision.Continue(currentIntervalSeconds + SLOW_DOWN_PENALTY_SECONDS)
            "expired_token" -> PollDecision.Expired
            "access_denied" -> PollDecision.Denied
            null -> PollDecision.Failed("unknown_response")
            else -> PollDecision.Failed(response.error)
        }
    }

    const val SLOW_DOWN_PENALTY_SECONDS = 5
}
