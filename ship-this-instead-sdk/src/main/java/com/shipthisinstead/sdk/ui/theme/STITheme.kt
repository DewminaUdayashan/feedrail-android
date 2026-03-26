package com.shipthisinstead.sdk.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun STITheme(
    accentColor: Color = STIDefaultAccent,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val ctx = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(ctx).copy(primary = accentColor)
        else dynamicLightColorScheme(ctx).copy(primary = accentColor)
    } else {
        if (darkTheme) {
            darkColorScheme(
                primary = accentColor,
                secondary = accentColor.copy(alpha = 0.7f),
            )
        } else {
            lightColorScheme(
                primary = accentColor,
                secondary = accentColor.copy(alpha = 0.7f),
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
