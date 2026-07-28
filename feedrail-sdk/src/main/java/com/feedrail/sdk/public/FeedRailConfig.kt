package com.feedrail.sdk.`public`

import androidx.compose.ui.graphics.Color

/**
 * Environment configuration for the FeedRail SDK.
 */
sealed class FeedRailEnvironment {
    data class Production(val baseUrl: String = "https://api.feedrail.ai") : FeedRailEnvironment()
    data class Development(val baseUrl: String) : FeedRailEnvironment()

    val resolvedBaseUrl: String
        get() = when (this) {
            is Production -> baseUrl
            is Development -> baseUrl
        }
}

/**
 * Configuration for the FeedRail SDK.
 *
 * @param organizationSlug Your organization slug (used in x-org-slug header).
 * @param projectSlug Your project slug (used in x-project-slug header).
 * @param environment Production or Development environment.
 * @param apiKey Project-scoped SDK key for automatic user authentication.
 * @param userId Unique user identifier in your system.
 * @param userName Display name of the user.
 * @param userEmail Email address of the user.
 * @param userAvatarUrl URL for the user's avatar image.
 * @param isPaidCustomer Whether the user is a paid customer.
 * @param productId Plan/product ID associated with the user.
 * @param userMetadata Custom metadata key-value pairs (max 5 KB).
 * @param preferredLocale Force a specific language (e.g., "ar", "ja"). Defaults to device language.
 * @param themeColor Use your app's own native theme color instead of the dashboard/default
 *   FeedRail brand color. Only takes effect on plans with branding customization enabled —
 *   the SDK verifies this server-side on every load, so it's always safe to pass even if the
 *   org's plan doesn't support it (silently ignored in that case).
 * @param sourceOverride Overrides the platform source attributed to users on auth. Defaults to
 *   "ANDROID_SDK" when null. Cross-platform wrappers set this (e.g. "FLUTTER_PLUGIN") so users
 *   are tagged with the wrapper instead of the raw native SDK. Must be one of the values the
 *   backend accepts: "WEB", "IOS_SDK", "ANDROID_SDK", "FLUTTER_PLUGIN", "RN_PLUGIN".
 */
data class FeedRailConfig(
    val organizationSlug: String,
    val projectSlug: String,
    val environment: FeedRailEnvironment = FeedRailEnvironment.Production(),
    val apiKey: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val userEmail: String? = null,
    val userAvatarUrl: String? = null,
    val isPaidCustomer: Boolean = false,
    val productId: String? = null,
    val userMetadata: Map<String, String>? = null,
    val preferredLocale: String? = null,
    val themeColor: Color? = null,
    val sourceOverride: String? = null,
)
