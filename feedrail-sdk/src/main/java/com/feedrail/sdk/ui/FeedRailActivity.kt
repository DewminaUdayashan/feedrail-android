package com.feedrail.sdk.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.feedrail.sdk.`public`.FeedRailSDK

/**
 * Host activity that presents the FeedRail feedback experience over the calling screen.
 *
 * This gives non-Compose hosts (plain Views, Flutter, React Native, …) a simple,
 * Compose-free way to show the feedback UI via [FeedRailSDK.present]. The activity
 * finishes itself as soon as the experience is dismissed.
 *
 * It used to be translucent so a bottom sheet could float over the host screen. The
 * feedback UI is now a full screen, so the activity is opaque and draws edge to edge;
 * the platform's own open/close transition and predictive back gesture then apply,
 * which is what makes it feel like part of the host app rather than a trapped overlay.
 */
class FeedRailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!FeedRailSDK.isConfigured) {
            finish()
            return
        }

        enableEdgeToEdge()

        setContent {
            FeedRailFeedbackContent(
                sdk = FeedRailSDK,
                onDismiss = { finish() },
                onFeedbackSubmitted = {},
            )
        }
    }
}
