package com.shipthisinstead.sdk.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shipthisinstead.sdk.R
import com.shipthisinstead.sdk.models.STIUserProfile
import com.shipthisinstead.sdk.ui.components.*
import com.shipthisinstead.sdk.ui.viewmodels.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    viewModel: ProfileViewModel,
    onFeedbackClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(userId) {
        viewModel.load(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.sti_profile_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.sti_general_close),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                )
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                ErrorContent(
                    message = uiState.error ?: stringResource(R.string.sti_general_error_unknown),
                    onRetry = { viewModel.load(userId) },
                    modifier = Modifier.padding(paddingValues),
                )
            }
            else -> {
                val profile = uiState.profile ?: return@Scaffold
                val activity = uiState.activity

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState()),
                ) {
                    // Profile header
                    ProfileHeader(profile = profile)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Stats row
                    ProfileStats(profile = profile)

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Tabs - Using Primary color for the row
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) },
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { 
                                Text(
                                    stringResource(R.string.sti_profile_tabs_requests),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium)
                                ) 
                            },
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { 
                                Text(
                                    stringResource(R.string.sti_profile_tabs_comments),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium)
                                ) 
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab content
                    when (selectedTab) {
                        0 -> {
                            val feedbackList = activity?.feedback ?: emptyList()
                            if (feedbackList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(48.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "No requests yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    )
                                }
                            } else {
                                feedbackList.forEach { item ->
                                    FeedbackRowItem(
                                        item = item,
                                        onClick = { onFeedbackClick(item.id) },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                        1 -> {
                            val commentList = activity?.comments ?: emptyList()
                            if (commentList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(48.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "No comments yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    commentList.forEach { comment ->
                                        CommentItem(comment = comment)
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: STIUserProfile,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            border = null
        ) {
            AvatarImage(
                avatarUrl = profile.avatarUrl,
                displayName = profile.displayName,
                size = 80.dp,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = profile.displayName,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!profile.email.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = profile.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun ProfileStats(
    profile: STIUserProfile,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        StatCard(
            count = profile.count.feedbackItems,
            label = stringResource(R.string.sti_profile_stats_requests),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            count = profile.count.comments,
            label = stringResource(R.string.sti_profile_stats_comments),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            count = profile.count.votes,
            label = stringResource(R.string.sti_profile_stats_votes),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    count: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}
