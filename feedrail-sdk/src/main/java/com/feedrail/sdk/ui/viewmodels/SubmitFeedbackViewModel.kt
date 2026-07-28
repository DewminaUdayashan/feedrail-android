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
import com.feedrail.sdk.models.AttachmentInput
import com.feedrail.sdk.models.FeedRailBoard
import com.feedrail.sdk.models.StagedAttachment
import com.feedrail.sdk.models.SubmitFeedbackBody
import com.feedrail.sdk.`public`.FeedRailSDK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class SubmitFeedbackUiState(
    val title: String = "",
    val description: String = "",
    val selectedBoard: FeedRailBoard? = null,
    val boards: List<FeedRailBoard> = emptyList(),
    val stagedAttachments: List<StagedAttachment> = emptyList(),
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val isAuthenticating: Boolean = false,
) {
    val canSubmit: Boolean get() = title.isNotBlank() && selectedBoard != null && isAuthenticated && !isSubmitting
}

class SubmitFeedbackViewModel(
    private val apiClient: FeedRailApiClient,
    private val authManager: AuthManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubmitFeedbackUiState())
    val uiState: StateFlow<SubmitFeedbackUiState> = _uiState.asStateFlow()

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

    fun setBoards(boards: List<FeedRailBoard>) {
        _uiState.value = _uiState.value.copy(
            boards = boards,
            selectedBoard = if (boards.size == 1) boards.first() else _uiState.value.selectedBoard,
        )
    }

    fun updateTitle(title: String) = run { _uiState.value = _uiState.value.copy(title = title) }
    fun updateDescription(desc: String) = run { _uiState.value = _uiState.value.copy(description = desc) }
    fun selectBoard(board: FeedRailBoard) = run { _uiState.value = _uiState.value.copy(selectedBoard = board) }

    fun addAttachment(uri: Uri, context: Context) {
        val s = _uiState.value
        if (s.stagedAttachments.size >= 5) return
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
        _uiState.value = _uiState.value.copy(stagedAttachments = s.stagedAttachments + staged)
        uploadAttachment(staged, context)
    }

    private fun uploadAttachment(staged: StagedAttachment, context: Context) {
        viewModelScope.launch {
            updateStagedState(staged.localUri) { it.copy(isUploading = true) }
            try {
                val bytes = context.contentResolver.openInputStream(staged.localUri)?.readBytes()
                    ?: return@launch
                val body = bytes.toRequestBody(staged.mimeType.toMediaType())
                val part = MultipartBody.Part.createFormData("file", staged.fileName, body)
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
            stagedAttachments = _uiState.value.stagedAttachments.map { if (it.localUri == uri) block(it) else it }
        )
    }

    fun submit(onSuccess: () -> Unit) {
        val s = _uiState.value
        if (!s.canSubmit) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            try {
                val attachments = s.stagedAttachments.mapNotNull { a ->
                    a.uploadResponse?.let { r ->
                        AttachmentInput(r.url, r.fileName, r.mimeType, r.fileSize)
                    }
                }
                apiClient.call {
                    submitFeedback(
                        SubmitFeedbackBody(
                            title = s.title.trim(),
                            content = s.description.trim().ifEmpty { null },
                            boardId = s.selectedBoard!!.id,
                            attachments = attachments.ifEmpty { null },
                        )
                    )
                }
                _uiState.value = _uiState.value.copy(isSubmitting = false, isSubmitted = true)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = e.message)
            }
        }
    }

    companion object {
        fun factory(sdk: FeedRailSDK): ViewModelProvider.Factory = viewModelFactory {
            initializer { SubmitFeedbackViewModel(sdk.apiClient, sdk.authManager) }
        }
    }
}
