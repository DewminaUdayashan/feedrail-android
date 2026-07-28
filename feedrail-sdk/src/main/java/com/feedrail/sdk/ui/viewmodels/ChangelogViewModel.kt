package com.feedrail.sdk.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.feedrail.sdk.api.FeedRailApiClient
import com.feedrail.sdk.models.FeedRailReleaseNote
import com.feedrail.sdk.`public`.FeedRailSDK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChangelogUiState(
    val notes: List<FeedRailReleaseNote> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
) {
    val hasMore: Boolean get() = currentPage < totalPages
}

class ChangelogViewModel(private val apiClient: FeedRailApiClient) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangelogUiState())
    val uiState: StateFlow<ChangelogUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = apiClient.call { getChangelog(page = 1, limit = 10) }
                _uiState.value = _uiState.value.copy(
                    notes = response.items.filter { it.isPublished },
                    currentPage = response.meta.page,
                    totalPages = response.meta.totalPages,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadMore() {
        val s = _uiState.value
        if (!s.hasMore || s.isLoadingMore) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            try {
                val response = apiClient.call { getChangelog(page = s.currentPage + 1, limit = 10) }
                _uiState.value = _uiState.value.copy(
                    notes = s.notes + response.items.filter { it.isPublished },
                    currentPage = response.meta.page,
                    totalPages = response.meta.totalPages,
                    isLoadingMore = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    companion object {
        fun factory(sdk: FeedRailSDK): ViewModelProvider.Factory = viewModelFactory {
            initializer { ChangelogViewModel(sdk.apiClient) }
        }
    }
}
