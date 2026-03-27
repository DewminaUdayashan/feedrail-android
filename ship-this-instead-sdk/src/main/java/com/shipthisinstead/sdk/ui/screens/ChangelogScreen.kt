package com.shipthisinstead.sdk.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shipthisinstead.sdk.R
import com.shipthisinstead.sdk.models.STIReleaseNote
import com.shipthisinstead.sdk.ui.components.*
import com.shipthisinstead.sdk.ui.viewmodels.ChangelogViewModel

@Composable
fun ChangelogScreen(
    viewModel: ChangelogViewModel,
    onFeedbackClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 2 && uiState.hasMore && !uiState.isLoadingMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    when {
        uiState.isLoading && uiState.notes.isEmpty() -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null && uiState.notes.isEmpty() -> {
            ErrorContent(
                message = uiState.error ?: stringResource(R.string.sti_general_error_unknown),
                onRetry = {},
                modifier = modifier,
            )
        }
        uiState.notes.isEmpty() -> {
            EmptyStateContent(
                title = stringResource(R.string.sti_general_changelog_empty),
                subtitle = "",
                modifier = modifier,
            )
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(uiState.notes, key = { _, note -> note.id }) { _, note ->
                    ChangelogNoteCard(
                        note = note,
                        onFeedbackClick = onFeedbackClick,
                    )
                }

                if (uiState.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }

                // Add spacing for floating toolbar at the end
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
private fun ChangelogNoteCard(
    note: STIReleaseNote,
    onFeedbackClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium,
        border = null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Version badge + title row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val version = note.version
                if (!version.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    ) {
                        Text(
                            text = "v$version",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    modifier = Modifier.weight(1f),
                )
            }

            // Publish date
            val dateStr = note.publishedAt ?: note.createdAt
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = relativeTime(dateStr),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content
            RichTextContent(html = note.content)

            // Linked feedback
            val linked = note.linkedFeedbacks
            if (!linked.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.sti_general_changelog_linked_feedback),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                linked.forEach { linkedFeedback ->
                    val item = linkedFeedback.feedbackItem
                    Surface(
                        onClick = { onFeedbackClick?.invoke(item.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp),
                        ) {
                            Text(
                                text = "#${item.number}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val status = item.status
                            if (status != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusBadge(name = status.name, color = status.color)
                            }
                        }
                    }
                }
            }
        }
    }
}
