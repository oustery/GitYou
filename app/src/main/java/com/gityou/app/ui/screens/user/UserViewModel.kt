package com.gityou.app.ui.screens.user

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gityou.app.data.remote.dto.Page
import com.gityou.app.data.remote.dto.Repo
import com.gityou.app.data.remote.dto.User
import com.gityou.app.data.repo.UsersRepository
import com.gityou.app.data.util.ApiErrors
import com.gityou.app.data.util.safeCall
import com.gityou.app.ui.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The four browsable lists of a profile. */
enum class UserTab { REPOS, STARS, FOLLOWERS, FOLLOWING }

/** One paginated list; items are Repo or User depending on [UserTab]. */
data class UserListState(
    val items: List<Any> = emptyList(),
    val nextPage: Int? = 1,
    val loading: Boolean = false,
    val loadMoreFailed: Boolean = false,
)

data class UserUi(
    val user: User,
    val selectedTab: UserTab = UserTab.REPOS,
    val lists: Map<UserTab, UserListState> = emptyMap(),
) {
    fun list(tab: UserTab): UserListState = lists[tab] ?: UserListState()
}

/**
 * Public profile of any GitHub user. The profile body loads eagerly; the
 * four lists load on first visit of their tab and paginate independently.
 */
@HiltViewModel
class UserViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val usersRepository: UsersRepository,
) : ViewModel() {

    val login: String = savedStateHandle.get<String>("login").orEmpty()

    private val _state = MutableStateFlow<UiState<UserUi>>(UiState.Loading)
    val state: StateFlow<UiState<UserUi>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = safeCall { usersRepository.profile(login) }.fold(
                onSuccess = { user -> UiState.Content(UserUi(user = user)) },
                onFailure = { t -> UiState.Error(ApiErrors.keyOf(t)) },
            )
            if (_state.value !is UiState.Content) return@launch

            // Repositories matter most; the star list is a cheap second call.
            // Follower lists load lazily, on the first visit of their tab.
            val reposDeferred = async { safeCall { usersRepository.repos(login, page = 1) } }
            val starsDeferred = async { safeCall { usersRepository.stars(login, page = 1) } }

            (_state.value as? UiState.Content)?.let { content ->
                var ui = content.value
                reposDeferred.await().getOrNull()?.let { page ->
                    ui = ui.withList(UserTab.REPOS, page)
                }
                starsDeferred.await().getOrNull()?.let { page ->
                    ui = ui.withList(UserTab.STARS, page)
                }
                _state.value = UiState.Content(ui)
            }
        }
    }

    fun selectTab(tab: UserTab) {
        update { it.copy(selectedTab = tab) }
        when (tab) {
            UserTab.FOLLOWERS -> ensureLoaded(UserTab.FOLLOWERS) { page ->
                usersRepository.followers(login, page = page)
            }
            UserTab.FOLLOWING -> ensureLoaded(UserTab.FOLLOWING) { page ->
                usersRepository.following(login, page = page)
            }
            else -> Unit
        }
    }

    fun loadMore() {
        val current = (_state.value as? UiState.Content)?.value ?: return
        val tab = current.selectedTab
        val list = current.list(tab)
        if (list.loading) return
        val page = list.nextPage ?: return

        setList(tab, list.copy(loading = true))
        viewModelScope.launch {
            val result = safeCall { fetchPage(tab, page) }
            val latest = (_state.value as? UiState.Content)?.value ?: return@launch
            val latestList = latest.list(tab)
            setList(
                tab,
                result.fold(
                    onSuccess = { p ->
                        UserListState(
                            items = latestList.items + p.items,
                            nextPage = p.nextPage,
                            loadMoreFailed = false,
                        )
                    },
                    onFailure = { _ -> latestList.copy(loading = false, loadMoreFailed = true) },
                ),
            )
        }
    }

    private suspend fun fetchPage(tab: UserTab, page: Int): Page<Any> {
        @Suppress("UNCHECKED_CAST")
        return when (tab) {
            UserTab.REPOS -> usersRepository.repos(login, page = page) as Page<Any>
            UserTab.STARS -> usersRepository.stars(login, page = page) as Page<Any>
            UserTab.FOLLOWERS -> usersRepository.followers(login, page = page) as Page<Any>
            UserTab.FOLLOWING -> usersRepository.following(login, page = page) as Page<Any>
        }
    }

    /** Lazily loads the first page of a tab that has not been fetched yet. */
    private fun ensureLoaded(tab: UserTab, fetch: suspend (Int) -> Page<User>) {
        val current = (_state.value as? UiState.Content)?.value ?: return
        val list = current.list(tab)
        // Already fetched, exhausted, or a request in flight — do nothing.
        if (list.items.isNotEmpty() || list.nextPage == null || list.loading) return

        setList(tab, list.copy(loading = true))
        viewModelScope.launch {
            val result = safeCall { fetch(1) }
            val latest = (_state.value as? UiState.Content)?.value ?: return@launch
            setList(
                tab,
                result.fold(
                    onSuccess = { p ->
                        UserListState(items = p.items, nextPage = p.nextPage)
                    },
                    onFailure = { _ ->
                        latest.list(tab).copy(loading = false, loadMoreFailed = true)
                    },
                ),
            )
        }
    }

    private fun UserUi.withList(tab: UserTab, page: Page<*>): UserUi {
        @Suppress("UNCHECKED_CAST")
        val items = page.items as List<Any>
        return copy(lists = lists + (tab to UserListState(items = items, nextPage = page.nextPage)))
    }

    private fun setList(tab: UserTab, value: UserListState) {
        update { it.copy(lists = it.lists + (tab to value)) }
    }

    private inline fun update(block: (UserUi) -> UserUi) {
        (_state.value as? UiState.Content)?.let { content ->
            _state.value = UiState.Content(block(content.value))
        }
    }
}
