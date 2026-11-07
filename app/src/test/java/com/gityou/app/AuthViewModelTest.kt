package com.gityou.app

import com.gityou.app.data.remote.dto.AccessTokenResponse
import com.gityou.app.data.remote.dto.DeviceCodeResponse
import com.gityou.app.data.repo.AuthRepository
import com.gityou.app.ui.screens.auth.AuthViewModel
import com.gityou.app.ui.screens.auth.DeviceFlowPhase
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.time.TimeSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Regression test for F-003: the polling Job used to be thrown away, so
 * pressing «Отмена» left the device-flow loop running (and able to complete
 * a sign-in) for up to 15 minutes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = mockk<AuthRepository>()

    private val deviceCode = DeviceCodeResponse(
        deviceCode = "device-code",
        userCode = "ABCD-1234",
        verificationUri = "https://github.com/login/device",
        expiresIn = 900,
        interval = 5,
    )

    @Test
    fun `cancelDeviceFlow stops the polling loop`() = runTest {
        coEvery { repository.clientId() } returns "client-id"
        coEvery { repository.beginDeviceFlow("client-id") } returns deviceCode
        var polls = 0
        coEvery { repository.pollForToken(any(), any()) } coAnswers {
            polls++
            AccessTokenResponse(error = "authorization_pending")
        }

        val viewModel = AuthViewModel(repository, TimeSource.Monotonic)
        advanceUntilIdle() // init loads the client id

        viewModel.startDeviceFlow()
        advanceTimeBy(30_000) // ~6 polls at the 5s interval
        runCurrent()
        assertEquals(DeviceFlowPhase.WAITING, viewModel.deviceFlow.value.phase)
        val pollsBeforeCancel = polls
        assertTrue(pollsBeforeCancel >= 3)

        viewModel.cancelDeviceFlow()
        assertEquals(DeviceFlowPhase.IDLE, viewModel.deviceFlow.value.phase)

        // Even after many further intervals nothing polls the token endpoint.
        advanceTimeBy(300_000)
        runCurrent()
        assertEquals(pollsBeforeCancel, polls)
    }

    @Test
    fun `slow_down increases the polling interval`() = runTest {
        coEvery { repository.clientId() } returns "client-id"
        coEvery { repository.beginDeviceFlow("client-id") } returns deviceCode
        val responses = listOf(
            AccessTokenResponse(error = "authorization_pending"),
            AccessTokenResponse(error = "slow_down", interval = 10),
            AccessTokenResponse(error = "authorization_pending"),
            AccessTokenResponse(error = "authorization_pending"),
        )
        var index = 0
        coEvery { repository.pollForToken(any(), any()) } coAnswers {
            responses[index++.coerceAtMost(responses.lastIndex)]
        }

        val viewModel = AuthViewModel(repository, TimeSource.Monotonic)
        advanceUntilIdle()
        viewModel.startDeviceFlow()

        // Timeline: poll#1 at 5s -> Continue(5); poll#2 at 10s -> slow_down
        // -> Continue(15); poll#3 at 25s.
        advanceTimeBy(9_000)
        runCurrent()
        assertEquals(1, index) // only the first poll happened (5s)
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(3, index)
        assertEquals(DeviceFlowPhase.WAITING, viewModel.deviceFlow.value.phase)
    }
}
