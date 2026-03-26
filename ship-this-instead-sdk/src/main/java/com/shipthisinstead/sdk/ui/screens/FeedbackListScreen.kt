package com.shipthisinstead.sdk.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shipthisinstead.sdk.R
import com.shipthisinstead.sdk.ui.components.BoardFilterChips
import com.shipthisinstead.sdk.ui.components.EmptyStateContent
import com.shipthisinstead.sdk.ui.components.ErrorContent
import com.shipthisinstead.sdk.ui.components.FeedbackRowItem
import com.shipthisinstead.sdk.ui.components.FeedbackRowSkeleton
import com.shipthisinstead.sdk.ui.components.PoweredByBadge
import com.shipthisinstead.sdk.ui.viewmodels.FeedbackListViewModel

private data class SortOption(val key: String, val labelResId: Int)

private val sortOptions = listOf(
    SortOption("newest", R.string.sti_feedback_list_sort_newest),
    SortOption("oldest", R.string.sti_feedback_list_sort_oldest),
    SortOption("top", R.string.sti_feedback_list_sort_top),
    SortOption("comments", R.string.sti_feedback_list_sort_comments),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackListScreen(
    viewModel: FeedbackListViewModel,
    onFeedbackClick: (String) -> Unit,
    onSubmitClick: () -> Unit,
    onProfileClick: ((String) -> Unit)? = null,
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
        ?: stringResource(R.string.sti_feedback_list_sort_newest)

    Column(modifier = modifier.fillMaxSize()) {
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
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Box {
                TextButton(
                    onClick = { showSortMenu = true },
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = currentSortLabel,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                ) {
                    sortOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(option.labelResId)) },
                            onClick = {
                                viewModel.selectSort(option.key)
                                showSortMenu = false
                            },
                        )
                    }
                }
            }
        }

        // Archived project warning
        if (uiState.isProjectArchived) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.sti_project_archived),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        HorizontalDivider()

        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading && uiState.items.isEmpty() -> {
                    // Skeleton loading
                    Column {
                        repeat(6) {
                            FeedbackRowSkeleton()
                            HorizontalDivider()
                        }
                    }
                }
                uiState.error != null && uiState.items.isEmpty() -> {
                    ErrorContent(
                        message = uiState.error ?: stringResource(R.string.sti_general_error_unknown),
                        onRetry = viewModel::refresh,
                    )
                }
                uiState.items.isEmpty() -> {
                    EmptyStateContent(
                        title = stringResource(R.string.sti_feedback_list_empty_title),
                        subtitle = stringResource(R.string.sti_feedback_list_empty_subtitle),
                        actionLabel = if (!uiState.isProjectArchived && uiState.isAuthenticated) {
                            stringResource(R.string.sti_feedback_submit_cta)
                        } else null,
                        onAction = if (!uiState.isProjectArchived && uiState.isAuthenticated) {
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
                            contentPadding = PaddingValues(bottom = 80.dp),
                        ) {
                            itemsIndexed(
                                items = uiState.items,
                                key = { _, item -> item.id },
                            ) { _, item ->
                                FeedbackRowItem(
                                    item = item,
                                    onClick = { onFeedbackClick(item.id) },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
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

            // FAB
            if (!uiState.isProjectArchived) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                ) {
                    when {
                        uiState.isAuthenticating -> {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        }
                        uiState.isAuthenticated -> {
                            FloatingActionButton(
                                onClick = onSubmitClick,
                                containerColor = MaterialTheme.colorScheme.primary,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.sti_feedback_submit_cta),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                        // Guest: no FAB
                        else -> {}
                    }
                }
            }
        }
    }
}
