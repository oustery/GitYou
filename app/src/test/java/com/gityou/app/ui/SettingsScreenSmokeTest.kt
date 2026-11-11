package com.gityou.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.gityou.app.R
import com.gityou.app.data.remote.dto.User
import com.gityou.app.data.repo.AuthRepository
import com.gityou.app.data.repo.UpdateRepository
import com.gityou.app.data.store.TokenStore
import com.gityou.app.data.telemetry.CrashReporter
import com.gityou.app.data.telemetry.NoopTelemetry
import com.gityou.app.ui.screens.settings.SettingsScreen
import com.gityou.app.ui.screens.settings.SettingsViewModel
import com.gityou.app.ui.screens.settings.UpdateViewModel
import com.gityou.app.ui.theme.GitYouTheme
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Smoke test of the settings flow (G-011): every section renders and the new
 * diagnostics toggle is wired to persistence (G-017).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class SettingsScreenSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `renders diagnostics section and persists the telemetry toggle`() {
        val tokenStore = mockk<TokenStore>(relaxed = true) {
            every { settingsFlow } returns MutableStateFlow(TokenStore.AppSettings())
        }
        val authRepository = mockk<AuthRepository> {
            coEvery { currentUser() } returns User(login = "octocat")
        }
        val crashReporter = mockk<CrashReporter>(relaxed = true) {
            every { count() } returns 0
            every { latest() } returns null
        }
        val viewModel = SettingsViewModel(
            tokenStore = tokenStore,
            authRepository = authRepository,
            telemetry = NoopTelemetry,
            crashReporter = crashReporter,
        )
        val updateViewModel = UpdateViewModel(mockk<UpdateRepository>(relaxed = true))

        compose.setContent {
            GitYouTheme {
                SettingsScreen(onBack = {}, viewModel = viewModel, updateViewModel = updateViewModel)
            }
        }
        compose.waitForIdle()

        // The Diagnostics section sits at the bottom of a long scrollable
        // column: scroll to it, then assert.
        val scroller = compose.onNode(hasScrollAction())
        scroller.performScrollToNode(hasText(context.getString(R.string.settings_diagnostics_section), ignoreCase = true))
        compose.waitForIdle()
        compose.onNodeWithText(
            context.getString(R.string.settings_diagnostics_section),
            ignoreCase = true,
        ).assertExists()
        scroller.performScrollToNode(hasText(context.getString(R.string.settings_telemetry_title)))
        compose.waitForIdle()
        compose.onNodeWithText(context.getString(R.string.settings_telemetry_title))
            .assertExists()
        scroller.performScrollToNode(hasText(context.getString(R.string.settings_crash_none)))
        compose.waitForIdle()
        compose.onNodeWithText(context.getString(R.string.settings_crash_none))
            .assertExists()

        // The switch must persist through the TokenStore, not just flip state.
        viewModel.setTelemetryEnabled(true)
        compose.waitForIdle()
        coVerify(exactly = 1) { tokenStore.saveTelemetryEnabled(true) }
    }
}
