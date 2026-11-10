package com.feedrail.sdk.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SubmitFeedbackBody(
    val title: String,
    val content: String?,
    /** The board's slug. The API identifies boards by slug here and rejects a `boardId`. */
    val boardSlug: String,
    val attachments: List<AttachmentInput>? = null,
)

@JsonClass(generateAdapter = true)
data class AttachmentInput(
    val storageKey: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Int,
)

@JsonClass(generateAdapter = true)
data class AddCommentBody(
    val content: String,
    val attachments: List<AttachmentInput>? = null,
)

/** Body for reporting a post or comment. [reason] is a [FeedRailReportReason] name. */
@JsonClass(generateAdapter = true)
data class ReportContentBody(
    val reason: String,
    /** Optional free text, at most 500 characters (the API rejects longer). */
    val note: String? = null,
)

@JsonClass(generateAdapter = true)
data class SdkAuthBody(
    val apiKey: String,
    val userData: SdkUserData,
    /**
     * The device-scoped anonymous ID the SDK used before this identify call.
     * When present, the anonymous account is merged into the identified one.
     */
    val anonymousId: String? = null,
)

@JsonClass(generateAdapter = true)
data class AnonymousAuthBody(
    val anonymousId: String,
    val device: String? = null,
    val model: String? = null,
    val os: String? = null,
    val osVersion: String? = null,
    val appVersion: String? = null,
    val source: String? = null,
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
    val device: String? = null,
    val model: String? = null,
    val os: String? = null,
    val osVersion: String? = null,
    val appVersion: String? = null,
    val source: String? = null,
)

/** Staged attachment before upload completes. */
data class StagedAttachment(
    val localUri: android.net.Uri,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val uploadResponse: FeedRailUploadResponse? = null,
    val isUploading: Boolean = false,
    val error: String? = null,
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isUploaded: Boolean get() = uploadResponse != null
}
