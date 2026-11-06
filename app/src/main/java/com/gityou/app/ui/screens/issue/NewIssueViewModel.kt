package com.gityou.app.ui.screens.issue

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.repo.IssuesRepository
import com.gityou.app.data.util.ApiErrors
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NewIssuePhase {
    data object Idle : NewIssuePhase
    data object Submitting : NewIssuePhase
    data class Error(val errorKey: String) : NewIssuePhase
}

data class NewIssueState(
    val phase: NewIssuePhase = NewIssuePhase.Idle,
    val createdNumber: Int? = null,
)

@HiltViewModel
class NewIssueViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val issuesRepository: IssuesRepository,
) : ViewModel() {

    val owner: String = savedStateHandle.get<String>("owner").orEmpty()
    val repoName: String = savedStateHandle.get<String>("name").orEmpty()

    private val _state = MutableStateFlow(NewIssueState())
    val state: StateFlow<NewIssueState> = _state.asStateFlow()

    fun createIssue(title: String, body: String) {
        if (title.isBlank() || _state.value.phase == NewIssuePhase.Submitting) return
        viewModelScope.launch {
            _state.value = NewIssueState(phase = NewIssuePhase.Submitting)
            try {
                val issue = issuesRepository.createIssue(owner, repoName, title.trim(), body.trim().ifBlank { null })
                _state.value = NewIssueState(phase = NewIssuePhase.Idle, createdNumber = issue.number)
            } catch (t: Throwable) {
                _state.value = NewIssueState(phase = NewIssuePhase.Error(ApiErrors.keyOf(t)))
            }
        }
    }
}
