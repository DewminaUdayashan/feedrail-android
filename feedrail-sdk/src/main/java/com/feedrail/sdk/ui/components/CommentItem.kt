package com.feedrail.sdk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feedrail.sdk.R
import com.feedrail.sdk.models.FeedRailComment
import com.feedrail.sdk.models.FeedRailUser
import com.feedrail.sdk.models.FeedRailAttachment

@Composable
fun CommentItem(
    comment: FeedRailComment,
    baseUrl: String = "",
    isReply: Boolean = false,
    modifier: Modifier = Modifier,
    /** See [FeedbackRowItem]. Applies to the comment itself; replies carry their own. */
    fallbackAuthor: FeedRailUser? = null,
    /** The post this comment belongs to. Report and block are offered only when set. */
    feedbackId: String? = null,
) {
    val moderation = LocalModerationActions.current
    val author = comment.author ?: fallbackAuthor
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (isReply) 32.dp else 0.dp,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
        ) {
            AvatarImage(
                avatarUrl = author?.avatarUrl,
                displayName = author?.displayName ?: stringResource(R.string.feedrail_general_anonymous),
                size = if (isReply) 28.dp else 32.dp,
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = author?.displayName ?: stringResource(R.string.feedrail_general_anonymous),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )

                    if (comment.isVerified) {
                        TeamBadge()
                    }

                    Text(
                        text = relativeTime(comment.createdAt),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )

                    if (feedbackId != null) {
                        ModerationOverflowButton(
                            author = comment.endUserAuthor,
                            onReport = { reason, note ->
                                moderation?.reportComment(feedbackId, comment.id, reason, note)
                            },
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (hasHtmlTags(comment.content)) {
                    RichTextContent(html = comment.content)
                } else {
                    Text(
                        text = comment.content,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                }

                // Only ever present on the reader's own comment — without it a
                // pending comment looks published and the author has no idea nobody
                // else can see it yet.
                val moderation = comment.moderationState
                if (moderation != null && moderation.needsAuthorAttention) {
                    Spacer(modifier = Modifier.height(6.dp))
                    ModerationNotice(
                        status = moderation,
                        rejectionReason = comment.rejectionReason,
                    )
                }

                val attachments = comment.attachments
                if (!attachments.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    AttachmentDisplayRow(
                        attachments = attachments,
                        baseUrl = baseUrl,
                    )
                }
            }
        }

        // Replies (max 1 level deep)
        if (!isReply) {
            val replies = comment.replies
            if (!replies.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                replies.forEach { reply ->
                    CommentItem(
                        comment = reply,
                        baseUrl = baseUrl,
                        isReply = true,
                        feedbackId = feedbackId,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun TeamBadge() {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    ) {
        Text(
            text = "Team",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
        )
    }
}
