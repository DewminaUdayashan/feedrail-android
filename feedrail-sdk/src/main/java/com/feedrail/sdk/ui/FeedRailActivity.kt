package com.feedrail.sdk.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.feedrail.sdk.`public`.FeedRailSDK

/**
 * Transparent host activity that presents the FeedRail feedback bottom sheet
 * over the calling screen.
 *
 * This gives non-Compose hosts (plain Views, Flutter, React Native, …) a simple,
 * Compose-free way to show the feedback UI via [FeedRailSDK.present]. The
 * activity finishes itself as soon as the sheet is dismissed.
 */
class FeedRailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!FeedRailSDK.isConfigured) {
            finish()
            return
        }

        setContent {
            MaterialTheme {
                var visible by remember { mutableStateOf(true) }
                if (visible) {
                    FeedRailSDK.FeedbackSheet(
                        onDismiss = {
                            visible = false
                            finish()
                        },
                    )
                }
            }
        }
    }
}
