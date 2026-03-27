package com.shipthisinstead.sdk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shipthisinstead.sdk.R
import com.shipthisinstead.sdk.models.STIFeedbackItem
import com.shipthisinstead.sdk.models.STIStatus
import com.shipthisinstead.sdk.ui.components.EmptyStateContent
import com.shipthisinstead.sdk.ui.components.ErrorContent
import com.shipthisinstead.sdk.ui.components.VoteButton
import com.shipthisinstead.sdk.ui.theme.Color as STIColor
import com.shipthisinstead.sdk.ui.viewmodels.RoadmapViewModel

@Composable
fun RoadmapScreen(
    viewModel: RoadmapViewModel,
    onFeedbackClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null -> {
            ErrorContent(
                message = uiState.error ?: stringResource(R.string.sti_general_error_unknown),
                onRetry = viewModel::load,
                modifier = modifier,
            )
        }
        uiState.statuses.isEmpty() -> {
            EmptyStateContent(
                title = stringResource(R.string.sti_general_roadmap_empty),
                subtitle = "",
                modifier = modifier,
            )
        }
        else -> {
            LazyRow(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(uiState.statuses, key = { it.id }) { status ->
                    RoadmapColumn(
                        status = status,
                        onFeedbackClick = onFeedbackClick,
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun RoadmapColumn(
    status: STIStatus,
    onFeedbackClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val defaultStatusColor = MaterialTheme.colorScheme.primary
    val statusColor = try {
        STIColor(status.color)
    } catch (e: Exception) {
        defaultStatusColor
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Column header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp, start = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
                Text(
                    text = status.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val count = status.feedbackItems?.size ?: 0
                if (count > 0) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            val items = status.feedbackItems
            if (items.isNullOrEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.sti_general_roadmap_no_items),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 80.dp) // Added padding for floating toolbar
                ) {
                    items.forEach { item ->
                        RoadmapCard(
                            item = item,
                            statusColor = statusColor,
                            onClick = { onFeedbackClick(item.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoadmapCard(
    item: STIFeedbackItem,
    statusColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "#${item.number}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            VoteButton(
                voteCount = item.voteCount,
                hasVoted = item.hasVoted ?: false,
                isEnabled = false,
                onClick = onClick,
            )
        }
    }
}
