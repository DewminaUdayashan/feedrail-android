package com.shipthisinstead.sdk.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ── User ─────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class STIUser(
    val id: String,
    val externalId: String? = null,
    val name: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val isGuest: Boolean? = null,
    val isPaidCustomer: Boolean? = null,
    val productId: String? = null,
) {
    val displayName: String
        get() {
            if (!name.isNullOrBlank()) return name
            val full = listOfNotNull(firstName, lastName).joinToString(" ")
            return if (full.isNotBlank()) full else "Anonymous"
        }

    val isGuestUser: Boolean get() = isGuest ?: false
    val isPaid: Boolean get() = isPaidCustomer ?: false
}

@JsonClass(generateAdapter = true)
data class STIUserCount(
    val feedbackItems: Int,
    val comments: Int,
    val votes: Int,
)

/** Profile response — backend flattens user fields at top level alongside _count. */
@JsonClass(generateAdapter = true)
data class STIUserProfile(
    val id: String,
    val name: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    @Json(name = "_count") val count: STIUserCount,
) {
    val displayName: String
        get() {
            if (!name.isNullOrBlank()) return name
            val full = listOfNotNull(firstName, lastName).joinToString(" ")
            return if (full.isNotBlank()) full else "Anonymous"
        }
}

@JsonClass(generateAdapter = true)
data class STIUserActivity(
    val feedback: List<STIFeedbackItem>,
    val comments: List<STIComment>,
)

// ── Board ─────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class STIBoard(
    val id: String,
    val name: String,
    val slug: String,
    val color: String? = null,
)

// ── Status ────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class STIStatus(
    val id: String,
    val name: String,
    val color: String,
    val feedbackItems: List<STIFeedbackItem>? = null,
)

// ── Attachment ────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class STIAttachment(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Int,
    val url: String,
    val createdAt: String,
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")

    val formattedSize: String
        get() = when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> "%.1f KB".format(fileSize / 1024.0)
            else -> "%.1f MB".format(fileSize / (1024.0 * 1024))
        }
}

// ── Comment ───────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class STIComment(
    val id: String,
    val content: String,
    val isInternal: Boolean? = null,
    val user: STIUser? = null,
    val orgUser: STIUser? = null,
    val attachments: List<STIAttachment>? = null,
    val replies: List<STIComment>? = null,
    val feedbackItem: STIReleaseNoteFeedbackItem? = null,
    val createdAt: String,
) {
    val author: STIUser? get() = orgUser ?: user
    val isVerified: Boolean get() = orgUser != null
}

// ── Feedback ──────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class STIFeedbackCount(
    val votes: Int,
    val comments: Int? = null,
)

@JsonClass(generateAdapter = true)
data class STIFeedbackItem(
    val id: String,
    val number: Int,
    val title: String,
    val content: String? = null,
    val isPinned: Boolean = false,
    val isTeamPost: Boolean = false,
    val status: STIStatus? = null,
    val board: STIBoard? = null,
    val user: STIUser? = null,
    val orgUser: STIUser? = null,
    val attachments: List<STIAttachment>? = null,
    val comments: List<STIComment>? = null,
    val hasVoted: Boolean? = null,
    @Json(name = "_count") val count: STIFeedbackCount? = null,
    val createdAt: String,
    val updatedAt: String,
) {
    val voteCount: Int get() = count?.votes ?: 0
    val commentCount: Int get() = count?.comments ?: 0
    val author: STIUser? get() = if (isTeamPost) orgUser else user
}

@JsonClass(generateAdapter = true)
data class STIVoteResponse(
    val voted: Boolean,
    val voteCount: Int,
)

// ── Branding ──────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class STIBranding(
    val projectName: String? = null,
    val accentColor: String? = null,
    val logoUrl: String? = null,
    val portalTheme: String? = null,
    val showPoweredBy: Boolean? = null,
    val ssoLoginUrl: String? = null,
    val requiresAuth: Boolean? = null,
    val allowAttachments: Boolean? = null,
) {
    val resolvedAccentColor: String get() = accentColor ?: "#6366F1"
    val shouldShowPoweredBy: Boolean get() = showPoweredBy ?: true
    val attachmentsAllowed: Boolean get() = allowAttachments ?: true
}

@JsonClass(generateAdapter = true)
data class STIBoardsResponse(
    val boards: List<STIBoard>,
    val branding: STIBranding? = null,
    val isProjectArchived: Boolean? = null,
)

// ── Pagination ────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class STIPaginationMeta(
    val total: Int,
    val page: Int,
    val limit: Int,
    val totalPages: Int,
)

// Generic — KotlinJsonAdapterFactory handles this at runtime (KSP codegen can't handle type params)
data class STIPaginatedResponse<T>(
    val items: List<T>,
    val meta: STIPaginationMeta,
)

// ── Changelog ─────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class STIReleaseNote(
    val id: String,
    val title: String,
    val content: String,
    val version: String? = null,
    val isDraft: Boolean,
    val publishedAt: String? = null,
    val createdAt: String,
    val linkedFeedbacks: List<STIReleaseNoteFeedback>? = null,
) {
    val isPublished: Boolean get() = !isDraft
}

@JsonClass(generateAdapter = true)
data class STIReleaseNoteFeedback(
    val feedbackItem: STIReleaseNoteFeedbackItem,
) {
    val id: String get() = feedbackItem.id
}

@JsonClass(generateAdapter = true)
data class STIReleaseNoteFeedbackItem(
    val id: String,
    val number: Int,
    val title: String,
    val status: STIStatus? = null,
)

// ── Auth ──────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class STIAuthResponse(
    val endUserToken: String? = null,
    val user: STIUser? = null,
)

// ── Upload ────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class STIUploadResponse(
    val url: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Int,
)
