package com.shipthisinstead.sdk.ui

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shipthisinstead.sdk.`public`.ShipThisInsteadSDK
import com.shipthisinstead.sdk.ui.navigation.STINavGraph
import com.shipthisinstead.sdk.ui.theme.STITheme
import com.shipthisinstead.sdk.ui.theme.Color as STIColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShipThisInsteadBottomSheet(
    sdk: ShipThisInsteadSDK,
    onDismiss: () -> Unit,
    onFeedbackSubmitted: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Observe language config for RTL direction
    val langConfig by sdk.languageManager.config.collectAsStateWithLifecycle()
    val layoutDirection = if (langConfig.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    // Parse accent color from the branding (we use the default until branding loads)
    // This is set early with a sensible default; it will recompose once FeedbackListVM loads branding
    val accentColor = STIColor("#6366F1")

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
    ) {
        STITheme(accentColor = accentColor) {
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = sheetState,
                containerColor = Color.Unspecified,
                modifier = Modifier.fillMaxHeight(0.92f),
                dragHandle = null,
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
}
