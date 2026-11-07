package com.gityou.app

import com.gityou.app.data.remote.dto.Repo
import com.gityou.app.data.repo.ReposRepository
import com.gityou.app.data.remote.dto.Page
import com.gityou.app.ui.components.UiState
import com.gityou.app.ui.screens.search.SearchSort
import com.gityou.app.ui.screens.search.SearchViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Regression tests for the search screen. The original implementation read
 * StateFlows through `snapshotFlow`, which never re-emits — the whole screen
 * was dead (F-001). These tests would have caught it (F-026).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = mockk<ReposRepository>()

    private fun repo(name: String) = Repo(name = name, fullName = "owner/$name")

    @Test
    fun `typing a query runs the search after debounce`() = runTest {
        coEvery { repository.search(any(), any(), any()) } returns Page(listOf(repo("kotlinx")), nextPage = null)
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChanged("kotlin")
        // Fewer than the debounce window: the search must not run yet.
        advanceTimeBy(300)
        var content = (viewModel.results.value as UiState.Content).value
        assertEquals(false, content.searched)

        advanceTimeBy(400)
        advanceUntilIdle()

        content = (viewModel.results.value as UiState.Content).value
        assertEquals("kotlin", content.query)
        assertEquals(listOf("kotlinx"), content.repos.map { it.name })
        coVerify(exactly = 1) { repository.search("kotlin", 1, null) }
    }

    @Test
    fun `error state is retried by retrySearch`() = runTest {
        coEvery { repository.search(any(), any(), any()) } throws IOException("offline")
        val viewModel = SearchViewModel(repository)
        viewModel.onQueryChanged("kotlin")
        advanceUntilIdle()
        assertTrue(viewModel.results.value is UiState.Error)

        coEvery { repository.search(any(), any(), any()) } returns Page(listOf(repo("ok")), nextPage = null)
        viewModel.retrySearch()
        advanceUntilIdle()

        val state = viewModel.results.value
        assertTrue(state is UiState.Content)
        assertEquals(listOf("ok"), (state as UiState.Content).value.repos.map { it.name })
    }

    @Test
    fun `loadMore appends a page and flags a failure for retry`() = runTest {
        coEvery { repository.search("kotlin", 1, any()) } returns Page(listOf(repo("first")), nextPage = 2)
        coEvery { repository.search("kotlin", 2, any()) } throws IOException("boom")
        val viewModel = SearchViewModel(repository)
        viewModel.onQueryChanged("kotlin")
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()
        var content = (viewModel.results.value as UiState.Content).value
        assertEquals(1, content.repos.size)
        assertFalse(content.loadingMore)
        assertTrue(content.loadMoreFailed)

        // Retry after the network is back: pagination continues (F-013).
        coEvery { repository.search("kotlin", 2, any()) } returns Page(listOf(repo("second")), nextPage = null)
        viewModel.loadMore()
        advanceUntilIdle()
        content = (viewModel.results.value as UiState.Content).value
        assertEquals(listOf("first", "second"), content.repos.map { it.name })
        assertFalse(content.loadMoreFailed)
    }

    @Test
    fun `changing sort triggers a new search`() = runTest {
        coEvery { repository.search(any(), any(), any()) } returns Page(emptyList(), nextPage = null)
        val viewModel = SearchViewModel(repository)
        viewModel.onQueryChanged("kotlin")
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.search("kotlin", 1, null) }

        viewModel.setSort(SearchSort.STARS)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.search("kotlin", 1, "stars") }
    }
}
