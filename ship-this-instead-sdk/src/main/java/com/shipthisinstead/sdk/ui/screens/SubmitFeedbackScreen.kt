package com.shipthisinstead.sdk.ui.screens

import androidx.compose.ui.graphics.Color

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shipthisinstead.sdk.R
import com.shipthisinstead.sdk.models.STIBoard
import com.shipthisinstead.sdk.ui.viewmodels.SubmitFeedbackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitFeedbackScreen(
    viewModel: SubmitFeedbackViewModel,
    boards: List<STIBoard>,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var boardDropdownExpanded by remember { mutableStateOf(false) }

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
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.sti_submit_title),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Title field
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text(stringResource(R.string.sti_submit_title_label)) },
                placeholder = { Text(stringResource(R.string.sti_submit_title_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.title.isBlank() && uiState.error != null,
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                supportingText = if (uiState.title.isBlank() && uiState.error != null) {
                    { Text(stringResource(R.string.sti_submit_title_required)) }
                } else null,
            )

            // Board selection
            ExposedDropdownMenuBox(
                expanded = boardDropdownExpanded,
                onExpandedChange = { boardDropdownExpanded = it },
            ) {
                OutlinedTextField(
                    value = uiState.selectedBoard?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.sti_submit_board_label)) },
                    placeholder = { Text(stringResource(R.string.sti_submit_board_placeholder)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = boardDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true),
                    shape = MaterialTheme.shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                ExposedDropdownMenu(
                    expanded = boardDropdownExpanded,
                    onDismissRequest = { boardDropdownExpanded = false },
                ) {
                    uiState.boards.forEach { board ->
                        DropdownMenuItem(
                            text = { Text(board.name, style = MaterialTheme.typography.bodyLarge) },
                            onClick = {
                                viewModel.selectBoard(board)
                                boardDropdownExpanded = false
                            },
                        )
                    }
                }
            }

            // Description field
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::updateDescription,
                label = { Text(stringResource(R.string.sti_submit_description_label)) },
                placeholder = { Text(stringResource(R.string.sti_submit_description_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                minLines = 4,
                maxLines = 10,
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            // Attachments section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.sti_submit_attachments_label),
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
                                        contentDescription = stringResource(R.string.sti_general_remove),
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
                                    stringResource(R.string.sti_submit_add_attachment),
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
                onClick = { viewModel.submit(onSuccess) },
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
                    stringResource(R.string.sti_general_submit),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
