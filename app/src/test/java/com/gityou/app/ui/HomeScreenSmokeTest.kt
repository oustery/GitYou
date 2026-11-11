package com.gityou.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.gityou.app.R
import com.gityou.app.data.remote.dto.Page
import com.gityou.app.data.remote.dto.Repo
import com.gityou.app.data.repo.ReposRepository
import com.gityou.app.ui.screens.home.HomeScreen
import com.gityou.app.ui.screens.home.HomeViewModel
import com.gityou.app.ui.theme.GitYouTheme
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Smoke test of the home flow (G-011): the repository list renders real data
 * and the top-bar actions are wired to their callbacks.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class HomeScreenSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `renders repositories and opens settings`() {
        val repository = mockk<ReposRepository> {
            coEvery { myRepos(any()) } returns Page(
                items = listOf(
                    Repo(name = "Hello-World", fullName = "octocat/Hello-World"),
                ),
                nextPage = null,
            )
        }
        val viewModel = HomeViewModel(repository)

        var settingsOpened = false
        compose.setContent {
            GitYouTheme {
                HomeScreen(
                    onOpenRepo = { _, _ -> },
                    onOpenSettings = { settingsOpened = true },
                    onCreateRepo = {},
                    viewModel = viewModel,
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText("octocat/Hello-World").assertIsDisplayed()

        compose.onNodeWithContentDescription(context.getString(R.string.settings_title))
            .performClick()
        compose.waitUntil("settings callback") { settingsOpened }
        assert(settingsOpened)
    }

    @Test
    fun `empty repository list shows the empty state`() {
        val repository = mockk<ReposRepository> {
            coEvery { myRepos(any()) } returns Page(items = emptyList(), nextPage = null)
        }
        val viewModel = HomeViewModel(repository)

        compose.setContent {
            GitYouTheme {
                HomeScreen(
                    onOpenRepo = { _, _ -> },
                    onOpenSettings = {},
                    onCreateRepo = {},
                    viewModel = viewModel,
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(context.getString(R.string.home_empty_title)).assertIsDisplayed()
    }
}
