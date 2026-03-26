package com.shipthisinstead.sdk.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
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
import com.shipthisinstead.sdk.ui.components.AvatarImage
import com.shipthisinstead.sdk.ui.components.AttachmentDisplayRow
import com.shipthisinstead.sdk.ui.components.CommentInputBar
import com.shipthisinstead.sdk.ui.components.CommentItem
import com.shipthisinstead.sdk.ui.components.ErrorContent
import com.shipthisinstead.sdk.ui.components.RichTextContent
import com.shipthisinstead.sdk.ui.components.StatusBadge
import com.shipthisinstead.sdk.ui.components.VoteButton
import com.shipthisinstead.sdk.ui.components.relativeTime
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
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.sti_general_close),
                        )
                    }
                },
            )
        },
        bottomBar = {
            when {
                uiState.isAuthenticating -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
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
                    Spacer(modifier = Modifier.height(8.dp))

                    // Title
                    Text(
                        text = feedback.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status + Vote row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val status = feedback.status
                        if (status != null) {
                            StatusBadge(name = status.name, color = status.color)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        VoteButton(
                            voteCount = feedback.voteCount,
                            hasVoted = feedback.hasVoted ?: false,
                            isEnabled = uiState.isAuthenticated && !uiState.isVoting,
                            onClick = viewModel::vote,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Author info
                    val author = feedback.author
                    if (author != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AvatarImage(
                                avatarUrl = author.avatarUrl,
                                displayName = author.displayName,
                                size = 28.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = author.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = relativeTime(feedback.createdAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Body content
                    val content = feedback.content
                    if (!content.isNullOrBlank()) {
                        RichTextContent(html = content)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Attachments
                    val attachments = feedback.attachments
                    if (!attachments.isNullOrEmpty()) {
                        Text(
                            text = stringResource(R.string.sti_feedback_detail_attachments),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AttachmentDisplayRow(
                            attachments = attachments,
                            baseUrl = "",
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Comments section header
                    val comments = feedback.comments ?: emptyList()
                    Text(
                        text = "${stringResource(R.string.sti_feedback_detail_comments)} (${comments.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (comments.isEmpty()) {
                        Text(
                            text = stringResource(R.string.sti_feedback_detail_no_comments),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        comments.forEach { comment ->
                            CommentItem(
                                comment = comment,
                                baseUrl = "",
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
