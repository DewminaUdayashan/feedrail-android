package com.shipthisinstead.sdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.shipthisinstead.sdk.ui.navigation.STITab

@Composable
fun STIFloatingToolbar(
    tabs: List<STITab>,
    currentRoute: String?,
    onTabClick: (STITab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main Navigation Bar (Pill shaped)
        Surface(
            modifier = Modifier
                .height(64.dp)
                .weight(1f, fill = false),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEach { tab ->
                    val isSelected = currentRoute == tab.route
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { onTabClick(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Separate Add Button (Rounded Square/Circle)
        Surface(
            modifier = Modifier
                .size(64.dp)
                .clickable { onAddClick() },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
