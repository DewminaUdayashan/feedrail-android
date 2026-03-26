package com.shipthisinstead.sdk.ui.components

import java.time.Instant
import java.time.format.DateTimeParseException

private val STRIP_HTML_REGEX = Regex("<[^>]+>")
private val EXTRA_SPACE_REGEX = Regex("\\s+")

/** Remove HTML tags and collapse whitespace. */
fun stripHtml(html: String): String {
    return html
        .replace(STRIP_HTML_REGEX, " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
        .replace(EXTRA_SPACE_REGEX, " ")
        .trim()
}

/** Convert an ISO-8601 timestamp string to a human-readable relative time. */
fun relativeTime(isoString: String): String {
    return try {
        val then = Instant.parse(isoString)
        val nowMillis = System.currentTimeMillis()
        val thenMillis = then.toEpochMilli()
        val diffMs = nowMillis - thenMillis

        when {
            diffMs < 0 -> "just now"
            diffMs < 60_000L -> "just now"
            diffMs < 3_600_000L -> {
                val minutes = (diffMs / 60_000L).toInt()
                if (minutes == 1) "1 minute ago" else "$minutes minutes ago"
            }
            diffMs < 86_400_000L -> {
                val hours = (diffMs / 3_600_000L).toInt()
                if (hours == 1) "1 hour ago" else "$hours hours ago"
            }
            diffMs < 2_592_000_000L -> {
                val days = (diffMs / 86_400_000L).toInt()
                if (days == 1) "1 day ago" else "$days days ago"
            }
            diffMs < 31_536_000_000L -> {
                val months = (diffMs / 2_592_000_000L).toInt()
                if (months == 1) "1 month ago" else "$months months ago"
            }
            else -> {
                val years = (diffMs / 31_536_000_000L).toInt()
                if (years == 1) "1 year ago" else "$years years ago"
            }
        }
    } catch (e: DateTimeParseException) {
        isoString
    } catch (e: Exception) {
        isoString
    }
}
