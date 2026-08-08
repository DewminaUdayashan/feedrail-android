package com.feedrail.sdk.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.feedrail.sdk.models.FeedRailBranding

@Composable
fun BrandingHeader(
    branding: FeedRailBranding?,
    title: String,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        contentAlignment = alignment,
    ) {
        val logoUrl = branding?.logoUrl
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = branding?.projectName ?: title,
                modifier = Modifier.height(28.dp),
                contentScale = ContentScale.Fit,
            )
        } else {
            Text(
                text = branding?.projectName ?: title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
