package com.feedrail.sdk.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.feedrail.sdk.api.FeedRailApiClient
import com.feedrail.sdk.auth.AuthManager
import com.feedrail.sdk.auth.AuthState
import com.feedrail.sdk.models.AddCommentBody
import com.feedrail.sdk.models.AttachmentInput
import com.feedrail.sdk.models.FeedRailAttachment
import com.feedrail.sdk.models.FeedRailFeedbackItem
import com.feedrail.sdk.models.StagedAttachment
import com.feedrail.sdk.`public`.FeedRailSDK
import com.feedrail.sdk.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/** Matches the submit-feedback form and the iOS SDK's comment composer. */
private const val MAX_ATTACHMENTS = 5

data class FeedbackDetailUiState(
    val feedback: FeedRailFeedbackItem? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isVoting: Boolean = false,
    val isSubmittingComment: Boolean = false,
    val commentText: String = "",
    val isAuthenticated: Boolean = false,
    val isAuthenticating: Boolean = false,
    val stagedAttachments: List<StagedAttachment> = emptyList(),
) {
    /**
     * Attachments still uploading block sending — otherwise the comment would post
     * without them, since only uploaded items have a storageKey to send.
     */
    val isUploadingAttachment: Boolean get() = stagedAttachments.any { it.isUploading }

    val canSendComment: Boolean
        get() = commentText.isNotBlank() && !isSubmittingComment && !isUploadingAttachment
}

class FeedbackDetailViewModel(
    private val apiClient: FeedRailApiClient,
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
                            ?: com.feedrail.sdk.models.FeedRailFeedbackCount(votes = response.voteCount)
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

    /**
     * Stages a picked file and starts uploading it immediately, so the upload is
     * already done (or visibly in flight) by the time the user hits send.
     */
    fun addAttachment(uri: Uri, context: Context) {
        val current = _uiState.value.stagedAttachments
        if (current.size >= MAX_ATTACHMENTS) return

        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val cursor = resolver.query(uri, null, null, null, null)
        val name = cursor?.use { c ->
            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            c.moveToFirst(); if (idx >= 0) c.getString(idx) else "file"
        } ?: "file"
        val size = cursor?.use { c ->
            val idx = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
            c.moveToFirst(); if (idx >= 0) c.getLong(idx) else 0L
        } ?: 0L

        val staged = StagedAttachment(uri, name, mimeType, size)
        _uiState.value = _uiState.value.copy(stagedAttachments = current + staged)
        uploadAttachment(staged, context)
    }

    private fun uploadAttachment(staged: StagedAttachment, context: Context) {
        viewModelScope.launch {
            updateStagedState(staged.localUri) { it.copy(isUploading = true) }
            try {
                // Downscale/re-encode off the main thread — decoding a 12MP photo on it
                // would jank the picker dismissal animation.
                val compressed = withContext(Dispatchers.IO) {
                    ImageCompressor.compress(
                        context, staged.localUri, staged.mimeType, staged.fileName,
                    )
                }
                if (compressed == null) {
                    // Unreadable Uri. Must clear isUploading, or the item spins forever
                    // and blocks sending with no way out but removing it.
                    updateStagedState(staged.localUri) { it.copy(isUploading = false, error = "Could not read file") }
                    return@launch
                }
                val body = compressed.bytes.toRequestBody(compressed.mimeType.toMediaType())
                val part = MultipartBody.Part.createFormData("file", compressed.fileName, body)
                val response = apiClient.call { uploadAttachment(part) }
                updateStagedState(staged.localUri) { it.copy(isUploading = false, uploadResponse = response) }
            } catch (e: Exception) {
                updateStagedState(staged.localUri) { it.copy(isUploading = false, error = e.message) }
            }
        }
    }

    fun removeAttachment(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            stagedAttachments = _uiState.value.stagedAttachments.filter { it.localUri != uri }
        )
    }

    private fun updateStagedState(uri: Uri, block: (StagedAttachment) -> StagedAttachment) {
        _uiState.value = _uiState.value.copy(
            stagedAttachments = _uiState.value.stagedAttachments.map {
                if (it.localUri == uri) block(it) else it
            }
        )
    }

    fun submitComment() {
        val feedbackId = _uiState.value.feedback?.id ?: return
        val text = _uiState.value.commentText.trim()
        if (text.isEmpty()) return

        val uploaded = _uiState.value.stagedAttachments.mapNotNull { it.uploadResponse }
        // `storageKey`, never `url` — the API's DTO validates with
        // forbidNonWhitelisted, so sending `url` gets the whole comment rejected 400.
        val attachments = uploaded.map { r ->
            AttachmentInput(
                storageKey = r.storageKey,
                fileName = r.fileName,
                mimeType = r.mimeType,
                fileSize = r.fileSize,
            )
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingComment = true)
            try {
                val comment = apiClient.call {
                    addComment(
                        feedbackId,
                        AddCommentBody(content = text, attachments = attachments.ifEmpty { null }),
                    )
                }

                // The create response doesn't echo attachments even though they are
                // persisted (confirmed against the API — a follow-up detail fetch shows
                // them). Without this, the comment we just appended would render with
                // its images missing until the screen is reloaded. The upload response
                // already carries everything needed, including a ready-to-render URL.
                val echoed = comment.attachments
                val posted = if (echoed.isNullOrEmpty() && uploaded.isNotEmpty()) {
                    comment.copy(
                        attachments = uploaded.map { r ->
                            FeedRailAttachment(
                                id = r.storageKey,
                                fileName = r.fileName,
                                mimeType = r.mimeType,
                                fileSize = r.fileSize,
                                url = r.url,
                                createdAt = comment.createdAt,
                            )
                        }
                    )
                } else {
                    comment
                }

                val updated = _uiState.value.feedback?.let { fb ->
                    fb.copy(comments = (fb.comments ?: emptyList()) + posted)
                }
                _uiState.value = _uiState.value.copy(
                    feedback = updated,
                    commentText = "",
                    stagedAttachments = emptyList(),
                    isSubmittingComment = false,
                )
            } catch (e: Exception) {
                // Keep the text and the staged attachments so a failed send can be
                // retried without re-picking and re-uploading everything.
                _uiState.value = _uiState.value.copy(isSubmittingComment = false)
            }
        }
    }

    companion object {
        fun factory(sdk: FeedRailSDK): ViewModelProvider.Factory = viewModelFactory {
            initializer { FeedbackDetailViewModel(sdk.apiClient, sdk.authManager) }
        }
    }
}
