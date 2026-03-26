package com.shipthisinstead.sdk.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shipthisinstead.sdk.api.STIApiClient
import com.shipthisinstead.sdk.auth.AuthManager
import com.shipthisinstead.sdk.auth.AuthState
import com.shipthisinstead.sdk.models.STIBoard
import com.shipthisinstead.sdk.models.STIBranding
import com.shipthisinstead.sdk.models.STIFeedbackItem
import com.shipthisinstead.sdk.`public`.ShipThisInsteadSDK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class FeedbackListUiState(
    val items: List<STIFeedbackItem> = emptyList(),
    val boards: List<STIBoard> = emptyList(),
    val branding: STIBranding? = null,
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
) {
    val hasMore: Boolean get() = currentPage < totalPages
}

class FeedbackListViewModel(
    private val apiClient: STIApiClient,
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
                )
                if (state is AuthState.Authenticated || state is AuthState.Guest) {
                    if (_uiState.value.items.isEmpty() && !_uiState.value.isLoading) {
                        loadInitial()
                    }
                }
            }
        }
        loadBoards()
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
            } catch (_: Exception) {}
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
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
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
                    count = item.count?.copy(votes = voteCount) ?: com.shipthisinstead.sdk.models.STIFeedbackCount(votes = voteCount)
                ) else item
            }
        )
    }

    companion object {
        fun factory(sdk: ShipThisInsteadSDK): ViewModelProvider.Factory = viewModelFactory {
            initializer { FeedbackListViewModel(sdk.apiClient, sdk.authManager) }
        }
    }
}
