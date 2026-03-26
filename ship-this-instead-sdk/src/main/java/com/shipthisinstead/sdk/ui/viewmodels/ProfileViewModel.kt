package com.shipthisinstead.sdk.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shipthisinstead.sdk.api.STIApiClient
import com.shipthisinstead.sdk.models.STIUserActivity
import com.shipthisinstead.sdk.models.STIUserProfile
import com.shipthisinstead.sdk.`public`.ShipThisInsteadSDK
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: STIUserProfile? = null,
    val activity: STIUserActivity? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class ProfileViewModel(private val apiClient: STIApiClient) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun load(userId: String) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState(isLoading = true)
            try {
                val profileDeferred = async { apiClient.call { getUserProfile(userId) } }
                val activityDeferred = async { apiClient.call { getUserActivity(userId) } }
                _uiState.value = ProfileUiState(
                    profile = profileDeferred.await(),
                    activity = activityDeferred.await(),
                )
            } catch (e: Exception) {
                _uiState.value = ProfileUiState(error = e.message)
            }
        }
    }

    companion object {
        fun factory(sdk: ShipThisInsteadSDK): ViewModelProvider.Factory = viewModelFactory {
            initializer { ProfileViewModel(sdk.apiClient) }
        }
    }
}
