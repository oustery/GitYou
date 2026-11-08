package com.gityou.app.ui.screens.commits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.remote.dto.CommitFile
import com.gityou.app.data.repo.ReposRepository
import com.gityou.app.data.util.ApiErrors
import com.gityou.app.data.util.safeCall
import com.gityou.app.ui.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Shared state: the diff of one commit or of all pull-request files. */
data class DiffUi(
    val title: String,
    val subtitle: String? = null,
    val files: List<CommitFile> = emptyList(),
)

/**
 * Serves both entry points: a single commit (`key` = sha) and the changed
 * files of a pull request (`key` = number, [isPull] = true).
 */
@HiltViewModel
class DiffViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reposRepository: ReposRepository,
) : ViewModel() {

    val owner: String = savedStateHandle.get<String>("owner").orEmpty()
    val repoName: String = savedStateHandle.get<String>("name").orEmpty()
    val isPull: Boolean = savedStateHandle.get<Boolean>("isPull") ?: false
    private val sha: String = savedStateHandle.get<String>("sha").orEmpty()
    private val number: Int = savedStateHandle.get<Int>("number") ?: 0

    private val _state = MutableStateFlow<UiState<DiffUi>>(UiState.Loading)
    val state: StateFlow<UiState<DiffUi>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val result = safeCall {
                if (isPull) {
                    DiffUi(
                        title = "#$number",
                        subtitle = null,
                        files = reposRepository.pullFiles(owner, repoName, number),
                    )
                } else {
                    reposRepository.commit(owner, repoName, sha).let { commit ->
                        DiffUi(
                            title = commit.firstLine,
                            subtitle = commit.author?.login
                                ?: commit.commit?.author?.name.orEmpty(),
                            files = commit.files,
                        )
                    }
                }
            }
            _state.value = result.fold(
                onSuccess = { UiState.Content(it) },
                onFailure = { t -> UiState.Error(ApiErrors.keyOf(t)) },
            )
        }
    }
}
