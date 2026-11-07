package com.gityou.app

import com.gityou.app.data.remote.dto.Page
import com.gityou.app.data.remote.dto.Repo
import com.gityou.app.data.repo.ReposRepository
import com.gityou.app.ui.components.UiState
import com.gityou.app.ui.screens.home.HomeViewModel
import io.mockk.coEvery
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Covers the two pagination hazards called out by the audit: a refresh that
 * completes while a `loadMore` is in flight must not be rolled back (F-006),
 * and a failed `loadMore` must stay retryable instead of wedging the list
 * forever (F-013).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = mockk<ReposRepository>()

    private fun repo(name: String) = Repo(name = name, fullName = "owner/$name")

    @Test
    fun `refresh completing during loadMore is not rolled back`() = runTest {
        coEvery { repository.myRepos(1) } returns Page(listOf(repo("a")), nextPage = 2)
        val gate = CompletableDeferred<Page<Repo>>()
        coEvery { repository.myRepos(2) } coAnswers { gate.await() }
        val viewModel = HomeViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMore()
        runCurrent() // state now shows loadingMore = true, network parked on the gate

        // Pull-to-refresh finishes while the loadMore request is still out.
        coEvery { repository.myRepos(1) } returns Page(listOf(repo("fresh")), nextPage = null)
        viewModel.refresh()
        advanceUntilIdle()

        gate.complete(Page(listOf(repo("b")), nextPage = null))
        runCurrent()

        val content = (viewModel.state.value as UiState.Content).value
        // The stale page must NOT overwrite the fresh refresh result (F-006).
        assertEquals(listOf("fresh"), content.repos.map { it.name })
        assertFalse(content.loadingMore)
    }

    @Test
    fun `failed loadMore keeps content and becomes retryable`() = runTest {
        coEvery { repository.myRepos(1) } returns Page(listOf(repo("a")), nextPage = 2)
        coEvery { repository.myRepos(2) } throws IOException("boom")
        val viewModel = HomeViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        var content = (viewModel.state.value as UiState.Content).value
        assertEquals(listOf("a"), content.repos.map { it.name })
        assertFalse(content.loadingMore)
        assertTrue(content.loadMoreFailed)

        // Retry once the network is back (F-013).
        coEvery { repository.myRepos(2) } returns Page(listOf(repo("b")), nextPage = null)
        viewModel.loadMore()
        advanceUntilIdle()

        content = (viewModel.state.value as UiState.Content).value
        assertEquals(listOf("a", "b"), content.repos.map { it.name })
        assertFalse(content.loadMoreFailed)
    }
}
