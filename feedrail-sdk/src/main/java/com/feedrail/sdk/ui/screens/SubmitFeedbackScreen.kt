package com.feedrail.sdk.ui.screens

import androidx.compose.ui.graphics.Color

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feedrail.sdk.R
import com.feedrail.sdk.models.FeedRailBoard
import com.feedrail.sdk.models.FeedRailFeedbackItem
import com.feedrail.sdk.ui.viewmodels.SubmitFeedbackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitFeedbackScreen(
    viewModel: SubmitFeedbackViewModel,
    boards: List<FeedRailBoard>,
    onBack: () -> Unit,
    onSuccess: (FeedRailFeedbackItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(boards) {
        viewModel.setBoards(boards)
    }

    val attachmentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris: List<Uri> ->
        uris.forEach { uri -> viewModel.addAttachment(uri, context) }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        Text(
                            stringResource(R.string.feedrail_submit_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.feedrail_general_close),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    )
                )
                if (uiState.isSubmitting) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                }
            }
        },
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                // Hosts are usually edge-to-edge, where adjustResize no longer shrinks
                // the window; without this the keyboard covers the submit button.
                .imePadding()
                // Tapping anywhere outside a field dismisses the keyboard.
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Title field
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text(stringResource(R.string.feedrail_submit_title_label)) },
                placeholder = { Text(stringResource(R.string.feedrail_submit_title_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                isError = uiState.title.isBlank() && uiState.error != null,
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                supportingText = if (uiState.title.isBlank() && uiState.error != null) {
                    { Text(stringResource(R.string.feedrail_submit_title_required)) }
                } else null,
            )

            // Board selection: every board as a chip, all visible at once. People
            // don't know what a "board" is, but they recognise "Bug Reports" vs
            // "Feature Requests" when they see them side by side. Hidden when there
            // is only one board, since the view model picks it automatically.
            if (uiState.boards.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.feedrail_submit_board_label),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        uiState.boards.forEach { board ->
                            val isSelected = uiState.selectedBoard?.id == board.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.selectBoard(board)
                                },
                                label = { Text(board.name) },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                                        )
                                    } else {
                                        Surface(
                                            modifier = Modifier.size(10.dp),
                                            shape = CircleShape,
                                            color = board.color.toComposeColor(),
                                        ) {}
                                    }
                                },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.semantics { role = Role.RadioButton },
                            )
                        }
                    }
                }
            }

            // Description field
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::updateDescription,
                label = { Text(stringResource(R.string.feedrail_submit_description_label)) },
                placeholder = { Text(stringResource(R.string.feedrail_submit_description_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                minLines = 4,
                maxLines = 10,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            // Attachments section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.feedrail_submit_attachments_label),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.stagedAttachments.forEach { attachment ->
                        InputChip(
                            selected = false,
                            onClick = {},
                            shape = MaterialTheme.shapes.medium,
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    if (attachment.isImage) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    Text(
                                        text = attachment.fileName,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (attachment.isUploading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 1.5.dp,
                                        )
                                    }
                                }
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { viewModel.removeAttachment(attachment.localUri) },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.feedrail_general_remove),
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            },
                        )
                    }

                    if (uiState.stagedAttachments.size < 5) {
                        AssistChip(
                            onClick = { attachmentPickerLauncher.launch("*/*") },
                            shape = MaterialTheme.shapes.medium,
                            label = {
                                Text(
                                    stringResource(R.string.feedrail_submit_add_attachment),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = MaterialTheme.colorScheme.primary,
                                leadingIconContentColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // Error message
            val error = uiState.error
            if (error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Submit button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.submit(onSuccess)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState.canSubmit,
                shape = MaterialTheme.shapes.large,
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    stringResource(R.string.feedrail_general_submit),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun String?.toComposeColor(): Color =
    runCatching { Color(android.graphics.Color.parseColor(this ?: "#CCCCCC")) }
        .getOrDefault(Color(0xFFCCCCCC))
