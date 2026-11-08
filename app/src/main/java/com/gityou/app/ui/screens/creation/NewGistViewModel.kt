package com.gityou.app.ui.screens.creation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.repo.CreationRepository
import com.gityou.app.data.util.safeCall
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NewGistUi(
    val description: String = "",
    val filename: String = "gist.md",
    val content: String = "",
    val public: Boolean = false,
    val submitting: Boolean = false,
    val failed: Boolean = false,
)

/** Creates a single-file gist through `POST /gists`. */
@HiltViewModel
class NewGistViewModel @Inject constructor(
    private val creationRepository: CreationRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(NewGistUi())
    val ui: StateFlow<NewGistUi> = _ui.asStateFlow()

    fun patch(update: (NewGistUi) -> NewGistUi) {
        _ui.value = update(_ui.value)
    }

    fun submit(onCreated: (String) -> Unit) {
        val current = _ui.value
        // Double-submit guard: two rapid taps both read submitting=false
        // before the recomposition disables the button — guard in the VM.
        if (current.submitting) return
        val filename = current.filename.trim().ifBlank { "gist.md" }
        if (current.content.isBlank()) {
            _ui.value = current.copy(failed = true)
            return
        }
        viewModelScope.launch {
            _ui.value = current.copy(submitting = true, failed = false)
            val result = safeCall {
                creationRepository.createGist(
                    description = current.description,
                    public = current.public,
                    filename = filename,
                    content = current.content,
                )
            }
            result.fold(
                onSuccess = { gist -> onCreated(gist.htmlUrl.orEmpty()) },
                onFailure = { _ -> _ui.value = _ui.value.copy(submitting = false, failed = true) },
            )
        }
    }
}
