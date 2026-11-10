package com.feedrail.sdk.`public`

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import com.feedrail.sdk.api.AuthInterceptor
import com.feedrail.sdk.api.FeedRailApiClient
import com.feedrail.sdk.auth.AuthManager
import com.feedrail.sdk.data.BrandingStore
import com.feedrail.sdk.data.TokenStorage
import com.feedrail.sdk.localization.LanguageManager
import com.feedrail.sdk.models.FeedRailBoardsResponse
import com.feedrail.sdk.ui.FeedRailFeedbackContent
import com.feedrail.sdk.ui.FeedRailFeedbackDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

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
    private var _brandingStore: BrandingStore? = null

    /** Scope for launch-time warm-up work. Survives Activity/ViewModel lifecycles. */
    private val prefetchScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * The in-flight `/portal/boards` fetch started by [configure].
     *
     * Held so the first screen to need branding can await *this* rather than
     * issuing a second identical request. Cleared once consumed.
     */
    private var brandingPrefetch: Deferred<FeedRailBoardsResponse>? = null

    internal val brandingStore: BrandingStore?
        get() = _brandingStore

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

        // The library's own BuildConfig.DEBUG is false in every published AAR, so ask
        // whether the *host app* is debuggable instead. Release builds log nothing.
        val hostIsDebuggable =
            (appContext.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val apiClient = FeedRailApiClient.create(
            baseUrl = config.environment.resolvedBaseUrl,
            authInterceptor = authInterceptor,
            httpLogging = hostIsDebuggable,
        )
        _apiClient = apiClient

        val authManager = AuthManager(config, tokenStorage, appContext)
        authManager.apiClient = apiClient
        _authManager = authManager

        _brandingStore = BrandingStore(
            context = appContext,
            organizationSlug = config.organizationSlug,
            projectSlug = config.projectSlug,
        )
        startBrandingPrefetch()
    }

    /**
     * Warms branding and the board list at launch, so presenting the feedback UI
     * does not begin with a round-trip the user watches.
     *
     * `/portal/boards` is unauthenticated and identity-independent, which is why
     * this is safe to do at launch: it deliberately does *not* authenticate.
     * Creating the anonymous session here instead would register an end user for
     * every app launch, including the many that never open feedback — inflating
     * the audience list with people who never used the feature.
     *
     * Failure-tolerant: a launch must never be slowed or broken by this. If it
     * fails, the view model falls back to fetching normally.
     */
    private fun startBrandingPrefetch() {
        val client = _apiClient ?: return
        brandingPrefetch?.cancel()
        brandingPrefetch = prefetchScope.async {
            client.call { getBoards() }
        }
    }

    /**
     * Hands over the prefetched payload, waiting on it if it is still in flight.
     *
     * Returns `null` when there was no prefetch, or it failed or was cancelled —
     * all of which mean "fetch it yourself". Single-use: the handle is cleared so
     * a later refresh goes to the network rather than replaying a stale result.
     */
    internal suspend fun consumeBrandingPrefetch(): FeedRailBoardsResponse? {
        val task = brandingPrefetch ?: return null
        brandingPrefetch = null
        return try {
            task.await()
        } catch (e: Exception) {
            null
        }
    }

    // Deliberately no re-warm on app resume. Presenting the feedback UI always
    // hits the network (the prefetch is single-use), so staleness is already
    // corrected on every open; re-warming per resume would add a request each
    // time the user switches apps, for a saving only the second open would see.

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
     * Presents the feedback experience full-screen over the current screen.
     *
     * Present this inside a conditional block driven by your own state:
     * ```kotlin
     * if (showFeedback) {
     *     FeedRailSDK.FeedbackScreen(onDismiss = { showFeedback = false })
     * }
     * ```
     *
     * [onDismiss] fires when the user closes the experience from its root screen —
     * either with the close button or the system back gesture. Back on any screen below
     * the root navigates back within the feedback UI instead, so this is never called
     * mid-flow.
     *
     * To embed the experience as a destination in your own navigation graph rather than
     * overlaying it, use [com.feedrail.sdk.ui.FeedRailFeedbackContent] directly.
     */
    @Composable
    fun FeedbackScreen(
        onDismiss: () -> Unit = {},
        onFeedbackSubmitted: () -> Unit = {},
    ) {
        FeedRailFeedbackDialog(
            sdk = this,
            onDismiss = onDismiss,
            onFeedbackSubmitted = onFeedbackSubmitted,
        )
    }

    /**
     * The feedback experience, laid out to fill whatever space you give it.
     *
     * Use this when FeedRail is a destination in your own navigation graph. For the
     * common case of overlaying it on the current screen, use [FeedbackScreen].
     */
    @Composable
    fun FeedbackContent(
        onDismiss: () -> Unit = {},
        onFeedbackSubmitted: () -> Unit = {},
    ) {
        FeedRailFeedbackContent(
            sdk = this,
            onDismiss = onDismiss,
            onFeedbackSubmitted = onFeedbackSubmitted,
        )
    }

    @Deprecated(
        message = "The feedback UI is now a full screen rather than a bottom sheet — a " +
            "multi-screen flow with its own navigation is a task, not a modal, and as a " +
            "sheet it was drag-dismissable from every screen and swallowed the back " +
            "button. This still works and presents the new full-screen UI.",
        replaceWith = ReplaceWith("FeedbackScreen(onDismiss, onFeedbackSubmitted)"),
    )
    @Composable
    fun FeedbackSheet(
        onDismiss: () -> Unit = {},
        onFeedbackSubmitted: () -> Unit = {},
    ) {
        FeedbackScreen(
            onDismiss = onDismiss,
            onFeedbackSubmitted = onFeedbackSubmitted,
        )
    }

    /**
     * Presents the feedback UI from a non-Compose host by launching
     * [com.feedrail.sdk.ui.FeedRailActivity] over the current screen.
     *
     * This is the entry point used by cross-platform wrappers (Flutter, React
     * Native) and by plain-View apps that don't host their own Compose tree.
     * Prefer passing an `Activity` as [context] so the screen appears over it;
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
