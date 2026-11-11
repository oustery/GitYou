package com.gityou.app.ui

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import com.gityou.app.R
import com.gityou.app.data.repo.AuthRepository
import com.gityou.app.data.telemetry.NoopTelemetry
import com.gityou.app.ui.screens.auth.AuthScreen
import com.gityou.app.ui.screens.auth.AuthViewModel
import com.gityou.app.ui.theme.GitYouTheme
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.time.TimeSource
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Smoke test of the auth flow (G-011): the first screen a new user sees must
 * render, accept a Client ID and only then offer Device Flow.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class AuthScreenSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `renders and unlocks device flow after the client id is saved`() {
        val repository = mockk<AuthRepository> {
            coEvery { clientId() } returnsMany listOf("", "saved-client-id")
            coEvery { saveClientId(any()) } returns Unit
        }
        val viewModel = AuthViewModel(repository, TimeSource.Monotonic, NoopTelemetry)

        compose.setContent {
            GitYouTheme { AuthScreen(viewModel = viewModel) }
        }
        compose.waitForIdle()

        compose.onNodeWithText(context.getString(R.string.app_name)).assertIsDisplayed()
        val saveLabel = context.getString(R.string.action_save)
        compose.onNodeWithText(saveLabel).assertIsDisplayed()

        // The Client ID field is the first text input on the screen (the PAT
        // fallback field comes after the Device Flow card).
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("saved-client-id")
        compose.waitForIdle()
        compose.onNodeWithText(saveLabel).performClick()
        compose.waitForIdle()

        // The client id is persisted, so the Device Flow entry unlocks.
        compose.onNodeWithText(context.getString(R.string.auth_device_flow_button))
            .assertIsDisplayed()
            .assertIsEnabled()
    }
}
