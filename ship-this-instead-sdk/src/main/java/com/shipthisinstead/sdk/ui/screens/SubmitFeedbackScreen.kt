package com.shipthisinstead.sdk.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
                title = { Text(stringResource(R.string.sti_submit_title)) },
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
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                        .menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = boardDropdownExpanded,
                    onDismissRequest = { boardDropdownExpanded = false },
                ) {
                    uiState.boards.forEach { board ->
                        DropdownMenuItem(
                            text = { Text(board.name) },
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
                    .height(140.dp),
                minLines = 4,
                maxLines = 8,
            )

            // Attachments section
            Text(
                text = stringResource(R.string.sti_submit_attachments_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                uiState.stagedAttachments.forEach { attachment ->
                    InputChip(
                        selected = false,
                        onClick = {},
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (attachment.isImage) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                                Text(
                                    text = attachment.fileName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (attachment.isUploading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.5.dp,
                                    )
                                }
                            }
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { viewModel.removeAttachment(attachment.localUri) },
                                modifier = Modifier.size(16.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.sti_general_remove),
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        },
                    )
                }

                if (uiState.stagedAttachments.size < 5) {
                    FilterChip(
                        selected = false,
                        onClick = { attachmentPickerLauncher.launch("*/*") },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(stringResource(R.string.sti_submit_add_attachment))
                            }
                        },
                    )
                }
            }

            // Error message
            val error = uiState.error
            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Submit button
            Button(
                onClick = { viewModel.submit(onSuccess) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canSubmit,
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.sti_general_submit))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
