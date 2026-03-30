package com.shipthisinstead.sdk.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shipthisinstead.sdk.ui.theme.Color as STIColor

@Composable
fun StatusBadge(
    name: String,
    color: String,
    modifier: Modifier = Modifier,
) {
    val resolvedColor = try {
        STIColor(color)
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        color = resolvedColor.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier
    ) {
        Text(
            text = name.uppercase(),
            color = resolvedColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
