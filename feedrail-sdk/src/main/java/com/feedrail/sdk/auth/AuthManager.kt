package com.feedrail.sdk.auth

import android.content.Context
import com.feedrail.sdk.`public`.FeedRailConfig
import com.feedrail.sdk.api.FeedRailApiClient
import com.feedrail.sdk.data.TokenStorage
import com.feedrail.sdk.models.AnonymousAuthBody
import com.feedrail.sdk.models.FeedRailUser
import com.feedrail.sdk.models.SdkAuthBody
import com.feedrail.sdk.models.SdkUserData
import com.feedrail.sdk.util.DeviceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Authenticating : AuthState()
    data class Authenticated(val user: FeedRailUser?, val token: String) : AuthState()
    object Guest : AuthState()
}

/**
 * Manages end-user authentication.
 *
 * Every user gets a **persistent identity**, RevenueCat-style:
 *  - `apiKey` + `userId` present (via config or [identify]) → authenticated as that identity.
 *  - otherwise → a stable anonymous ID is generated, stored, and authenticated
 *    against `POST /auth/portal/anonymous`, so the same device keeps the same
 *    identity across launches.
 *
 * When an anonymous user is later identified, the stored anonymous ID is sent
 * along so the backend merges the guest's history into the identified account.
 */
class AuthManager(
    private val config: FeedRailConfig,
    private val tokenStorage: TokenStorage,
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    // Retain the application context only, to avoid leaking an Activity.
    private val appContext: Context = context.applicationContext

    /** Effective platform source; overridable by cross-platform wrappers (e.g. Flutter). */
    private val source: String get() = config.sourceOverride ?: "ANDROID_SDK"
    // apiClient is injected after construction to break circular dependency
    var apiClient: FeedRailApiClient? = null

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val isAuthenticated: Boolean get() = _authState.value is AuthState.Authenticated
    val isAuthenticating: Boolean get() = _authState.value is AuthState.Authenticating
    val currentToken: String? get() = (_authState.value as? AuthState.Authenticated)?.token
        ?: tokenStorage.getToken()
    val currentUser: FeedRailUser? get() = (_authState.value as? AuthState.Authenticated)?.user

    init {
        val stored = tokenStorage.getToken()
        // Show a token immediately for returning users so the UI doesn't flash
        // an unauthenticated state while we refresh in the background.
        _authState.value = if (stored != null) {
            AuthState.Authenticated(user = null, token = stored)
        } else {
            AuthState.Authenticating
        }
        scope.launch { bootstrap() }
    }

    private suspend fun bootstrap() {
        val stored = tokenStorage.getToken()
        when {
            config.apiKey != null && config.userId != null -> {
                performSdkAuth(userDataFromConfig(config.apiKey!!, config.userId!!))
            }
            stored != null -> {
                // Validate + hydrate the restored session; if it's stale, re-establish anonymously.
                val me = fetchMe()
                if (me != null) {
                    _authState.value = AuthState.Authenticated(me, stored)
                } else {
                    performAnonymousAuth()
                }
            }
            else -> performAnonymousAuth()
        }
    }

    /**
     * Identify (log in) the current user at runtime. Requires `apiKey` in the
     * configuration. Activity performed while anonymous is merged into the
     * identified account.
     */
    fun identify(
        userId: String,
        name: String? = null,
        email: String? = null,
        avatarUrl: String? = null,
        isPaidCustomer: Boolean? = null,
        productId: String? = null,
        metadata: Map<String, String>? = null,
    ) {
        val secret = config.apiKey
        if (secret == null) {
            android.util.Log.w("FeedRail", "identify() requires an apiKey in your FeedRailConfig — ignoring.")
            return
        }
        val userData = SdkUserData(
            externalId = userId,
            name = name,
            email = email,
            avatarUrl = avatarUrl,
            isPaidCustomer = isPaidCustomer,
            productId = productId,
            metadata = metadata,
        )
        scope.launch { performSdkAuth(userData) }
    }

    /** Log the current user out and revert to a fresh anonymous identity. */
    fun logout() {
        tokenStorage.clearToken()
        tokenStorage.clearAnonymousId()
        _authState.value = AuthState.Authenticating
        scope.launch { performAnonymousAuth() }
    }

    private fun userDataFromConfig(secret: String, userId: String) = SdkUserData(
        externalId = userId,
        name = config.userName,
        email = config.userEmail,
        avatarUrl = config.userAvatarUrl,
        isPaidCustomer = if (config.isPaidCustomer) true else null,
        productId = config.productId,
        metadata = config.userMetadata,
    )

    suspend fun performSdkAuth(userData: SdkUserData): Boolean {
        val client = apiClient ?: return false
        val secret = config.apiKey ?: return false

        _authState.value = AuthState.Authenticating
        return try {
            val body = SdkAuthBody(
                apiKey = secret,
                userData = userData.copy(
                    device = DeviceInfo.device(),
                    model = DeviceInfo.model(),
                    os = DeviceInfo.osName(),
                    osVersion = DeviceInfo.osVersion(),
                    appVersion = DeviceInfo.appVersion(appContext),
                    source = source,
                ),
                anonymousId = tokenStorage.getAnonymousId(),
            )
            val response = client.api.sdkAuth(body)
            val token = response.endUserToken
            if (token != null) {
                tokenStorage.saveToken(token)
                // Identity established — the anonymous account (if any) has been
                // merged server-side, so drop the local anonymous ID.
                tokenStorage.clearAnonymousId()
                _authState.value = AuthState.Authenticated(response.user ?: fetchMe(), token)
                true
            } else {
                _authState.value = AuthState.Guest
                false
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Guest
            false
        }
    }

    suspend fun performAnonymousAuth(): Boolean {
        val client = apiClient ?: return false

        _authState.value = AuthState.Authenticating
        return try {
            val anonymousId = tokenStorage.getOrCreateAnonymousId()
            val response = client.api.anonymousAuth(
                AnonymousAuthBody(
                    anonymousId = anonymousId,
                    device = DeviceInfo.device(),
                    model = DeviceInfo.model(),
                    os = DeviceInfo.osName(),
                    osVersion = DeviceInfo.osVersion(),
                    appVersion = DeviceInfo.appVersion(appContext),
                    source = source,
                )
            )
            val token = response.endUserToken
            if (token != null) {
                tokenStorage.saveToken(token)
                _authState.value = AuthState.Authenticated(response.user ?: fetchMe(), token)
                true
            } else {
                _authState.value = AuthState.Guest
                false
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Guest
            false
        }
    }

    /** Fetches the current end-user profile (`/me`); null on any failure. */
    private suspend fun fetchMe(): FeedRailUser? = try {
        apiClient?.api?.getPortalMe()
    } catch (e: Exception) {
        null
    }

    fun clearToken() {
        tokenStorage.clearToken()
        _authState.value = AuthState.Guest
    }
}
