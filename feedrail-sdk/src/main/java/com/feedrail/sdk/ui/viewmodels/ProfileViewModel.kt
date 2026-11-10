package com.feedrail.sdk.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.feedrail.sdk.api.FeedRailApiClient
import com.feedrail.sdk.auth.AuthManager
import com.feedrail.sdk.auth.AuthState
import com.feedrail.sdk.models.FeedRailBlockedUserEntry
import com.feedrail.sdk.models.FeedRailUserActivity
import com.feedrail.sdk.models.FeedRailUserProfile
import com.feedrail.sdk.moderation.Moderation
import com.feedrail.sdk.moderation.ModerationEvent
import com.feedrail.sdk.moderation.isByEndUser
import com.feedrail.sdk.moderation.withoutAuthor
import com.feedrail.sdk.`public`.FeedRailSDK
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: FeedRailUserProfile? = null,
    val activity: FeedRailUserActivity? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Who the reader has blocked. Only loaded on their own profile. */
    val blockedUsers: List<FeedRailBlockedUserEntry> = emptyList(),
    val isOwnProfile: Boolean = false,
)

class ProfileViewModel(
    private val apiClient: FeedRailApiClient,
    private val authManager: AuthManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            Moderation.events.collect(::onModerationEvent)
        }
    }

    fun load(userId: String) {
        val isOwn = (authManager.authState.value as? AuthState.Authenticated)?.user?.id == userId
        viewModelScope.launch {
            _uiState.value = ProfileUiState(isLoading = true, isOwnProfile = isOwn)
            try {
                val profileDeferred = async { apiClient.call { getUserProfile(userId) } }
                val activityDeferred = async { apiClient.call { getUserActivity(userId) } }
                _uiState.value = ProfileUiState(
                    profile = profileDeferred.await(),
                    activity = activityDeferred.await(),
                    isOwnProfile = isOwn,
                )
            } catch (e: Exception) {
                _uiState.value = ProfileUiState(error = e.message, isOwnProfile = isOwn)
                return@launch
            }
            if (isOwn) loadBlockedUsers()
        }
    }

    /** A failure here leaves the section empty rather than failing the whole profile. */
    private suspend fun loadBlockedUsers() {
        try {
            val blocks = apiClient.call { getBlockedUsers() }
            _uiState.value = _uiState.value.copy(blockedUsers = blocks)
        } catch (_: Exception) {
        }
    }

    private fun onModerationEvent(event: ModerationEvent) {
        val state = _uiState.value
        when (event) {
            is ModerationEvent.UserUnblocked -> _uiState.value = state.copy(
                blockedUsers = state.blockedUsers.filter { it.blockedUser.id != event.userId },
            )
            is ModerationEvent.UserBlocked -> {
                // The activity lists hold the profile owner's content, plus replies
                // under it, so this rarely removes anything — but it must not show
                // a blocked author either way.
                val activity = state.activity
                _uiState.value = state.copy(
                    activity = activity?.copy(
                        feedback = activity.feedback.filter { !it.isByEndUser(event.userId) },
                        comments = activity.comments.withoutAuthor(event.userId),
                    ),
                )
                if (state.isOwnProfile) viewModelScope.launch { loadBlockedUsers() }
            }
            is ModerationEvent.FeedbackReported -> state.activity?.let { a ->
                _uiState.value = state.copy(activity = a.copy(feedback = a.feedback.filter { it.id != event.feedbackId }))
            }
            is ModerationEvent.CommentReported -> state.activity?.let { a ->
                _uiState.value = state.copy(activity = a.copy(comments = a.comments.filter { it.id != event.commentId }))
            }
        }
    }

    companion object {
        fun factory(sdk: FeedRailSDK): ViewModelProvider.Factory = viewModelFactory {
            initializer { ProfileViewModel(sdk.apiClient, sdk.authManager) }
        }
    }
}
