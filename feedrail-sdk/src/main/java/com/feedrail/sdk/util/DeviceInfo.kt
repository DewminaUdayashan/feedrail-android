package com.feedrail.sdk.util

import android.content.Context
import android.os.Build

/**
 * Collects device + platform metadata sent to the backend on auth, so the org
 * dashboard can attribute each user to their real device/platform instead of WEB.
 */
internal object DeviceInfo {
    fun device(): String = Build.MANUFACTURER

    fun model(): String = Build.MODEL

    fun osName(): String = "Android"

    fun osVersion(): String = Build.VERSION.RELEASE

    fun appVersion(context: Context): String? = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) {
        null
    }
}
