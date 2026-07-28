package com.feedrail.sdk.api

import com.feedrail.sdk.`public`.FeedRailConfig
import com.feedrail.sdk.data.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that injects required headers on every request:
 * - x-org-slug, x-project-slug
 * - Accept-Language (from current LanguageManager selection)
 * - Authorization: Bearer <token> (skipped for the sdk-auth endpoint itself)
 */
class AuthInterceptor(
    private val config: FeedRailConfig,
    private val tokenStorage: TokenStorage,
    private var acceptLanguage: String = "en",
) : Interceptor {

    fun updateLanguage(code: String) {
        acceptLanguage = code
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()
            .header("x-org-slug", config.organizationSlug)
            .header("x-project-slug", config.projectSlug)
            .header("Accept-Language", acceptLanguage)

        // Don't inject Bearer when creating the token
        val isSdkAuth = original.url.encodedPath.endsWith("auth/portal/sdk-auth")
        if (!isSdkAuth) {
            tokenStorage.getToken()?.let { token ->
                builder.header("Authorization", "Bearer $token")
            }
        }

        return chain.proceed(builder.build())
    }
}
