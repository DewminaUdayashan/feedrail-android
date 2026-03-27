package com.shipthisinstead.sdk.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shipthisinstead.sdk.R
import com.shipthisinstead.sdk.ui.components.*
import com.shipthisinstead.sdk.ui.viewmodels.FeedbackDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDetailScreen(
    feedbackId: String,
    viewModel: FeedbackDetailViewModel,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(feedbackId) {
        viewModel.load(feedbackId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.sti_feedback_detail_title),
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
        bottomBar = {
            when {
                uiState.isAuthenticating -> {
                    Surface(
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.sti_auth_signing_in),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                else -> {
                    CommentInputBar(
                        text = uiState.commentText,
                        onTextChange = viewModel::updateCommentText,
                        onSend = { viewModel.submitComment() },
                        onAttach = { /* TODO: attachment picker */ },
                        isSubmitting = uiState.isSubmittingComment,
                        enabled = uiState.isAuthenticated,
                    )
                }
            }
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
            uiState.error != null && uiState.feedback == null -> {
                ErrorContent(
                    message = uiState.error ?: stringResource(R.string.sti_general_error_unknown),
                    onRetry = { viewModel.load(feedbackId) },
                    modifier = Modifier.padding(paddingValues),
                )
            }
            else -> {
                val feedback = uiState.feedback ?: return@Scaffold

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Text(
                        text = feedback.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            lineHeight = 30.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Status + Vote row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val status = feedback.status
                        if (status != null) {
                            StatusBadge(name = status.name, color = status.color)
                        }
                        VoteButton(
                            voteCount = feedback.voteCount,
                            hasVoted = feedback.hasVoted ?: false,
                            isEnabled = uiState.isAuthenticated && !uiState.isVoting,
                            onClick = viewModel::vote,
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Author info
                    val author = feedback.author
                    if (author != null) {
                        Surface(
                            onClick = { onUserClick(author.id) },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AvatarImage(
                                    avatarUrl = author.avatarUrl,
                                    displayName = author.displayName,
                                    size = 32.dp,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = author.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    )
                                    Text(
                                        text = relativeTime(feedback.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Body content
                    val content = feedback.content
                    if (!content.isNullOrBlank()) {
                        RichTextContent(html = content)
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Attachments
                    val attachments = feedback.attachments
                    if (!attachments.isNullOrEmpty()) {
                        Text(
                            text = stringResource(R.string.sti_feedback_detail_attachments),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AttachmentDisplayRow(
                            attachments = attachments,
                            baseUrl = "",
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(24.dp))

                    // Comments section header
                    val comments = feedback.comments ?: emptyList()
                    Text(
                        text = "${stringResource(R.string.sti_feedback_detail_comments)} (${comments.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (comments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.sti_feedback_detail_no_comments),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                    } else {
                        comments.forEach { comment ->
                            CommentItem(
                                comment = comment,
                                baseUrl = "",
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}
