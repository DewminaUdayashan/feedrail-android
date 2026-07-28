package com.feedrail.sdk.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.feedrail.sdk.R
import com.feedrail.sdk.models.FeedRailBoard

@Composable
fun BoardFilterChips(
    boards: List<FeedRailBoard>,
    selectedSlug: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(8.dp)) // Lead spacing
        
        FilterChip(
            selected = selectedSlug == null,
            onClick = { onSelect(null) },
            label = { 
                Text(
                    text = stringResource(R.string.feedrail_feedback_list_filter_all),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                ) 
            },
            shape = MaterialTheme.shapes.small,
        )
        
        boards.forEach { board ->
            FilterChip(
                selected = selectedSlug == board.slug,
                onClick = { onSelect(board.slug) },
                label = { 
                    Text(
                        text = board.name,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (selectedSlug == board.slug) FontWeight.Bold else FontWeight.Medium)
                    ) 
                },
                shape = MaterialTheme.shapes.small,
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp)) // Trail spacing
    }
}
