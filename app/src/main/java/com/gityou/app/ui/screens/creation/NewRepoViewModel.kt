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

data class NewRepoUi(
    val name: String = "",
    val description: String = "",
    val private: Boolean = false,
    val autoInit: Boolean = true,
    val submitting: Boolean = false,
    val nameMissing: Boolean = false,
    val failed: Boolean = false,
)

@HiltViewModel
class NewRepoViewModel @Inject constructor(
    private val creationRepository: CreationRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(NewRepoUi())
    val ui: StateFlow<NewRepoUi> = _ui.asStateFlow()

    fun patch(update: (NewRepoUi) -> NewRepoUi) {
        _ui.value = update(_ui.value)
    }

    fun submit(onCreated: (String) -> Unit) {
        val current = _ui.value
        // Double-submit guard: two rapid taps both read submitting=false
        // before the recomposition disables the button — guard in the VM.
        if (current.submitting) return
        val name = current.name.trim()
        if (name.isEmpty()) {
            _ui.value = current.copy(nameMissing = true)
            return
        }
        viewModelScope.launch {
            _ui.value = current.copy(submitting = true, failed = false)
            val result = safeCall {
                creationRepository.createRepo(
                    name = name,
                    description = current.description,
                    private = current.private,
                    autoInit = current.autoInit,
                )
            }
            result.fold(
                onSuccess = { repo -> onCreated(repo.fullName ?: repo.name) },
                onFailure = { _ -> _ui.value = _ui.value.copy(submitting = false, failed = true) },
            )
        }
    }
}
