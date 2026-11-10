package com.feedrail.sdk.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ── User ─────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class FeedRailUser(
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
    /**
     * Falls back to "Anonymous · " plus a short id suffix when the user never told
     * us who they are, so two nameless users in one thread are still distinguishable
     * — a bare "Anonymous" made a thread read as one person talking to themselves.
     * Matches the name the API computes for feedback and comments, so the same user
     * is labelled identically on endpoints that apply it server-side and ones that
     * do not.
     */
    val displayName: String
        get() {
            if (!name.isNullOrBlank()) return name
            val full = listOfNotNull(firstName, lastName).joinToString(" ")
            return if (full.isNotBlank()) full else anonymousDisplayName(id)
        }

    val isGuestUser: Boolean get() = isGuest ?: false
    val isPaid: Boolean get() = isPaidCustomer ?: false
}

/** Shared fallback for a nameless end user. Mirrors the API's `anonymousDisplayName`. */
internal fun anonymousDisplayName(id: String): String {
    val suffix = id.takeLast(4)
    return if (suffix.isEmpty()) "Anonymous" else "Anonymous · $suffix"
}

@JsonClass(generateAdapter = true)
data class FeedRailUserCount(
    val feedbackItems: Int,
    val comments: Int,
    val votes: Int,
)

/** Profile response — backend flattens user fields at top level alongside _count. */
@JsonClass(generateAdapter = true)
data class FeedRailUserProfile(
    val id: String,
    val externalId: String? = null,
    val name: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val isGuest: Boolean? = null,
    @Json(name = "_count") val count: FeedRailUserCount,
) {
    /** See [FeedRailUser.displayName]. */
    val displayName: String
        get() {
            if (!name.isNullOrBlank()) return name
            val full = listOfNotNull(firstName, lastName).joinToString(" ")
            return if (full.isNotBlank()) full else anonymousDisplayName(id)
        }

    val isGuestUser: Boolean get() = isGuest ?: false

    /** The profile subject as an author, for activity rows that carry none. */
    fun asAuthor(): FeedRailUser = FeedRailUser(
        id = id,
        externalId = externalId,
        name = name,
        firstName = firstName,
        lastName = lastName,
        email = email,
        avatarUrl = avatarUrl,
        isGuest = isGuest,
    )
}

@JsonClass(generateAdapter = true)
data class FeedRailUserActivity(
    val feedback: List<FeedRailFeedbackItem>,
    val comments: List<FeedRailComment>,
)

// ── Board ─────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class FeedRailBoard(
    val id: String,
    val name: String,
    val slug: String,
    val color: String? = null,
)

// ── Status ────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class FeedRailStatus(
    val id: String,
    val name: String,
    val color: String,
    val feedbackItems: List<FeedRailFeedbackItem>? = null,
)

// ── Attachment ────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class FeedRailAttachment(
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
data class FeedRailComment(
    val id: String,
    val content: String,
    val isInternal: Boolean? = null,
    val user: FeedRailUser? = null,
    val orgUser: FeedRailUser? = null,
    val attachments: List<FeedRailAttachment>? = null,
    val replies: List<FeedRailComment>? = null,
    val feedbackItem: FeedRailReleaseNoteFeedbackItem? = null,
    /**
     * Review state of the comment. The API only returns a non-approved comment to its
     * own author, so PENDING/REJECTED here always concerns the reader's own comment.
     * Kept as a raw String and mapped leniently — see [moderationState].
     */
    val moderationStatus: String? = null,
    /** Why the comment was rejected, when the team supplied a reason. */
    val rejectionReason: String? = null,
    /** Written by the team's AI agent. The portal API filters these out today. */
    val isAiComment: Boolean? = null,
    val createdAt: String,
) {
    val author: FeedRailUser? get() = orgUser ?: user
    val isVerified: Boolean get() = orgUser != null

    /**
     * The end user who wrote this comment, when it is one — the only kind of author
     * that can be reported or blocked. Null for team members and the AI agent.
     */
    val endUserAuthor: FeedRailUser?
        get() = if (orgUser != null || isAiComment == true) null else user

    /** [moderationStatus] mapped to a known case, or null if absent/unrecognised. */
    val moderationState: FeedRailModerationStatus?
        get() = FeedRailModerationStatus.from(moderationStatus)
}

// ── Feedback ──────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class FeedRailFeedbackCount(
    val votes: Int,
    val comments: Int? = null,
)

@JsonClass(generateAdapter = true)
data class FeedRailFeedbackItem(
    val id: String,
    val number: Int,
    val title: String,
    val content: String? = null,
    val isPinned: Boolean = false,
    val isTeamPost: Boolean = false,
    val status: FeedRailStatus? = null,
    val board: FeedRailBoard? = null,
    val user: FeedRailUser? = null,
    val orgUser: FeedRailUser? = null,
    val attachments: List<FeedRailAttachment>? = null,
    val comments: List<FeedRailComment>? = null,
    val hasVoted: Boolean? = null,
    /**
     * Review state of the post. The API only ever returns a non-approved post to its
     * own author, so a PENDING/REJECTED value here always concerns the current user's
     * own submission. Kept as a raw String rather than an enum so an unrecognised
     * value from a newer API can't fail the whole response — see [moderationState].
     */
    val moderationStatus: String? = null,
    /** Why the post was rejected, when the team supplied a reason. */
    val rejectionReason: String? = null,
    @Json(name = "_count") val count: FeedRailFeedbackCount? = null,
    val createdAt: String,
    val updatedAt: String,
) {
    val voteCount: Int get() = count?.votes ?: 0
    val commentCount: Int get() = count?.comments ?: 0
    val author: FeedRailUser? get() = if (isTeamPost) orgUser else user

    /** The end user who wrote this post, or null for a team post. See [FeedRailComment.endUserAuthor]. */
    val endUserAuthor: FeedRailUser?
        get() = if (isTeamPost || orgUser != null) null else user

    /** [moderationStatus] mapped to a known case, or null if absent/unrecognised. */
    val moderationState: FeedRailModerationStatus?
        get() = FeedRailModerationStatus.from(moderationStatus)
}

