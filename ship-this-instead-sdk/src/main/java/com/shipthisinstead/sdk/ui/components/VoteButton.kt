package com.shipthisinstead.sdk.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VoteButton(
    voteCount: Int,
    hasVoted: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val contentColor = if (hasVoted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (hasVoted) primary else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (hasVoted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = isEnabled,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = if (hasVoted) null else BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = voteCount.toString(),
                color = contentColor,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (hasVoted) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                )
            )
        }
    }
}
