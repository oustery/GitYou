package com.gityou.app.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import com.gityou.app.R
import com.gityou.app.data.remote.dto.Notification
import com.gityou.app.data.repo.NotificationsRepository
import com.gityou.app.data.store.TokenStore
import com.gityou.app.data.util.ApiErrors
import com.gityou.app.data.util.safeCall
import com.gityou.app.ui.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationsUi(
    val items: List<Notification> = emptyList(),
    val nextPage: Int? = 1,
    val loadingMore: Boolean = false,
    // Turns the pagination footer into a retry button after a failure (F-013).
    val loadMoreFailed: Boolean = false,
) {
    val hasMore: Boolean get() = nextPage != null
    val unreadCount: Int get() = items.count { it.unread }
}

/** One-shot snackbar content: either a success string or a mapped error key (F-016). */
sealed interface UiMessage {
    data class Res(@StringRes val id: Int) : UiMessage
    data class Error(val errorKey: String) : UiMessage
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
    tokenStore: TokenStore,
) : ViewModel() {

    /** User preference: opening a notification marks it as read. */
    val markReadOnOpen: StateFlow<Boolean> = tokenStore.settingsFlow
        .map { it.markReadOnOpen }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _state = MutableStateFlow<UiState<NotificationsUi>>(UiState.Loading)
    val state: StateFlow<UiState<NotificationsUi>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    init {
        refresh()
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            _state.value = safeCall { notificationsRepository.notifications(page = 1) }.fold(
                onSuccess = { page ->
                    UiState.Content(NotificationsUi(items = page.items, nextPage = page.nextPage))
                },
                onFailure = { t -> UiState.Error(ApiErrors.keyOf(t)) },
            )
            _refreshing.value = false
        }
    }

    fun loadMore() {
        val current = (_state.value as? UiState.Content)?.value ?: return
        if (current.loadingMore || current.nextPage == null) return
        val nextPage = current.nextPage ?: return
        viewModelScope.launch {
            _state.value = UiState.Content(current.copy(loadingMore = true))
            val page = safeCall { notificationsRepository.notifications(page = nextPage) }
            // Merge only while the request we started is still current; a
            // refresh that completed meanwhile must not be rolled back (F-006).
            val latest = (_state.value as? UiState.Content)?.value
            if (latest == null || !latest.loadingMore) return@launch
            _state.value = page.fold(
                onSuccess = { p ->
                    UiState.Content(
                        latest.copy(
                            items = latest.items + p.items,
                            nextPage = p.nextPage,
                            loadingMore = false,
                            loadMoreFailed = false,
                        ),
                    )
                },
                onFailure = { _ ->
                    UiState.Content(latest.copy(loadingMore = false, loadMoreFailed = true))
                },
            )
        }
    }

    fun markRead(notification: Notification) {
        viewModelScope.launch {
            safeCall { notificationsRepository.markThreadRead(notification.id) }.fold(
                onSuccess = { updateItem(notification.id, unread = false) },
                onFailure = { t -> _message.value = UiMessage.Error(ApiErrors.keyOf(t)) },
            )
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            safeCall { notificationsRepository.markAllRead() }.fold(
                onSuccess = {
                    (_state.value as? UiState.Content)?.let { content ->
                        _state.value = UiState.Content(
                            content.value.copy(
                                items = content.value.items.map { it.copy(unread = false) },
                            ),
                        )
                    }
                    _message.value = UiMessage.Res(R.string.notifications_marked_all)
                },
                onFailure = { t -> _message.value = UiMessage.Error(ApiErrors.keyOf(t)) },
            )
        }
    }

    private fun updateItem(id: String, unread: Boolean) {
        (_state.value as? UiState.Content)?.let { content ->
            _state.value = UiState.Content(
                content.value.copy(items = content.value.items.map {
                    if (it.id == id) it.copy(unread = unread) else it
                }),
            )
        }
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
