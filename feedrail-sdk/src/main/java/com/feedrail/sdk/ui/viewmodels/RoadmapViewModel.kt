package com.feedrail.sdk.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.feedrail.sdk.api.FeedRailApiClient
import com.feedrail.sdk.models.FeedRailStatus
import com.feedrail.sdk.`public`.FeedRailSDK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RoadmapUiState(
    val statuses: List<FeedRailStatus> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class RoadmapViewModel(private val apiClient: FeedRailApiClient) : ViewModel() {

    private val _uiState = MutableStateFlow(RoadmapUiState())
    val uiState: StateFlow<RoadmapUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = RoadmapUiState(isLoading = true)
            try {
                val statuses = apiClient.call { getRoadmap() }
                _uiState.value = RoadmapUiState(statuses = statuses)
            } catch (e: Exception) {
                _uiState.value = RoadmapUiState(error = e.message)
            }
        }
    }

    companion object {
        fun factory(sdk: FeedRailSDK): ViewModelProvider.Factory = viewModelFactory {
            initializer { RoadmapViewModel(sdk.apiClient) }
        }
    }
}
