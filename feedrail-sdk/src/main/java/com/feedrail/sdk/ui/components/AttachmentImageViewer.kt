package com.feedrail.sdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.feedrail.sdk.R
import com.feedrail.sdk.models.FeedRailAttachment

/**
 * Full-screen, zoomable viewer for image attachments.
 *
 * Replaces launching an `ACTION_VIEW` intent, which kicked the user out to a browser
 * to look at what is usually a screenshot attached to their own feedback. Leaving the
 * app for that is jarring, and it also handed a short-lived signed attachment URL to
 * the browser, where it lingers in history well after it stops working.
 *
 * Swipes between every image in the same attachment set and supports pinch-to-zoom,
 * since these are typically UI screenshots where the detail is the point.
 */
@Composable
internal fun AttachmentImageViewer(
    images: List<FeedRailAttachment>,
    baseUrl: String,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    if (images.isEmpty()) return

    Dialog(
        onDismissRequest = onDismiss,
        // Without this the dialog is inset to the platform's default width and the
        // image can't actually fill the screen.
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val pagerState = rememberPagerState(
            initialPage = initialIndex.coerceIn(0, images.lastIndex),
            pageCount = { images.size },
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalPager(state = pagerState) { page ->
                ZoomableImage(
                    url = resolveAttachmentUrl(images[page], baseUrl),
                    contentDescription = images[page].fileName,
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.feedrail_general_close),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ZoomableImage(url: String, contentDescription: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                    // Only pan while zoomed in; at 1x a drag should stay with the
                    // pager so swiping between images keeps working.
                    offset = if (scale > MIN_SCALE) offset + pan else Offset.Zero
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        )
    }
}

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f

/**
 * Attachment URLs come back from the API as root-relative paths carrying a
 * short-lived `exp`/`sig` capability token. Concatenating preserves that query
 * string — dropping it gets the request rejected with a 403.
 */
internal fun resolveAttachmentUrl(attachment: FeedRailAttachment, baseUrl: String): String =
    if (attachment.url.startsWith("http")) attachment.url else "$baseUrl${attachment.url}"
