package com.gityou.app

import com.gityou.app.data.remote.dto.Page
import com.gityou.app.data.remote.dto.Repo
import com.gityou.app.data.remote.dto.User
import com.gityou.app.data.repo.UsersRepository
import com.gityou.app.ui.components.UiState
import com.gityou.app.ui.screens.user.UserTab
import com.gityou.app.ui.screens.user.UserViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Profile loads eagerly; follower lists load lazily on their first tab visit. */
@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = mockk<UsersRepository>()

    private fun user(login: String) = User(login = login, id = login.hashCode().toLong())
    private fun repo(name: String) = Repo(name = name, fullName = "octocat/$name")

    private fun viewModel(): UserViewModel {
        val savedState = androidx.lifecycle.SavedStateHandle(mapOf("login" to "octocat"))
        return UserViewModel(savedState, repository)
    }

    @Test
    fun `profile and first pages load eagerly`() = runTest {
        coEvery { repository.profile("octocat") } returns user("octocat")
        coEvery { repository.repos("octocat", 1) } returns Page(listOf(repo("hello")), nextPage = 2)
        coEvery { repository.stars("octocat", 1) } returns Page(listOf(repo("starred")), nextPage = null)

        val vm = viewModel()
        advanceUntilIdle()

        val content = (vm.state.value as UiState.Content).value
        assertEquals("octocat", content.user.login)
        assertEquals(listOf("hello"), content.list(UserTab.REPOS).items.map { (it as Repo).name })
        assertEquals(listOf("starred"), content.list(UserTab.STARS).items.map { (it as Repo).name })
        // Follower lists were never requested: they load on tab visit.
        assertTrue(content.list(UserTab.FOLLOWERS).items.isEmpty())
    }

    @Test
    fun `followers tab loads lazily exactly once`() = runTest {
        coEvery { repository.profile("octocat") } returns user("octocat")
        coEvery { repository.repos("octocat", 1) } returns Page(emptyList(), nextPage = null)
        coEvery { repository.stars("octocat", 1) } returns Page(emptyList(), nextPage = null)
        coEvery { repository.followers("octocat", 1) } returns Page(listOf(user("torvalds")), nextPage = null)

        val vm = viewModel()
        advanceUntilIdle()
        vm.selectTab(UserTab.FOLLOWERS)
        advanceUntilIdle()
        vm.selectTab(UserTab.REPOS)
        advanceUntilIdle()
        vm.selectTab(UserTab.FOLLOWERS) // second visit must not refetch
        advanceUntilIdle()

        val content = (vm.state.value as UiState.Content).value
        assertEquals(listOf("torvalds"), content.list(UserTab.FOLLOWERS).items.map { (it as User).login })
        coVerify(exactly = 1) { repository.followers("octocat", 1) }
    }

    @Test
    fun `failed follower load stays retryable`() = runTest {
        coEvery { repository.profile("octocat") } returns user("octocat")
        coEvery { repository.repos("octocat", 1) } returns Page(emptyList(), nextPage = null)
        coEvery { repository.stars("octocat", 1) } returns Page(emptyList(), nextPage = null)
        coEvery { repository.followers("octocat", 1) } throws IOException("boom")

        val vm = viewModel()
        advanceUntilIdle()
        vm.selectTab(UserTab.FOLLOWERS)
        advanceUntilIdle()

        var failed = (vm.state.value as UiState.Content).value.list(UserTab.FOLLOWERS).loadMoreFailed
        assertTrue(failed)

        coEvery { repository.followers("octocat", 1) } returns Page(listOf(user("gaearon")), nextPage = null)
        vm.selectTab(UserTab.FOLLOWERS)
        advanceUntilIdle()

        val content = (vm.state.value as UiState.Content).value
        assertEquals(listOf("gaearon"), content.list(UserTab.FOLLOWERS).items.map { (it as User).login })
        failed = content.list(UserTab.FOLLOWERS).loadMoreFailed
        assertFalse(failed)
    }

    @Test
    fun `profile error surfaces as UiState Error`() = runTest {
        coEvery { repository.profile("octocat") } throws IOException("offline")
        val vm = viewModel()
        advanceUntilIdle()
        assertTrue(vm.state.value is UiState.Error)
    }
}
