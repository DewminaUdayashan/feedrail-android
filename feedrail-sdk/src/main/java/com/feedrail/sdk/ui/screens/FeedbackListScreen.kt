package com.feedrail.sdk.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feedrail.sdk.R
import com.feedrail.sdk.ui.components.*
import com.feedrail.sdk.ui.viewmodels.FeedbackListViewModel

private data class SortOption(val key: String, val labelResId: Int)

private val sortOptions = listOf(
    SortOption("newest", R.string.feedrail_feedback_list_sort_newest),
    SortOption("oldest", R.string.feedrail_feedback_list_sort_oldest),
    SortOption("top", R.string.feedrail_feedback_list_sort_top),
    SortOption("comments", R.string.feedrail_feedback_list_sort_comments),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackListScreen(
    viewModel: FeedbackListViewModel,
    onFeedbackClick: (String) -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var showSortMenu by remember { mutableStateOf(false) }

    // Trigger load more when near the end
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 3 && uiState.hasMore && !uiState.isLoadingMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    val currentSortLabel = sortOptions.firstOrNull { it.key == uiState.sortOption }
        ?.let { stringResource(it.labelResId) }
        ?: stringResource(R.string.feedrail_feedback_list_sort_newest)

    Column(modifier = modifier.fillMaxSize()) {
        // Board filter chips and Sort row (only if NOT in a global error state)
        val isErrorState = uiState.error != null && uiState.items.isEmpty()
        val isInitialLoading = (uiState.isLoading || !uiState.hasLoadedOnce) && uiState.items.isEmpty()
        
        if (!isErrorState && !isInitialLoading && (uiState.items.isNotEmpty() || uiState.boards.size > 1)) {
            // Board filter chips (only if more than 1 board)
            if (uiState.boards.size > 1) {
                BoardFilterChips(
                    boards = uiState.boards,
                    selectedSlug = uiState.selectedBoardSlug,
                    onSelect = viewModel::selectBoard,
                )
            }

            // Sort row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                // Sort pill
                Box {
                    Surface(
                        onClick = { showSortMenu = true },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentSortLabel,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        sortOptions.forEach { option ->
                            val isSelected = uiState.sortOption == option.key
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        stringResource(option.labelResId),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ) 
                                },
                                onClick = {
                                    viewModel.selectSort(option.key)
                                    showSortMenu = false
                                },
                                trailingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        // Archived project warning
        if (uiState.isProjectArchived) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.feedrail_project_archived),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                (uiState.isLoading || !uiState.hasLoadedOnce) && uiState.items.isEmpty() -> {
                    // Skeleton loading
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(6) {
                            FeedbackRowSkeleton()
                        }
                    }
                }
                uiState.error != null && uiState.items.isEmpty() -> {
                    ErrorContent(
                        message = uiState.error ?: stringResource(R.string.feedrail_general_error_unknown),
                        onRetry = viewModel::refresh,
                    )
                }
                uiState.items.isEmpty() -> {
                    EmptyStateContent(
                        title = stringResource(R.string.feedrail_feedback_list_empty_title),
                        subtitle = stringResource(R.string.feedrail_feedback_list_empty_subtitle),
                        actionLabel = if (!uiState.isProjectArchived && uiState.canSubmit) {
                            stringResource(R.string.feedrail_feedback_submit_cta)
                        } else null,
                        onAction = if (!uiState.isProjectArchived && uiState.canSubmit) {
                            onSubmitClick
                        } else null,
                    )
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(
                                items = uiState.items,
                                key = { _, item -> item.id },
                            ) { _, item ->
                                FeedbackRowItem(
                                    item = item,
                                    onClick = { onFeedbackClick(item.id) },
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

                            if (uiState.branding?.shouldShowPoweredBy == true) {
                                item {
                                    PoweredByBadge()
                                }
                            }
                        }
                    }
                }
            }

            // Authentication status indicator (if authenticating)
            if (uiState.isAuthenticating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
