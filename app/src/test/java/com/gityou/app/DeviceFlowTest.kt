package com.gityou.app

import com.gityou.app.data.remote.DeviceFlow
import com.gityou.app.data.remote.PollDecision
import com.gityou.app.data.remote.dto.AccessTokenResponse
import com.gityou.app.data.remote.dto.DeviceCodeResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceFlowTest {

    @Test
    fun `device code response parses snake_case fields`() {
        val json = """
            {
              "device_code": "ABC123",
              "user_code": "AB12-CD34",
              "verification_uri": "https://github.com/login/device",
              "expires_in": 899,
              "interval": 5
            }
        """.trimIndent()

        val parsed = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        }.decodeFromString(DeviceCodeResponse.serializer(), json)

        assertEquals("ABC123", parsed.deviceCode)
        assertEquals("AB12-CD34", parsed.userCode)
        assertEquals(899, parsed.expiresIn)
        assertEquals(5, parsed.interval)
    }

    @Test
    fun `access token success maps to Success decision`() {
        val response = AccessTokenResponse(accessToken = "gho_token", tokenType = "bearer")

        val decision = DeviceFlow.decide(response, currentIntervalSeconds = 5)

        assertEquals(PollDecision.Success("gho_token"), decision)
    }

    @Test
    fun `authorization pending keeps polling with the same interval`() {
        val response = AccessTokenResponse(error = "authorization_pending")

        val decision = DeviceFlow.decide(response, currentIntervalSeconds = 5)

        assertEquals(PollDecision.Continue(5), decision)
    }

    @Test
    fun `slow down increases the interval by five seconds`() {
        val response = AccessTokenResponse(error = "slow_down")

        val decision = DeviceFlow.decide(response, currentIntervalSeconds = 5)

        assertEquals(PollDecision.Continue(10), decision)
    }

    @Test
    fun `expired token terminates the flow`() {
        val decision = DeviceFlow.decide(AccessTokenResponse(error = "expired_token"), 5)
        assertEquals(PollDecision.Expired, decision)
    }

    @Test
    fun `access denied terminates the flow`() {
        val decision = DeviceFlow.decide(AccessTokenResponse(error = "access_denied"), 5)
        assertEquals(PollDecision.Denied, decision)
    }

    @Test
    fun `unknown errors surface as failed`() {
        val decision = DeviceFlow.decide(AccessTokenResponse(error = "unsupported_grant_type"), 5)
        assertTrue(decision is PollDecision.Failed)
    }

    @Test
    fun `issue DTO marks pull requests`() {
        val json = """
            {
              "number": 7,
              "title": "Fix login",
              "state": "open",
              "pull_request": { "url": "https://api.github.com/repos/o/r/pulls/7" }
            }
        """.trimIndent()

        val parsed = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        }.decodeFromString(com.gityou.app.data.remote.dto.Issue.serializer(), json)

        assertTrue(parsed.isPullRequest)

        val plain = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        }.decodeFromString(
            com.gityou.app.data.remote.dto.Issue.serializer(),
            """{"number": 8, "title": "Bug", "state": "open"}""",
        )

        assertEquals(false, plain.isPullRequest)
    }
}
