package com.feedrail.sdk.`public`

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import com.feedrail.sdk.api.AuthInterceptor
import com.feedrail.sdk.api.FeedRailApiClient
import com.feedrail.sdk.auth.AuthManager
import com.feedrail.sdk.data.TokenStorage
import com.feedrail.sdk.localization.LanguageManager
import com.feedrail.sdk.ui.FeedRailBottomSheet

/**
 * Main entry point for the FeedRail SDK.
 *
 * Call [configure] once during app startup (e.g., in Application.onCreate or MainActivity.onCreate).
 *
 * ```kotlin
 * FeedRailSDK.configure(
 *     config = FeedRailConfig(
 *         organizationSlug = "my-org",
 *         projectSlug = "my-project",
 *         environment = FeedRailEnvironment.Development(baseUrl = "http://10.0.2.2:3002"),
 *         apiKey = "your-sdk-key",
 *         userId = "user-123",
 *         userName = "Jane Doe",
 *     ),
 *     context = applicationContext,
 * )
 * ```
 */
object FeedRailSDK {

    private var _config: FeedRailConfig? = null
    private var _apiClient: FeedRailApiClient? = null
    private var _authManager: AuthManager? = null
    private var _languageManager: LanguageManager? = null
    private var _authInterceptor: AuthInterceptor? = null

    val config: FeedRailConfig
        get() = _config ?: error("FeedRailSDK.configure() must be called first.")

    val apiClient: FeedRailApiClient
        get() = _apiClient ?: error("FeedRailSDK.configure() must be called first.")

    val authManager: AuthManager
        get() = _authManager ?: error("FeedRailSDK.configure() must be called first.")

    val languageManager: LanguageManager
        get() = _languageManager ?: error("FeedRailSDK.configure() must be called first.")

    val authInterceptor: AuthInterceptor
        get() = _authInterceptor ?: error("FeedRailSDK.configure() must be called first.")

    /** Returns true if the SDK has been configured. */
    val isConfigured: Boolean get() = _config != null

    /**
     * Configures the SDK. Must be called before presenting the feedback sheet.
     *
     * @param config SDK configuration.
     * @param context Application context (stored as applicationContext to avoid leaks).
     */
    fun configure(config: FeedRailConfig, context: Context) {
        _config = config
        val appContext = context.applicationContext
        val tokenStorage = TokenStorage(appContext)
        val languageManager = LanguageManager(appContext, config.preferredLocale)
        _languageManager = languageManager

        val authInterceptor = AuthInterceptor(
            config = config,
            tokenStorage = tokenStorage,
            acceptLanguage = languageManager.currentCode,
        )
        _authInterceptor = authInterceptor

        val apiClient = FeedRailApiClient.create(
            baseUrl = config.environment.resolvedBaseUrl,
            authInterceptor = authInterceptor,
        )
        _apiClient = apiClient

        val authManager = AuthManager(config, tokenStorage, appContext)
        authManager.apiClient = apiClient
        _authManager = authManager
    }

    /**
     * Identify (log in) the current user at runtime. Use when the user signs in
     * after the SDK was configured. Any activity performed while anonymous is
     * merged into the identified account. Requires `apiKey` in the config.
     */
    fun identify(
        userId: String,
        userName: String? = null,
        userEmail: String? = null,
        userAvatarUrl: String? = null,
        isPaidCustomer: Boolean? = null,
        productId: String? = null,
        userMetadata: Map<String, String>? = null,
    ) {
        authManager.identify(
            userId = userId,
            name = userName,
            email = userEmail,
            avatarUrl = userAvatarUrl,
            isPaidCustomer = isPaidCustomer,
            productId = productId,
            metadata = userMetadata,
        )
    }

    /** Log the current user out. The SDK reverts to a fresh anonymous identity. */
    fun logout() {
        authManager.logout()
    }

    /**
     * Returns the feedback bottom sheet composable.
     *
     * Present this inside a conditional block driven by your own state:
     * ```kotlin
     * if (showFeedback) {
     *     FeedRailSDK.FeedbackSheet(onDismiss = { showFeedback = false })
     * }
     * ```
     */
    @Composable
    fun FeedbackSheet(
        onDismiss: () -> Unit = {},
        onFeedbackSubmitted: () -> Unit = {},
    ) {
        FeedRailBottomSheet(
            sdk = this,
            onDismiss = onDismiss,
            onFeedbackSubmitted = onFeedbackSubmitted,
        )
    }

    /**
     * Presents the feedback UI from a non-Compose host by launching a
     * transparent [com.feedrail.sdk.ui.FeedRailActivity] over the current screen.
     *
     * This is the entry point used by cross-platform wrappers (Flutter, React
     * Native) and by plain-View apps that don't host their own Compose tree.
     * Prefer passing an `Activity` as [context] so the sheet appears over it;
     * an application context works too but starts a new task.
     *
     * @throws IllegalStateException if [configure] has not been called.
     */
    fun present(context: Context) {
        check(isConfigured) { "FeedRailSDK.configure() must be called before present()." }
        val intent = Intent(context, com.feedrail.sdk.ui.FeedRailActivity::class.java)
        if (context !is android.app.Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
