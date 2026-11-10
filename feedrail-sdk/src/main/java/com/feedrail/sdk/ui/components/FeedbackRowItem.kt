package com.feedrail.sdk.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.feedrail.sdk.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feedrail.sdk.models.FeedRailFeedbackItem
import com.feedrail.sdk.models.FeedRailUser
import com.feedrail.sdk.ui.theme.Color as FeedRailColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedbackRowItem(
    item: FeedRailFeedbackItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Author to show when the row carries none of its own. Older API deployments
     * return a profile's own posts without repeating the author on every row, which
     * dropped the byline entirely; the profile subject wrote everything in that list.
     */
    fallbackAuthor: FeedRailUser? = null,
    /** Tints the row — used for the post the reader has just submitted. */
    highlighted: Boolean = false,
) {
    // Tonal, not elevated. A shadow under every row meant a dozen shadows stacked down
    // the feed, which read as noise rather than hierarchy; a container one tonal step
    // off the background separates the cards without any of that weight. The vertical
    // padding is gone too — it stacked on the list's own 12dp spacing and pushed the
    // real gap to 20dp, which is what made the feed feel both heavy and sparse.
    //
    // Long-press opens Report/Block for other end users' posts. Card's own onClick
    // can't take a long-press, hence combinedClickable on a clipped Card instead.
    val moderation = LocalModerationActions.current
    val moderatable = item.endUserAuthor?.takeIf { moderation?.canModerate(it) == true }
    var menuExpanded by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val longPressLabel = stringResource(R.string.feedrail_moderation_more_options)
    val shape = MaterialTheme.shapes.large

    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = if (moderatable != null) {
                        {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuExpanded = true
                        }
                    } else null,
                    onLongClickLabel = if (moderatable != null) longPressLabel else null,
                ),
            shape = shape,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (highlighted) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // Board + Status pill row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val board = item.board
                            if (board != null) {
                                val defaultBoardColor = MaterialTheme.colorScheme.primary
                                val boardColor = try {
                                    board.color?.let { FeedRailColor(it) } ?: defaultBoardColor
                                } catch (e: Exception) {
                                    defaultBoardColor
                                }
                                Surface(
                                    color = boardColor.copy(alpha = 0.1f),
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(
                                        text = board.name.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = boardColor,
                                            letterSpacing = 0.5.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }

                            // A post awaiting review or rejected is only ever visible to
                            // its own author, so the review state replaces the workflow
                            // status — it's what actually matters to them.
                            val moderation = item.moderationState
                            val status = item.status
                            if (moderation != null && moderation.needsAuthorAttention) {
                                ModerationBadge(status = moderation)
                            } else if (status != null) {
                                StatusBadge(name = status.name, color = status.color)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Title
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Vote pill
                    VoteButton(
                        voteCount = item.voteCount,
                        hasVoted = item.hasVoted ?: false,
                        isEnabled = true,
                        onClick = onClick,
                    )
                }

                // Expanded body if present
                val plainContent = item.content?.let { stripHtml(it) }
                if (!plainContent.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = plainContent,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Footer metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val author = item.author ?: fallbackAuthor
                        if (author != null) {
                            AvatarImage(
                                avatarUrl = author.avatarUrl,
                                displayName = author.displayName,
                                size = 20.dp,
                            )
                            Text(
                                text = author.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                        Text(
                            text = relativeTime(item.createdAt),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (item.commentCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = item.commentCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (moderatable != null) {
            ModerationMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                author = moderatable,
                onReport = { reason, note -> moderation?.reportFeedback(item.id, reason, note) },
            )
        }
    }
}
