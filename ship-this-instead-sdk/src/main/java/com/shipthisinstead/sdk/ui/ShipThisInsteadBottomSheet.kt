package com.shipthisinstead.sdk.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            // Zero the sheet's own insets so we don't double-apply the
            // navigation-bar height. The Scaffold inside STINavGraph handles
            // insets explicitly (NavigationBar + contentWindowInsets = 0).
            contentWindowInsets = { WindowInsets(0) },
            dragHandle = null,
            // Do NOT constrain height on the modifier — applying fillMaxHeight
            // here interferes with ModalBottomSheet's internal offset-based
            // slide-up animation and produces a "jumpy" opening. Instead the
            // content (fillMaxSize on STINavGraph) drives the sheet height,
            // which ModalBottomSheet naturally caps at the available screen area.
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
