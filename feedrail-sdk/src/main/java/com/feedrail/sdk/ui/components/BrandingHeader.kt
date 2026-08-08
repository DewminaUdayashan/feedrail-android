package com.feedrail.sdk.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.feedrail.sdk.models.FeedRailBranding

@Composable
fun BrandingHeader(
    branding: FeedRailBranding?,
    title: String,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    /**
     * API host, used to resolve a relative [FeedRailBranding.logoUrl]. Every logo
     * uploaded through the dashboard comes back as a path (`/api/v1/uploads/logos/…`),
     * which is meaningless to an image loader on its own.
     */
    baseUrl: String = "",
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        contentAlignment = alignment,
    ) {
        val logoUrl = branding?.logoUrl
        val projectName = branding?.projectName ?: title

        // A logo that fails to load falls back to the project name. Previously the
        // presence of a logoUrl alone committed to the image, so any failure — an
        // unresolvable path, a 404, no network — rendered an empty header rather than
        // the name, and there was no way back to the text branch.
        var logoFailed by remember(logoUrl, baseUrl) { mutableStateOf(false) }

        val resolvedLogoUrl = remember(logoUrl, baseUrl) {
            when {
                logoUrl.isNullOrBlank() -> null
                logoUrl.startsWith("http") -> logoUrl
                baseUrl.isBlank() -> null
                else -> baseUrl.trimEnd('/') + "/" + logoUrl.trimStart('/')
            }
        }

        if (resolvedLogoUrl != null && !logoFailed) {
            AsyncImage(
                model = resolvedLogoUrl,
                contentDescription = projectName,
                modifier = Modifier.height(28.dp),
                contentScale = ContentScale.Fit,
                onError = { logoFailed = true },
            )
        } else {
            Text(
                text = projectName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
