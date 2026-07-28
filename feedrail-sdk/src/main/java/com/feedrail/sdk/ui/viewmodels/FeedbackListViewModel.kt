package com.feedrail.sdk.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.feedrail.sdk.api.FeedRailApiClient
import com.feedrail.sdk.auth.AuthManager
import com.feedrail.sdk.auth.AuthState
import com.feedrail.sdk.models.FeedRailBoard
import com.feedrail.sdk.models.FeedRailBranding
import com.feedrail.sdk.models.FeedRailFeedbackItem
import com.feedrail.sdk.`public`.FeedRailSDK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class FeedbackListUiState(
    val items: List<FeedRailFeedbackItem> = emptyList(),
    val boards: List<FeedRailBoard> = emptyList(),
    val branding: FeedRailBranding? = null,
    val isProjectArchived: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val selectedBoardSlug: String? = null,
    val sortOption: String = "newest",
    val isAuthenticated: Boolean = false,
    val isAuthenticating: Boolean = false,
    val isGuest: Boolean = true,
    /** False until the first feedback load finishes — lets the UI show the
     * skeleton from the first frame instead of flashing the empty state. */
    val hasLoadedOnce: Boolean = false,
) {
    val hasMore: Boolean get() = currentPage < totalPages

    /** Whether the current user may submit feedback. Identified users always
     * can; guests/anonymous only when the plan allows guest posting. */
    val canSubmit: Boolean
        get() = isAuthenticated && (!isGuest || (branding?.guestPostingAllowed ?: false))
}

class FeedbackListViewModel(
    private val apiClient: FeedRailApiClient,
    private val authManager: AuthManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedbackListUiState())
    val uiState: StateFlow<FeedbackListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authManager.authState.collectLatest { state ->
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = state is AuthState.Authenticated,
                    isAuthenticating = state is AuthState.Authenticating,
                    isGuest = (state as? AuthState.Authenticated)?.user?.isGuestUser ?: true,
                )
                if (state is AuthState.Authenticated || state is AuthState.Guest) {
                    // Load boards (branding + board list) once auth resolves so we have a token
                    if (_uiState.value.boards.isEmpty()) {
                        loadBoards()
                    }
                    if (_uiState.value.items.isEmpty() && !_uiState.value.isLoading) {
                        loadInitial()
                    }
                }
            }
        }
    }

    private fun loadBoards() {
        viewModelScope.launch {
            try {
                val response = apiClient.call { getBoards() }
                _uiState.value = _uiState.value.copy(
                    boards = response.boards,
                    branding = response.branding,
                    isProjectArchived = response.isProjectArchived ?: false,
                )
            } catch (e: Exception) {
                Log.e("FeedRail", "loadBoards failed: ${e.message}", e)
            }
        }
    }

    private fun loadInitial() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val s = _uiState.value
                val response = apiClient.call {
                    getFeedback(
                        page = 1,
                        limit = 20,
                        boardSlugs = s.selectedBoardSlug,
                        sort = s.sortOption,
                    )
                }
                _uiState.value = _uiState.value.copy(
                    items = response.items,
                    currentPage = response.meta.page,
                    totalPages = response.meta.totalPages,
                    isLoading = false,
                    hasLoadedOnce = true,
                )
            } catch (e: Exception) {
                Log.e("FeedRail", "loadInitial failed: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasLoadedOnce = true,
                    error = e.message ?: "Unknown error",
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            try {
                val s = _uiState.value
                val response = apiClient.call {
                    getFeedback(page = 1, limit = 20, boardSlugs = s.selectedBoardSlug, sort = s.sortOption)
                }
                _uiState.value = _uiState.value.copy(
                    items = response.items,
                    currentPage = response.meta.page,
                    totalPages = response.meta.totalPages,
                    isRefreshing = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRefreshing = false, error = e.message)
            }
        }
    }

    fun loadMore() {
        val s = _uiState.value
        if (!s.hasMore || s.isLoadingMore) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            try {
                val response = apiClient.call {
                    getFeedback(
                        page = s.currentPage + 1,
                        limit = 20,
                        boardSlugs = s.selectedBoardSlug,
                        sort = s.sortOption,
                    )
                }
                _uiState.value = _uiState.value.copy(
                    items = s.items + response.items,
                    currentPage = response.meta.page,
                    totalPages = response.meta.totalPages,
                    isLoadingMore = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    fun selectBoard(slug: String?) {
        _uiState.value = _uiState.value.copy(selectedBoardSlug = slug, items = emptyList(), currentPage = 1)
        loadInitial()
    }

    fun selectSort(sort: String) {
        _uiState.value = _uiState.value.copy(sortOption = sort, items = emptyList(), currentPage = 1)
        loadInitial()
    }

    fun updateItemVote(itemId: String, voted: Boolean, voteCount: Int) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map { item ->
                if (item.id == itemId) item.copy(
                    hasVoted = voted,
                    count = item.count?.copy(votes = voteCount) ?: com.feedrail.sdk.models.FeedRailFeedbackCount(votes = voteCount)
                ) else item
            }
        )
    }

    companion object {
        fun factory(sdk: FeedRailSDK): ViewModelProvider.Factory = viewModelFactory {
            initializer { FeedbackListViewModel(sdk.apiClient, sdk.authManager) }
        }
    }
}
