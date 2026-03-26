package com.shipthisinstead.sdk.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shipthisinstead.sdk.R
import com.shipthisinstead.sdk.models.STIBoard

@Composable
fun BoardFilterChips(
    boards: List<STIBoard>,
    selectedSlug: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = selectedSlug == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.sti_feedback_list_filter_all)) },
        )
        boards.forEach { board ->
            FilterChip(
                selected = selectedSlug == board.slug,
                onClick = { onSelect(board.slug) },
                label = { Text(board.name) },
            )
        }
    }
}
