package com.feedrail.sdk.moderation

import com.feedrail.sdk.api.FeedRailApiClient
import com.feedrail.sdk.models.FeedRailComment
import com.feedrail.sdk.models.FeedRailError
import com.feedrail.sdk.models.FeedRailFeedbackItem
import com.feedrail.sdk.models.FeedRailReportReason
import com.feedrail.sdk.models.FeedRailUser
import com.feedrail.sdk.models.ReportContentBody
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Something the reader did that should take content off their screen right away.
 *
 * The API already hides reported items and blocked authors on every later fetch;
 * these events make the screens that are already showing that content agree with
 * it immediately, including ones further down the back stack.
 */
internal sealed interface ModerationEvent {
    data class FeedbackReported(val feedbackId: String) : ModerationEvent
    data class CommentReported(val feedbackId: String, val commentId: String) : ModerationEvent
    data class UserBlocked(val userId: String) : ModerationEvent
    data class UserUnblocked(val userId: String) : ModerationEvent
}

/** Report and block calls, plus the process-wide stream of their outcomes. */
internal object Moderation {

    private val _events = MutableSharedFlow<ModerationEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<ModerationEvent> = _events.asSharedFlow()

    suspend fun reportFeedback(
        api: FeedRailApiClient,
        feedbackId: String,
        reason: FeedRailReportReason,
        note: String?,
    ) {
        api.call { reportFeedback(feedbackId, body(reason, note)) }
        _events.emit(ModerationEvent.FeedbackReported(feedbackId))
    }

    suspend fun reportComment(
        api: FeedRailApiClient,
        feedbackId: String,
        commentId: String,
        reason: FeedRailReportReason,
        note: String?,
    ) {
        api.call { reportComment(feedbackId, commentId, body(reason, note)) }
        _events.emit(ModerationEvent.CommentReported(feedbackId, commentId))
    }

    suspend fun blockUser(api: FeedRailApiClient, userId: String) {
        val response = api.call { blockUser(userId) }
        if (!response.isSuccessful) throw FeedRailError.ServerError(response.code(), response.message())
        _events.emit(ModerationEvent.UserBlocked(userId))
    }

    suspend fun unblockUser(api: FeedRailApiClient, userId: String) {
        val response = api.call { unblockUser(userId) }
        if (!response.isSuccessful) throw FeedRailError.ServerError(response.code(), response.message())
        _events.emit(ModerationEvent.UserUnblocked(userId))
    }

    private fun body(reason: FeedRailReportReason, note: String?) =
        ReportContentBody(reason = reason.name, note = note?.trim()?.take(MAX_NOTE)?.ifEmpty { null })

    const val MAX_NOTE = 500
}

/**
 * Whether the reader may report or block [author]: only an end user, never a team
 * member or the AI agent (callers pass null for those), and never the reader.
 */
internal fun canModerate(author: FeedRailUser?, currentUserId: String?): Boolean =
    author != null && author.id != currentUserId

// ── State filters ────────────────────────────────────────────────────────────

/** Drops a comment, or a reply, by id. */
internal fun List<FeedRailComment>.withoutComment(commentId: String): List<FeedRailComment> =
    filter { it.id != commentId }.map { c ->
        val replies = c.replies
        if (replies.isNullOrEmpty()) c else c.copy(replies = replies.filter { it.id != commentId })
    }

/** Drops every comment and reply written by end user [userId]. */
internal fun List<FeedRailComment>.withoutAuthor(userId: String): List<FeedRailComment> =
    filter { it.endUserAuthor?.id != userId }.map { c ->
        val replies = c.replies
        if (replies.isNullOrEmpty()) c else c.copy(replies = replies.filter { it.endUserAuthor?.id != userId })
    }

internal fun FeedRailFeedbackItem.isByEndUser(userId: String): Boolean = endUserAuthor?.id == userId

internal fun FeedRailFeedbackItem.withoutComment(commentId: String): FeedRailFeedbackItem =
    comments?.let { copy(comments = it.withoutComment(commentId)) } ?: this

internal fun FeedRailFeedbackItem.withoutCommentsBy(userId: String): FeedRailFeedbackItem =
    comments?.let { copy(comments = it.withoutAuthor(userId)) } ?: this
