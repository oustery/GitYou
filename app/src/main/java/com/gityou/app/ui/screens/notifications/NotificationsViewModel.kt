package com.gityou.app.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.remote.dto.Notification
import com.gityou.app.data.repo.NotificationsRepository
import com.gityou.app.data.util.ApiErrors
import com.gityou.app.ui.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationsUi(
    val items: List<Notification> = emptyList(),
    val nextPage: Int? = 1,
    val loadingMore: Boolean = false,
) {
    val hasMore: Boolean get() = nextPage != null
    val unreadCount: Int get() = items.count { it.unread }
}

/** Where a notification should take the user. */
data class NotificationTarget(
    val owner: String,
    val repo: String,
    val number: Int,
    val isPull: Boolean,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<NotificationsUi>>(UiState.Loading)
    val state: StateFlow<UiState<NotificationsUi>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _message = MutableStateFlow<Int?>(null)
    val message: StateFlow<Int?> = _message.asStateFlow()

    init {
        refresh()
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            _state.value = try {
                val page = notificationsRepository.notifications(page = 1)
                UiState.Content(NotificationsUi(items = page.items, nextPage = page.nextPage))
            } catch (t: Throwable) {
                UiState.Error(ApiErrors.keyOf(t))
            }
            _refreshing.value = false
        }
    }

    fun loadMore() {
        val current = (_state.value as? UiState.Content)?.value ?: return
        if (current.loadingMore || current.nextPage == null) return
        viewModelScope.launch {
            _state.value = UiState.Content(current.copy(loadingMore = true))
            _state.value = try {
                val page = notificationsRepository.notifications(page = current.nextPage)
                UiState.Content(
                    current.copy(items = current.items + page.items, nextPage = page.nextPage, loadingMore = false),
                )
            } catch (t: Throwable) {
                UiState.Content(current.copy(loadingMore = false))
            }
        }
    }

    fun markRead(notification: Notification) {
        viewModelScope.launch {
            try {
                notificationsRepository.markThreadRead(notification.id)
                updateItem(notification.id, unread = false)
            } catch (t: Throwable) {
                _message.value = R_string_error_key(ApiErrors.keyOf(t))
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            try {
                notificationsRepository.markAllRead()
                _state.value = (_state.value as? UiState.Content)?.let { content ->
                    UiState.Content(
                        content.value.copy(items = content.value.items.map { it.copy(unread = false) }),
                    )
                } ?: _state.value
                _message.value = com.gityou.app.R.string.notifications_marked_all
            } catch (t: Throwable) {
                _message.value = R_string_error_key(ApiErrors.keyOf(t))
            }
        }
    }

    private fun R_string_error_key(errorKey: String): Int = when (errorKey) {
        ApiErrors.KEY_UNAUTHORIZED -> com.gityou.app.R.string.error_unauthorized
        ApiErrors.KEY_RATE_LIMIT -> com.gityou.app.R.string.error_rate_limit
        ApiErrors.KEY_NETWORK -> com.gityou.app.R.string.error_network
        else -> com.gityou.app.R.string.error_generic_action
    }

    private fun updateItem(id: String, unread: Boolean) {
        _state.value = (_state.value as? UiState.Content)?.let { content ->
            UiState.Content(
                content.value.copy(items = content.value.items.map {
                    if (it.id == id) it.copy(unread = unread) else it
                }),
            )
        } ?: _state.value
    }

    fun targetFor(notification: Notification): NotificationTarget? {
        val url = notification.subject?.url ?: return null
        val segments = url.split('/')
        if (segments.size < 8) return null
        val owner = segments.getOrNull(4) ?: return null
        val repo = segments.getOrNull(5) ?: return null
        val number = segments.lastOrNull()?.toIntOrNull() ?: return null
        return NotificationTarget(
            owner = owner,
            repo = repo,
            number = number,
            isPull = notification.subject?.type == "PullRequest",
        )
    }
}
