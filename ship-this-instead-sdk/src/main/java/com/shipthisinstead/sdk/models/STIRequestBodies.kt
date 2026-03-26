package com.shipthisinstead.sdk.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SubmitFeedbackBody(
    val title: String,
    val content: String?,
    val boardId: String,
    val attachments: List<AttachmentInput>? = null,
)

@JsonClass(generateAdapter = true)
data class AttachmentInput(
    val url: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Int,
)

@JsonClass(generateAdapter = true)
data class AddCommentBody(
    val content: String,
    val attachments: List<AttachmentInput>? = null,
)

@JsonClass(generateAdapter = true)
data class SdkAuthBody(
    val secretKey: String,
    val userData: SdkUserData,
)

@JsonClass(generateAdapter = true)
data class SdkUserData(
    val externalId: String,
    val name: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val isPaidCustomer: Boolean? = null,
    val productId: String? = null,
    val metadata: Map<String, String>? = null,
)

/** Staged attachment before upload completes. */
data class StagedAttachment(
    val localUri: android.net.Uri,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val uploadResponse: STIUploadResponse? = null,
    val isUploading: Boolean = false,
    val error: String? = null,
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isUploaded: Boolean get() = uploadResponse != null
}
