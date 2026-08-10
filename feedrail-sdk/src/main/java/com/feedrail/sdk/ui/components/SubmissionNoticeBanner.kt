package com.feedrail.sdk.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.feedrail.sdk.R
import com.feedrail.sdk.models.FeedRailModerationStatus

/**
 * Confirms that a post the reader just wrote was received, and says where it went.
 *
 * Submitting used to pop back to a list that had not changed, which told the reader
 * nothing. Under the default Top Voted order a new post with a single vote sorts near
 * the bottom, and a post awaiting review is not shown to anybody but its author — so
 * in the ordinary case there was no visible difference between a successful
 * submission and a silent failure.
 */
@Composable
internal fun SubmissionNoticeBanner(
    status: FeedRailModerationStatus?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPending = status == FeedRailModerationStatus.PENDING ||
        status == FeedRailModerationStatus.REJECTED

    val tint = if (isPending) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = tint.copy(alpha = 0.10f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = if (isPending) Icons.Outlined.Info else Icons.Default.Check,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(
                    if (isPending) {
                        R.string.feedrail_feedback_submit_notice_pending
                    } else {
                        R.string.feedrail_feedback_submit_notice_posted
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.feedrail_general_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