/** Review state of a post, as surfaced to its own author. */
enum class FeedRailModerationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    ;

    /**
     * Whether this state is worth surfacing. An approved post looks like everyone
     * else's and needs no extra chrome.
     */
    val needsAuthorAttention: Boolean get() = this == PENDING || this == REJECTED

    companion object {
        /**
         * Lenient lookup: an unknown value (e.g. a case added by a newer API) maps to
         * null and simply renders no badge, rather than throwing.
         */
        fun from(raw: String?): FeedRailModerationStatus? =
            entries.firstOrNull { it.name == raw }
    }
}

@JsonClass(generateAdapter = true)
data class FeedRailVoteResponse(
    val voted: Boolean,
    val voteCount: Int,
)

// ── Branding ──────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class FeedRailBranding(
    val projectName: String? = null,
    val accentColor: String? = null,
    val logoUrl: String? = null,
    val portalTheme: String? = null,
    val showPoweredBy: Boolean? = null,
    val ssoLoginUrl: String? = null,
    val requiresAuth: Boolean? = null,
    val allowAttachments: Boolean? = null,
    /** Whether the host app's [com.feedrail.sdk.public.FeedRailConfig.themeColor] may
     * override [accentColor]. Gated server-side by the org's plan. */
    val allowNativeTheme: Boolean? = null,
    /** Whether guest / anonymous users may post on this plan. */
    val allowGuestPosting: Boolean? = null,
) {
    val resolvedAccentColor: String get() = accentColor ?: "#25A777"
    val shouldShowPoweredBy: Boolean get() = showPoweredBy ?: true
    val attachmentsAllowed: Boolean get() = allowAttachments ?: true
    val allowsNativeTheme: Boolean get() = allowNativeTheme ?: false
    val guestPostingAllowed: Boolean get() = allowGuestPosting ?: false
}

@JsonClass(generateAdapter = true)
data class FeedRailBoardsResponse(
    val boards: List<FeedRailBoard>,
    val branding: FeedRailBranding? = null,
    val isProjectArchived: Boolean? = null,
)

// ── Pagination ────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class FeedRailPaginationMeta(
    val total: Int,
    val page: Int,
    val limit: Int,
    val totalPages: Int,
)

// Generic — KotlinJsonAdapterFactory handles this at runtime (KSP codegen can't handle type params)
data class FeedRailPaginatedResponse<T>(
    val items: List<T>,
    val meta: FeedRailPaginationMeta,
)

// ── Changelog ─────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class FeedRailReleaseNote(
    val id: String,
    val title: String,
    val content: String,
    val version: String? = null,
    val isDraft: Boolean,
    val publishedAt: String? = null,
    val createdAt: String,
    val linkedFeedbacks: List<FeedRailReleaseNoteFeedback>? = null,
) {
    val isPublished: Boolean get() = !isDraft
}

@JsonClass(generateAdapter = true)
data class FeedRailReleaseNoteFeedback(
    val feedbackItem: FeedRailReleaseNoteFeedbackItem,
) {
    val id: String get() = feedbackItem.id
}

@JsonClass(generateAdapter = true)
data class FeedRailReleaseNoteFeedbackItem(
    val id: String,
    val number: Int,
    val title: String,
    val status: FeedRailStatus? = null,
)

// ── Auth ──────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class FeedRailAuthResponse(
    val endUserToken: String? = null,
    val user: FeedRailUser? = null,
)

// ── Upload ────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class FeedRailUploadResponse(
    /** Opaque storage path (e.g. `attachments/<uuid>.jpg`). This — not [url] — is
     *  what the API expects back on the attachments[] of a submit/comment call. */
    val storageKey: String,
    /** Ready-to-render path for the uploaded file; display only. */
    val url: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Int,
)

// ── Report & Block ────────────────────────────────────────────────────────────

/** Why a post or comment is being reported. Sent as the enum name. */
enum class FeedRailReportReason {
    SPAM,
    HARASSMENT,
    HATE,
    SEXUAL,
    VIOLENCE,
    OTHER,
}

@JsonClass(generateAdapter = true)
data class FeedRailReportResponse(
    val id: String,
    val status: String,
)

@JsonClass(generateAdapter = true)
data class FeedRailBlockedUser(
    val id: String,
    val name: String? = null,
    val avatarUrl: String? = null,
) {
    val displayName: String
        get() = if (!name.isNullOrBlank()) name else anonymousDisplayName(id)
}

@JsonClass(generateAdapter = true)
data class FeedRailBlockedUserEntry(
    val id: String,
    val createdAt: String,
    val blockedUser: FeedRailBlockedUser,
)
