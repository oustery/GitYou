package com.gityou.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.gityou.app.data.remote.dto.Page
import com.gityou.app.data.remote.dto.Repo
import com.gityou.app.data.repo.ReposRepository
import com.gityou.app.data.telemetry.NoopTelemetry
import com.gityou.app.ui.screens.search.SearchScreen
import com.gityou.app.ui.screens.search.SearchViewModel
import com.gityou.app.ui.theme.GitYouTheme
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Smoke test of the search flow (G-011): trending repositories render before
 * any query and typing reaches the view model through the field.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class SearchScreenSmokeTest {

    @get:Rule
    val compose = createComposeRule()


    @Test
    fun `renders trending, then reflects the typed query`() {
        val repository = mockk<ReposRepository> {
            coEvery { trending(any()) } returns Page(
                items = listOf(
                    Repo(name = "torvalds", fullName = "torvalds/linux"),
                ),
                nextPage = null,
            )
            // The typed query settles after a 600 ms debounce into a real
            // search; stub all types so the smoke test never hits an
            // unstubbed mock from the polling coroutine.
            coEvery { search(any(), any(), any()) } returns Page(items = emptyList(), nextPage = null)
            coEvery { searchUsers(any(), any()) } returns Page(items = emptyList(), nextPage = null)
            coEvery { searchIssues(any(), any()) } returns Page(items = emptyList(), nextPage = null)
        }
        val viewModel = SearchViewModel(repository, NoopTelemetry)

        compose.setContent {
            GitYouTheme {
                SearchScreen(
                    onOpenRepo = { _, _ -> },
                    onOpenUser = {},
                    onOpenIssue = { _, _, _, _ -> },
                    viewModel = viewModel,
                )
            }
        }

        // Trending loads only after the view model's 600 ms debounce — real
        // time, so wait for the node instead of trusting waitForIdle().
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("torvalds/linux").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("torvalds/linux").assertIsDisplayed()

        // Typing must reach the view model: the field value is driven by the
        // view model's query flow, so the typed text coming back proves the
        // whole input loop.
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("kotlin")
        compose.waitForIdle()
        compose.onNodeWithText("kotlin").assertIsDisplayed()
    }
}
