package com.shipthisinstead.sdk.`public`

/**
 * Environment configuration for the ShipThisInstead SDK.
 */
sealed class STIEnvironment {
    data class Production(val baseUrl: String = "https://api.shipthisinstead.com") : STIEnvironment()
    data class Development(val baseUrl: String) : STIEnvironment()

    val resolvedBaseUrl: String
        get() = when (this) {
            is Production -> baseUrl
            is Development -> baseUrl
        }
}

/**
 * Configuration for the ShipThisInstead SDK.
 *
 * @param organizationSlug Your organization slug (used in x-org-slug header).
 * @param projectSlug Your project slug (used in x-project-slug header).
 * @param environment Production or Development environment.
 * @param ssoSecret SSO secret key for automatic user authentication.
 * @param userId Unique user identifier in your system.
 * @param userName Display name of the user.
 * @param userEmail Email address of the user.
 * @param userAvatarUrl URL for the user's avatar image.
 * @param isPaidCustomer Whether the user is a paid customer.
 * @param productId Plan/product ID associated with the user.
 * @param userMetadata Custom metadata key-value pairs (max 5 KB).
 * @param preferredLocale Force a specific language (e.g., "ar", "ja"). Defaults to device language.
 */
data class ShipThisInsteadConfig(
    val organizationSlug: String,
    val projectSlug: String,
    val environment: STIEnvironment = STIEnvironment.Production(),
    val ssoSecret: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val userEmail: String? = null,
    val userAvatarUrl: String? = null,
    val isPaidCustomer: Boolean = false,
    val productId: String? = null,
    val userMetadata: Map<String, String>? = null,
    val preferredLocale: String? = null,
)
