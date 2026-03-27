package com.shipthisinstead.sdk.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val STIShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val STITypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun STITheme(
    accentColor: Color = STIDefaultAccent,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val ctx = LocalContext.current
        if (darkTheme) {
            dynamicDarkColorScheme(ctx).copy(
                primary = accentColor,
                onPrimary = Color.White,
                primaryContainer = accentColor.copy(alpha = 0.2f),
                onPrimaryContainer = accentColor,
                secondary = accentColor,
                outlineVariant = accentColor.copy(alpha = 0.1f)
            )
        } else {
            dynamicLightColorScheme(ctx).copy(
                primary = accentColor,
                onPrimary = Color.White,
                primaryContainer = accentColor.copy(alpha = 0.1f),
                onPrimaryContainer = accentColor,
                secondary = accentColor,
                outlineVariant = accentColor.copy(alpha = 0.05f)
            )
        }
    } else {
        if (darkTheme) {
            darkColorScheme(
                primary = accentColor,
                onPrimary = Color.White,
                primaryContainer = accentColor.copy(alpha = 0.2f),
                onPrimaryContainer = accentColor,
                secondary = accentColor,
                surface = Color(0xFF121212),
                onSurface = Color.White,
                background = Color(0xFF000000),
                onBackground = Color.White,
                outlineVariant = accentColor.copy(alpha = 0.1f)
            )
        } else {
            lightColorScheme(
                primary = accentColor,
                onPrimary = Color.White,
                primaryContainer = accentColor.copy(alpha = 0.1f),
                onPrimaryContainer = accentColor,
                secondary = accentColor,
                surface = Color.White,
                onSurface = Color(0xFF121212),
                background = Color(0xFFF8F9FA),
                onBackground = Color(0xFF121212),
                outlineVariant = accentColor.copy(alpha = 0.05f)
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = STITypography,
        shapes = STIShapes,
        content = content,
    )
}
