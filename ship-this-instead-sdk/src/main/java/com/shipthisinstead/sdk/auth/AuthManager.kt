package com.shipthisinstead.sdk.auth

import com.shipthisinstead.sdk.`public`.ShipThisInsteadConfig
import com.shipthisinstead.sdk.api.STIApiClient
import com.shipthisinstead.sdk.data.TokenStorage
import com.shipthisinstead.sdk.models.STIAuthResponse
import com.shipthisinstead.sdk.models.STIUser
import com.shipthisinstead.sdk.models.SdkAuthBody
import com.shipthisinstead.sdk.models.SdkUserData
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
    data class Authenticated(val user: STIUser?, val token: String) : AuthState()
    object Guest : AuthState()
}

class AuthManager(
    private val config: ShipThisInsteadConfig,
    private val tokenStorage: TokenStorage,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    // apiClient is injected after construction to break circular dependency
    var apiClient: STIApiClient? = null

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val isAuthenticated: Boolean get() = _authState.value is AuthState.Authenticated
    val isAuthenticating: Boolean get() = _authState.value is AuthState.Authenticating
    val currentToken: String? get() = (_authState.value as? AuthState.Authenticated)?.token
        ?: tokenStorage.getToken()
    val currentUser: STIUser? get() = (_authState.value as? AuthState.Authenticated)?.user

    init {
        val stored = tokenStorage.getToken()
        if (config.ssoSecret != null && config.userId != null) {
            _authState.value = AuthState.Authenticating
            scope.launch { performSdkAuth() }
        } else if (stored != null) {
            _authState.value = AuthState.Authenticated(user = null, token = stored)
        } else {
            _authState.value = AuthState.Guest
        }
    }

    suspend fun performSdkAuth(): Boolean {
        val client = apiClient ?: return false
        val secret = config.ssoSecret ?: return false
        val userId = config.userId ?: return false

        _authState.value = AuthState.Authenticating
        return try {
            val body = SdkAuthBody(
                secretKey = secret,
                userData = SdkUserData(
                    externalId = userId,
                    name = config.userName,
                    email = config.userEmail,
                    avatarUrl = config.userAvatarUrl,
                    isPaidCustomer = if (config.isPaidCustomer) true else null,
                    productId = config.productId,
                    metadata = config.userMetadata,
                ),
            )
            val response: STIAuthResponse = client.api.sdkAuth(body)
            val token = response.endUserToken
            if (token != null) {
                tokenStorage.saveToken(token)
                _authState.value = AuthState.Authenticated(response.user, token)
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

    fun clearToken() {
        tokenStorage.clearToken()
        _authState.value = AuthState.Guest
    }
}
