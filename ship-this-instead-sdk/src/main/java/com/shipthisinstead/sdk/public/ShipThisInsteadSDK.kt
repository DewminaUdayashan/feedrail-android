package com.shipthisinstead.sdk.`public`

import android.content.Context
import androidx.compose.runtime.Composable
import com.shipthisinstead.sdk.api.AuthInterceptor
import com.shipthisinstead.sdk.api.STIApiClient
import com.shipthisinstead.sdk.auth.AuthManager
import com.shipthisinstead.sdk.data.TokenStorage
import com.shipthisinstead.sdk.localization.LanguageManager
import com.shipthisinstead.sdk.ui.ShipThisInsteadBottomSheet

/**
 * Main entry point for the ShipThisInstead SDK.
 *
 * Call [configure] once during app startup (e.g., in Application.onCreate or MainActivity.onCreate).
 *
 * ```kotlin
 * ShipThisInsteadSDK.configure(
 *     config = ShipThisInsteadConfig(
 *         organizationSlug = "my-org",
 *         projectSlug = "my-project",
 *         environment = STIEnvironment.Development(baseUrl = "http://10.0.2.2:3002"),
 *         ssoSecret = "your-secret",
 *         userId = "user-123",
 *         userName = "Jane Doe",
 *     ),
 *     context = applicationContext,
 * )
 * ```
 */
object ShipThisInsteadSDK {

    private var _config: ShipThisInsteadConfig? = null
    private var _apiClient: STIApiClient? = null
    private var _authManager: AuthManager? = null
    private var _languageManager: LanguageManager? = null
    private var _authInterceptor: AuthInterceptor? = null

    val config: ShipThisInsteadConfig
        get() = _config ?: error("ShipThisInsteadSDK.configure() must be called first.")

    val apiClient: STIApiClient
        get() = _apiClient ?: error("ShipThisInsteadSDK.configure() must be called first.")

    val authManager: AuthManager
        get() = _authManager ?: error("ShipThisInsteadSDK.configure() must be called first.")

    val languageManager: LanguageManager
        get() = _languageManager ?: error("ShipThisInsteadSDK.configure() must be called first.")

    val authInterceptor: AuthInterceptor
        get() = _authInterceptor ?: error("ShipThisInsteadSDK.configure() must be called first.")

    /** Returns true if the SDK has been configured. */
    val isConfigured: Boolean get() = _config != null

    /**
     * Configures the SDK. Must be called before presenting the feedback sheet.
     *
     * @param config SDK configuration.
     * @param context Application context (stored as applicationContext to avoid leaks).
     */
    fun configure(config: ShipThisInsteadConfig, context: Context) {
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

        val apiClient = STIApiClient.create(
            baseUrl = config.environment.resolvedBaseUrl,
            authInterceptor = authInterceptor,
        )
        _apiClient = apiClient

        val authManager = AuthManager(config, tokenStorage)
        authManager.apiClient = apiClient
        _authManager = authManager
    }

    /**
     * Returns the feedback bottom sheet composable.
     *
     * Present this inside a conditional block driven by your own state:
     * ```kotlin
     * if (showFeedback) {
     *     ShipThisInsteadSDK.FeedbackSheet(onDismiss = { showFeedback = false })
     * }
     * ```
     */
    @Composable
    fun FeedbackSheet(
        onDismiss: () -> Unit = {},
        onFeedbackSubmitted: () -> Unit = {},
    ) {
        ShipThisInsteadBottomSheet(
            sdk = this,
            onDismiss = onDismiss,
            onFeedbackSubmitted = onFeedbackSubmitted,
        )
    }
}
