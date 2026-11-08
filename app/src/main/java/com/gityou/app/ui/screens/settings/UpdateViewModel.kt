package com.gityou.app.ui.screens.settings

import com.gityou.app.BuildConfig
import com.gityou.app.data.repo.UpdateRepository
import com.gityou.app.data.update.UpdateInfo
import com.gityou.app.data.util.ApiErrors
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

/** State machine of the in-app update flow (Settings → About → Check for updates). */
sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState

    /** The running build is the newest release. */
    data object UpToDate : UpdateUiState

    /** A newer release exists; [UpdateInfo.asset] may be null (no downloadable APK). */
    data class Available(val update: UpdateInfo) : UpdateUiState
    data class Downloading(val update: UpdateInfo, val downloadedBytes: Long, val totalBytes: Long) : UpdateUiState
    data class Ready(val update: UpdateInfo, val file: File) : UpdateUiState

    /** Transient: the UI shows a snackbar and returns to [Idle]. */
    data class Failed(val errorKey: String) : UpdateUiState
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val repository: UpdateRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var downloadJob: Job? = null

    init {
        // APKs left in cache by a previous session are stale: they were never
        // installed (or the install failed), so re-downloading is the safe path.
        repository.clearDownloads()
    }

    /** Compares the running version against the newest GitHub release. */
    fun check() {
        // Checking: guard against double taps. Downloading: a stray check() must
        // not clobber the in-flight download state (N-004).
        if (_state.value is UpdateUiState.Checking || _state.value is UpdateUiState.Downloading) return
        viewModelScope.launch {
            _state.value = UpdateUiState.Checking
            _state.value = repository.checkForUpdate(BuildConfig.VERSION_NAME).fold(
                onSuccess = { update ->
                    if (update == null) UpdateUiState.UpToDate else UpdateUiState.Available(update)
                },
                onFailure = { t -> UpdateUiState.Failed(ApiErrors.keyOf(t)) },
            )
        }
    }

    /** Streams the release APK into cache; progress is published into [state]. */
    fun download(update: UpdateInfo) {
        val asset = update.asset ?: return
        downloadJob?.cancel()
        _state.value = UpdateUiState.Downloading(update, 0L, asset.size)
        downloadJob = viewModelScope.launch {
            repository.downloadApk(asset) { downloaded, total ->
                // Ignore late progress chunks after the download was cancelled:
                // cancelDownload() already moved the state back to Available.
                if (_state.value is UpdateUiState.Downloading) {
                    _state.value = UpdateUiState.Downloading(update, downloaded, total)
                }
            }.fold(
                onSuccess = { file -> _state.value = UpdateUiState.Ready(update, file) },
                onFailure = { t -> _state.value = UpdateUiState.Failed(ApiErrors.keyOf(t)) },
            )
        }
    }

    /** Aborts an in-flight download (if any) and closes the flow back to Idle. */
    fun dismiss() {
        downloadJob?.cancel()
        downloadJob = null
        _state.value = UpdateUiState.Idle
    }

    /** Keeps the dialog open after a cancelled download (back to the offer). */
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        val current = _state.value
        if (current is UpdateUiState.Downloading) {
            _state.value = UpdateUiState.Available(current.update)
        }
    }
}
