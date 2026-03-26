package com.shipthisinstead.sdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
        Color(0xFF6366F1)
    }

    val background = resolvedColor.copy(alpha = 0.15f)

    Text(
        text = name,
        color = resolvedColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .background(color = background, shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
