package com.feedrail.sdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.feedrail.sdk.R
import com.feedrail.sdk.models.FeedRailBoard
import com.feedrail.sdk.ui.theme.Color as FeedRailColor

/**
 * Board filters and the sort control on a single row.
 *
 * These used to occupy two rows, the second holding nothing but a sort pill pushed to
 * the far edge — a full band of empty space above the feed. The boards now scroll
 * horizontally in the space they need and sort sits pinned beside them, outside the
 * scroll so it stays reachable however many boards a project has.
 */
@Composable
fun FeedbackFilterBar(
    boards: List<FeedRailBoard>,
    selectedSlug: String?,
    onSelectBoard: (String?) -> Unit,
    sortLabel: String,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A single-board project has nothing to filter, so the chips are dropped and
        // sort keeps the row rather than sitting alone opposite an empty stretch.
        if (boards.size > 1) {
            LazyRow(
                modifier = Modifier
                    .weight(1f)
                    .fadingEdge(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                item {
                    BoardChip(
                        label = stringResource(R.string.feedrail_feedback_list_filter_all),
                        color = null,
                        selected = selectedSlug == null,
                        onClick = { onSelectBoard(null) },
                    )
                }
                items(items = boards, key = { it.slug }) { board ->
                    BoardChip(
                        label = board.name,
                        color = board.color,
                        selected = selectedSlug == board.slug,
                        onClick = { onSelectBoard(board.slug) },
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        SortButton(
            currentSortLabel = sortLabel,
            onClick = onSortClick,
            modifier = Modifier.padding(start = 8.dp, end = 16.dp),
        )
    }
}

@Composable
private fun BoardChip(
    label: String,
    color: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                ),
            )
        },
        // The board's own colour, as a dot — the same cue iOS has always shown, and the
        // one thing tying a chip to the coloured board label on the cards below it.
        leadingIcon = color?.let { hex ->
            val dotColor = runCatching { FeedRailColor(hex) }.getOrNull()
            if (dotColor == null) {
                null
            } else {
                {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                }
            }
        },
    )
}

/**
 * Sort, as an icon alone.
 *
 * The current order is not spelled out here — it is named, and checked, in the sheet this
 * opens, and carried for screen readers by the content description.
 */
@Composable
private fun SortButton(
    currentSortLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.size(40.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Sort,
            contentDescription = stringResource(
                R.string.feedrail_feedback_list_sort_label,
            ) + ": " + currentSortLabel,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Fades the chip row out where it meets the pinned sort control, so it reads as
 * scrollable content running under it rather than a list that happens to be clipped.
 */
private fun Modifier.fadingEdge(): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val fade = 24.dp.toPx()
        // The chips overflow towards whichever edge the sort chip is pinned against,
        // which flips with the layout direction.
        val brush = if (layoutDirection == LayoutDirection.Rtl) {
            Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startX = 0f,
                endX = fade,
            )
        } else {
            Brush.horizontalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startX = size.width - fade,
                endX = size.width,
            )
        }
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }
