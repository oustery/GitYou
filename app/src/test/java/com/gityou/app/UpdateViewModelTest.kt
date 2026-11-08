package com.gityou.app

import com.gityou.app.BuildConfig
import com.gityou.app.data.remote.dto.Release
import com.gityou.app.data.remote.dto.ReleaseAsset
import com.gityou.app.data.repo.UpdateRepository
import com.gityou.app.data.update.UpdateInfo
import com.gityou.app.data.util.ApiErrors
import com.gityou.app.ui.screens.settings.UpdateUiState
import com.gityou.app.ui.screens.settings.UpdateViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** State machine of the Settings → About → Check-for-updates flow. */
@OptIn(ExperimentalCoroutinesApi::class)
class UpdateViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = mockk<UpdateRepository>()

    @Before
    fun stubCleanup() {
        // Init wipes stale APKs from previous sessions.
        every { repository.clearDownloads() } just runs
    }

    private fun asset(name: String = "app-arm64-v8a-release.apk", size: Long = 100) =
        ReleaseAsset(id = 1, name = name, size = size, downloadUrl = "https://example.com/$name")

    private fun update(tag: String = "v99.0.0", withAsset: Boolean = true): UpdateInfo {
        val release = Release(tagName = tag, assets = if (withAsset) listOf(asset()) else emptyList())
        return UpdateInfo(release, if (withAsset) release.assets.first() else null)
    }

    private fun viewModel() = UpdateViewModel(repository)

    @Test
    fun `newer release becomes Available`() = runTest {
        coEvery { repository.checkForUpdate(BuildConfig.VERSION_NAME) } returns Result.success(update("v1.3.0"))

        val vm = viewModel()
        vm.check()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is UpdateUiState.Available)
        assertEquals("1.3.0", (state as UpdateUiState.Available).update.displayVersion)
    }

    @Test
    fun `same version reports UpToDate`() = runTest {
        coEvery { repository.checkForUpdate(BuildConfig.VERSION_NAME) } returns Result.success(null)

        val vm = viewModel()
        vm.check()
        advanceUntilIdle()

        assertTrue(vm.state.value is UpdateUiState.UpToDate)
    }

    @Test
    fun `check failure maps to a stable error key`() = runTest {
        coEvery { repository.checkForUpdate(BuildConfig.VERSION_NAME) } returns Result.failure(IOException("offline"))

        val vm = viewModel()
        vm.check()
        advanceUntilIdle()

        assertEquals(ApiErrors.KEY_NETWORK, (vm.state.value as UpdateUiState.Failed).errorKey)
    }

    @Test
    fun `dismiss resets transient states to Idle`() = runTest {
        coEvery { repository.checkForUpdate(BuildConfig.VERSION_NAME) } returns Result.failure(IOException("offline"))

        val vm = viewModel()
        vm.check()
        advanceUntilIdle()
        vm.dismiss()

        assertTrue(vm.state.value is UpdateUiState.Idle)
    }

    @Test
    fun `download publishes progress and ends Ready`() = runTest {
        val info = update()
        val apk = asset(size = 100)
        coEvery { repository.downloadApk(apk, any()) } coAnswers {
            secondArg<(Long, Long) -> Unit>().invoke(50, 100)
            secondArg<(Long, Long) -> Unit>().invoke(100, 100)
            Result.success(File("cache/updates/${apk.name}"))
        }

        val vm = viewModel()
        vm.download(info)
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is UpdateUiState.Ready)
        assertEquals(File("cache/updates/${apk.name}"), (state as UpdateUiState.Ready).file)
    }

    @Test
    fun `download failure maps to a stable error key`() = runTest {
        val info = update()
        coEvery { repository.downloadApk(any(), any()) } returns Result.failure(IOException("interrupted"))

        val vm = viewModel()
        vm.download(info)
        advanceUntilIdle()

        assertEquals(ApiErrors.KEY_NETWORK, (vm.state.value as UpdateUiState.Failed).errorKey)
    }

    @Test
    fun `cancel returns the dialog to the offer instead of Ready`() = runTest {
        val info = update()
        val apk = asset(size = 100)
        val gate = CompletableDeferred<Unit>()
        coEvery { repository.downloadApk(apk, any()) } coAnswers {
            secondArg<(Long, Long) -> Unit>().invoke(50, 100)
            gate.await()
            Result.success(File("cache/updates/${apk.name}"))
        }

        val vm = viewModel()
        vm.download(info)
        advanceUntilIdle()
        assertTrue((vm.state.value as UpdateUiState.Downloading).downloadedBytes == 50L)

        vm.cancelDownload()
        assertTrue(vm.state.value is UpdateUiState.Available)

        // The download finishes in the background anyway: it must NOT flip the
        // dialog back to Ready after the user cancelled it.
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(vm.state.value is UpdateUiState.Available)
    }

    @Test
    fun `update without apk asset still offers the release`() = runTest {
        coEvery { repository.checkForUpdate(BuildConfig.VERSION_NAME) } returns Result.success(update("v1.3.0", withAsset = false))

        val vm = viewModel()
        vm.check()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is UpdateUiState.Available)
        assertTrue((state as UpdateUiState.Available).update.asset == null)
    }
}
