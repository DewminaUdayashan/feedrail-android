package com.feedrail.sdk.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.feedrail.sdk.R
import com.feedrail.sdk.api.FeedRailApiClient
import com.feedrail.sdk.auth.AuthManager
import com.feedrail.sdk.auth.AuthState
import com.feedrail.sdk.localization.LanguageManager
import com.feedrail.sdk.models.FeedRailReportReason
import com.feedrail.sdk.models.FeedRailUser
import com.feedrail.sdk.moderation.Moderation
import com.feedrail.sdk.moderation.canModerate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Runs report/block/unblock and confirms the outcome in a snackbar.
 *
 * Lives at the navigation host, not on a screen: reporting a post from its detail
 * screen pops that screen, and a snackbar launched from the popped screen's scope
 * would be cancelled before anyone could read it. View models drop the affected
 * content themselves by listening to [Moderation.events].
 */
class ModerationActions internal constructor(
    private val api: FeedRailApiClient,
    private val authManager: AuthManager,
    private val strings: LanguageManager,
    private val scope: CoroutineScope,
    internal val snackbar: SnackbarHostState,
) {
    internal val currentUserId: String?
        get() = (authManager.authState.value as? AuthState.Authenticated)?.user?.id

    /** See [canModerate]. Also false before the reader has an identity at all. */
    internal fun canModerate(endUserAuthor: FeedRailUser?): Boolean =
        currentUserId != null && canModerate(endUserAuthor, currentUserId)

    internal fun reportFeedback(feedbackId: String, reason: FeedRailReportReason, note: String?) =
        run(strings.string(R.string.feedrail_report_success)) {
            Moderation.reportFeedback(api, feedbackId, reason, note)
        }

    internal fun reportComment(
        feedbackId: String,
        commentId: String,
        reason: FeedRailReportReason,
        note: String?,
    ) = run(strings.string(R.string.feedrail_report_success)) {
        Moderation.reportComment(api, feedbackId, commentId, reason, note)
    }

    internal fun block(user: FeedRailUser) =
        run(strings.string(R.string.feedrail_block_success, user.displayName)) {
            Moderation.blockUser(api, user.id)
        }

    internal fun unblock(userId: String, displayName: String) =
        run(strings.string(R.string.feedrail_unblock_success, displayName)) {
            Moderation.unblockUser(api, userId)
        }

    /**
     * Shows [message] in the host's snackbar from the host's own scope, so the caller
     * can clear whatever state produced it without cancelling the snackbar.
     */
    internal fun showMessage(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    private fun run(successMessage: String, block: suspend () -> Unit) {
        scope.launch {
            val message = try {
                block()
                successMessage
            } catch (e: Exception) {
                e.message ?: strings.string(R.string.feedrail_general_error_unknown)
            }
            snackbar.showSnackbar(message)
        }
    }
}

/** Null outside the SDK's own navigation host, which hides every moderation control. */
val LocalModerationActions = staticCompositionLocalOf<ModerationActions?> { null }

/** Overflow button opening [ModerationMenu]; renders nothing when [author] can't be moderated. */
@Composable
internal fun ModerationOverflowButton(
    author: FeedRailUser?,
    onReport: (FeedRailReportReason, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = LocalModerationActions.current ?: return
    if (author == null || !actions.canModerate(author)) return
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box(modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.feedrail_moderation_more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ModerationMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            author = author,
            onReport = onReport,
        )
    }
}

/**
 * "Report…" and "Block <name>" for a post or comment by [author], with the reason
 * and confirmation dialogs they lead to. Place it inside the Box it should anchor
 * to. The caller decides whether to show it at all — see [ModerationActions.canModerate].
 */
@Composable
internal fun ModerationMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    author: FeedRailUser,
    onReport: (FeedRailReportReason, String?) -> Unit,
) {
    val actions = LocalModerationActions.current ?: return
    var showReport by rememberSaveable { mutableStateOf(false) }
    var showBlock by rememberSaveable { mutableStateOf(false) }

    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.feedrail_report_action)) },
            leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
            onClick = {
                onDismissRequest()
                showReport = true
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.feedrail_block_action, author.displayName)) },
            leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null) },
            onClick = {
                onDismissRequest()
                showBlock = true
            },
        )
    }

    if (showReport) {
        ReportDialog(
            onDismiss = { showReport = false },
            onSubmit = { reason, note ->
                showReport = false
                onReport(reason, note)
            },
        )
    }

    if (showBlock) {
        AlertDialog(
            onDismissRequest = { showBlock = false },
            title = { Text(stringResource(R.string.feedrail_block_confirm_title, author.displayName)) },
            text = { Text(stringResource(R.string.feedrail_block_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showBlock = false
                    actions.block(author)
                }) {
                    Text(
                        stringResource(R.string.feedrail_block_confirm_button),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlock = false }) {
                    Text(stringResource(R.string.feedrail_general_cancel))
                }
            },
        )
    }
}

private fun reasonLabel(reason: FeedRailReportReason): Int = when (reason) {
    FeedRailReportReason.SPAM -> R.string.feedrail_report_reason_spam
    FeedRailReportReason.HARASSMENT -> R.string.feedrail_report_reason_harassment
    FeedRailReportReason.HATE -> R.string.feedrail_report_reason_hate
    FeedRailReportReason.SEXUAL -> R.string.feedrail_report_reason_sexual
    FeedRailReportReason.VIOLENCE -> R.string.feedrail_report_reason_violence
    FeedRailReportReason.OTHER -> R.string.feedrail_report_reason_other
}

@Composable
private fun ReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (FeedRailReportReason, String?) -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf<FeedRailReportReason?>(null) }
    var note by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feedrail_report_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.feedrail_report_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Column(Modifier.selectableGroup()) {
                    FeedRailReportReason.entries.forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selected == reason,
                                    onClick = { selected = reason },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selected == reason, onClick = null)
                            Text(
                                stringResource(reasonLabel(reason)),
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(Moderation.MAX_NOTE) },
                    label = { Text(stringResource(R.string.feedrail_report_note_label)) },
                    supportingText = { Text("${note.length}/${Moderation.MAX_NOTE}") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selected?.let { onSubmit(it, note) } },
                enabled = selected != null,
            ) {
                Text(stringResource(R.string.feedrail_report_submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.feedrail_general_cancel))
            }
        },
    )
}
