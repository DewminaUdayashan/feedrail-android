package com.feedrail.sdk.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.feedrail.sdk.models.FeedRailAttachment

@Composable
fun AttachmentDisplayRow(
    attachments: List<FeedRailAttachment>,
    baseUrl: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val images = attachments.filter { it.isImage }
    var viewingImageIndex by remember { mutableStateOf<Int?>(null) }

    viewingImageIndex?.let { index ->
        AttachmentImageViewer(
            images = images,
            baseUrl = baseUrl,
            initialIndex = index,
            onDismiss = { viewingImageIndex = null },
        )
    }

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        attachments.forEach { attachment ->
            val resolvedUrl = resolveAttachmentUrl(attachment, baseUrl)

            if (attachment.isImage) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .size(72.dp)
                        .clickable {
                            // Index within `images`, not `attachments` — the viewer
                            // only pages through images, so a mixed list would
                            // otherwise open the wrong one.
                            viewingImageIndex = images.indexOf(attachment)
                        },
                    tonalElevation = 1.dp,
                ) {
                    AsyncImage(
                        model = resolvedUrl,
                        contentDescription = attachment.fileName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(72.dp),
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resolvedUrl))
                            context.startActivity(intent)
                        },
                    tonalElevation = 1.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = attachment.fileName,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
