package com.shipthisinstead.sdk.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shipthisinstead.sdk.`public`.ShipThisInsteadSDK
import com.shipthisinstead.sdk.ui.navigation.STINavGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShipThisInsteadBottomSheet(
    sdk: ShipThisInsteadSDK,
    onDismiss: () -> Unit,
    onFeedbackSubmitted: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // RTL support
    val langConfig by sdk.languageManager.config.collectAsStateWithLifecycle()
    val layoutDirection = if (langConfig.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    // Responsive height: full-screen on landscape, 92 % on portrait
    val configuration = LocalConfiguration.current
    val heightFraction = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1f else 0.92f

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            // Pass empty insets — the Scaffold inside STINavGraph handles its
            // own insets via contentWindowInsets, so we must not double-apply
            // the navigation-bar padding here.
            contentWindowInsets = { WindowInsets(0) },
            dragHandle = null,
            modifier = Modifier.fillMaxHeight(heightFraction),
        ) {
            STINavGraph(
                sdk = sdk,
                onDismiss = onDismiss,
                onFeedbackSubmitted = onFeedbackSubmitted,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
