package com.shipthisinstead.sdk.ui.theme

import androidx.compose.ui.graphics.Color

val STIDefaultAccent = Color(0xFF6366F1)

/** Parse a CSS hex string (#RGB, #RRGGBB, #AARRGGBB) into a Compose Color. */
fun Color(hex: String): Color {
    val clean = hex.trimStart('#')
    return when (clean.length) {
        3 -> {
            val r = clean[0].toString().repeat(2)
            val g = clean[1].toString().repeat(2)
            val b = clean[2].toString().repeat(2)
            Color(android.graphics.Color.parseColor("#$r$g$b"))
        }
        6 -> Color(android.graphics.Color.parseColor("#$clean"))
        8 -> Color(android.graphics.Color.parseColor("#$clean"))
        else -> STIDefaultAccent
    }
}
