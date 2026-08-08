package com.feedrail.sdk.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feedrail.sdk.`public`.FeedRailSDK
import com.feedrail.sdk.ui.navigation.FeedRailNavGraph

/**
 * The feedback experience as a full-screen destination.
 *
 * This used to be a [androidx.compose.material3.ModalBottomSheet] wrapping the whole
 * navigation graph, which made every screen inside it drag-dismissable: a stray downward
 * swipe on the profile, the detail view or the submit form tore down the entire back
 * stack, and the system back button did the same because the sheet consumed it before
 * navigation ever saw it. A multi-screen flow with its own bottom navigation is a task,
 * not a modal, so it now occupies a screen and behaves like one. Bottom sheets are still
 * used inside the flow for what they are good at — short, single-purpose choices such as
 * the sort picker.
 *
 * Host this directly in your own navigation graph, or use [FeedRailFeedbackDialog] to
 * overlay it on the current screen without adding a destination.
 */
@Composable
fun FeedRailFeedbackContent(
    sdk: FeedRailSDK,
    onDismiss: () -> Unit,
    onFeedbackSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // RTL support
    val langConfig by sdk.languageManager.config.collectAsStateWithLifecycle()
    val layoutDirection = if (langConfig.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        FeedRailNavGraph(
            sdk = sdk,
            onDismiss = onDismiss,
            onFeedbackSubmitted = onFeedbackSubmitted,
            modifier = modifier.fillMaxSize(),
        )
    }
}

/**
 * [FeedRailFeedbackContent] presented over the current screen as a full-screen dialog.
 *
 * This is the drop-in replacement for the old bottom-sheet entry point: hosts that drive
 * presentation from their own boolean state keep working unchanged, they just get a
 * full-screen surface instead of a sheet.
 */
@Composable
fun FeedRailFeedbackDialog(
    sdk: FeedRailSDK,
    onDismiss: () -> Unit,
    onFeedbackSubmitted: () -> Unit,
) {
    Dialog(
        // Back inside the dialog is handled by the navigation graph, which pops to the
        // feedback list before it ever reaches dismissal. This callback only fires once
        // the graph is already at its root.
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // Lay out behind the system bars. The Compose tree applies the insets
            // itself, and without this the screen stopped short of them.
            decorFitsSystemWindows = false,
        ),
    ) {
        // A dialog window also dims what is behind it, which is right for a dialog and
        // wrong for something meant to read as a screen — it tinted the system bars
        // grey. Nothing shows through anyway, so drop the dim and fill the window.
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.apply {
                setDimAmount(0f)
                // The default dialog background is drawn outside the content and shows
                // as a dark band in the navigation-bar area.
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        }

        FeedRailFeedbackContent(
            sdk = sdk,
            onDismiss = onDismiss,
            onFeedbackSubmitted = onFeedbackSubmitted,
        )
    }
}
