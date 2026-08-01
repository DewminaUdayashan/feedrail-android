package com.feedrail.sdk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feedrail.sdk.R
import com.feedrail.sdk.models.FeedRailModerationStatus

private val PendingTint = Color(0xFFE08A00)
private val RejectedTint = Color(0xFFD93025)

/**
 * Compact badge marking a post that is awaiting review or was rejected.
 *
 * The API only returns a non-approved post to its own author, so this is always the
 * reader's own submission. Without it the post just appears in the list looking
 * published, and the author has no way to tell nobody else can see it yet — or that
 * it was turned down.
 */
@Composable
internal fun ModerationBadge(
    status: FeedRailModerationStatus,
    modifier: Modifier = Modifier,
) {
    if (!status.needsAuthorAttention) return
    val tint = status.tint()

    Surface(
        color = tint.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Icon(
                imageVector = status.icon(),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = stringResource(status.labelRes()).uppercase(),
                color = tint,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                ),
            )
        }
    }
}

/**
 * Full-width explanation shown on the author's own post detail, carrying the
 * rejection reason when the team left one.
 */
@Composable
internal fun ModerationNotice(
    status: FeedRailModerationStatus,
    rejectionReason: String?,
    modifier: Modifier = Modifier,
) {
    if (!status.needsAuthorAttention) return
    val tint = status.tint()

    Surface(
        color = tint.copy(alpha = 0.10f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            Icon(
                imageVector = status.icon(),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(status.labelRes()),
                    color = tint,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = stringResource(status.explanationRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Only rejections carry a reason, and only when the team wrote one —
                // a blank string would otherwise render an empty row.
                val reason = rejectionReason?.trim()
                if (status == FeedRailModerationStatus.REJECTED && !reason.isNullOrEmpty()) {
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

private fun FeedRailModerationStatus.tint(): Color = when (this) {
    FeedRailModerationStatus.PENDING -> PendingTint
    FeedRailModerationStatus.REJECTED -> RejectedTint
    FeedRailModerationStatus.APPROVED -> PendingTint // unused; never rendered
}

private fun FeedRailModerationStatus.icon(): ImageVector = when (this) {
    FeedRailModerationStatus.REJECTED -> Icons.Default.Block
    else -> Icons.Default.Schedule
}

private fun FeedRailModerationStatus.labelRes(): Int = when (this) {
    FeedRailModerationStatus.REJECTED -> R.string.feedrail_moderation_rejected
    else -> R.string.feedrail_moderation_pending
}

private fun FeedRailModerationStatus.explanationRes(): Int = when (this) {
    FeedRailModerationStatus.REJECTED -> R.string.feedrail_moderation_rejected_explanation
    else -> R.string.feedrail_moderation_pending_explanation
}
