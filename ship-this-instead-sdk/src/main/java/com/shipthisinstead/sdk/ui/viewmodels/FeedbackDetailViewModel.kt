package com.shipthisinstead.sdk.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shipthisinstead.sdk.api.STIApiClient
import com.shipthisinstead.sdk.auth.AuthManager
import com.shipthisinstead.sdk.auth.AuthState
import com.shipthisinstead.sdk.models.STIFeedbackItem
import com.shipthisinstead.sdk.`public`.ShipThisInsteadSDK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class FeedbackDetailUiState(
    val feedback: STIFeedbackItem? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isVoting: Boolean = false,
    val isSubmittingComment: Boolean = false,
    val commentText: String = "",
    val isAuthenticated: Boolean = false,
    val isAuthenticating: Boolean = false,
)

class FeedbackDetailViewModel(
    private val apiClient: STIApiClient,
    private val authManager: AuthManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedbackDetailUiState())
    val uiState: StateFlow<FeedbackDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authManager.authState.collectLatest { state ->
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = state is AuthState.Authenticated,
                    isAuthenticating = state is AuthState.Authenticating,
                )
            }
        }
    }

    fun load(feedbackId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val item = apiClient.call { getFeedbackDetail(feedbackId) }
                _uiState.value = _uiState.value.copy(feedback = item, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun vote() {
        val feedbackId = _uiState.value.feedback?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVoting = true)
            try {
                val response = apiClient.call { voteFeedback(feedbackId) }
                _uiState.value = _uiState.value.copy(
                    isVoting = false,
                    feedback = _uiState.value.feedback?.copy(
                        hasVoted = response.voted,
                        count = _uiState.value.feedback?.count?.copy(votes = response.voteCount)
                            ?: com.shipthisinstead.sdk.models.STIFeedbackCount(votes = response.voteCount)
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isVoting = false)
            }
        }
    }

    fun updateCommentText(text: String) {
        _uiState.value = _uiState.value.copy(commentText = text)
    }

    fun submitComment(
        attachments: List<com.shipthisinstead.sdk.models.AttachmentInput> = emptyList(),
    ) {
        val feedbackId = _uiState.value.feedback?.id ?: return
        val text = _uiState.value.commentText.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingComment = true)
            try {
                val comment = apiClient.call {
                    addComment(feedbackId, com.shipthisinstead.sdk.models.AddCommentBody(content = text, attachments = attachments))
                }
                val updated = _uiState.value.feedback?.let { fb ->
                    fb.copy(comments = (fb.comments ?: emptyList()) + comment)
                }
                _uiState.value = _uiState.value.copy(
                    feedback = updated,
                    commentText = "",
                    isSubmittingComment = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmittingComment = false)
            }
        }
    }

    companion object {
        fun factory(sdk: ShipThisInsteadSDK): ViewModelProvider.Factory = viewModelFactory {
            initializer { FeedbackDetailViewModel(sdk.apiClient, sdk.authManager) }
        }
    }
}
