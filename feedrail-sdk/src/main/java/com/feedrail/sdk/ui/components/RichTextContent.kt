package com.feedrail.sdk.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private val HTML_TAG_REGEX = Regex("<[^>]+>")

fun hasHtmlTags(text: String): Boolean = HTML_TAG_REGEX.containsMatchIn(text)

private fun composeColorToHex(color: androidx.compose.ui.graphics.Color): String {
    val argb = color.toArgb()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return "#%02x%02x%02x".format(r, g, b)
}

@Composable
fun RichTextContent(
    html: String,
    modifier: Modifier = Modifier,
) {
    if (!hasHtmlTags(html)) {
        Text(
            text = html,
            modifier = modifier,
            fontSize = 14.sp,
        )
        return
    }

    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) "#e5e7eb" else "#111827"
    val bgColor = if (isDark) "#1f2937" else "#ffffff"
    val linkColor = composeColorToHex(MaterialTheme.colorScheme.primary)
    val codeBg = if (isDark) "#374151" else "#f3f4f6"
    val blockquoteBorder = if (isDark) "#4b5563" else "#d1d5db"
    val mutedColor = if (isDark) "#9ca3af" else "#6b7280"

    val htmlDocument = buildString {
        append("<!DOCTYPE html><html><head>")
        append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
        append("<style>")
        append("* { box-sizing: border-box; }")
        append("body { color: $textColor; background: $bgColor; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; font-size: 14px; line-height: 1.6; padding: 0; margin: 0; word-break: break-word; }")
        append("a { color: $linkColor; text-decoration: none; }")
        append("p { margin: 0 0 8px 0; }")
        append("ul, ol { margin: 0 0 8px 0; padding-left: 20px; }")
        append("li { margin-bottom: 4px; }")
        append("code { background: $codeBg; padding: 2px 4px; border-radius: 3px; font-size: 12px; }")
        append("pre { background: $codeBg; padding: 12px; border-radius: 6px; overflow-x: auto; }")
        append("blockquote { border-left: 3px solid $blockquoteBorder; margin: 0 0 8px 0; padding-left: 12px; color: $mutedColor; }")
        append("img { max-width: 100%; height: auto; }")
        append("h1, h2, h3, h4, h5, h6 { margin: 0 0 8px 0; }")
        append("</style></head><body>")
        append(html)
        append("</body></html>")
    }

    var currentHtml by remember { mutableStateOf(htmlDocument) }
    currentHtml = htmlDocument

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webViewClient = WebViewClient()
                loadDataWithBaseURL(null, htmlDocument, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, currentHtml, "text/html", "UTF-8", null)
        },
        modifier = modifier,
    )
}
