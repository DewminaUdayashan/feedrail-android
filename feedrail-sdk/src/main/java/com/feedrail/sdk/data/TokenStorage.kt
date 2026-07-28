package com.feedrail.sdk.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val PREFS_FILE = "feedrail_secure_prefs"
private const val KEY_TOKEN = "feedrail_end_user_token"
private const val KEY_ANONYMOUS_ID = "feedrail_anonymous_id"

/** Secure JWT token storage backed by EncryptedSharedPreferences. */
class TokenStorage(context: Context) {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveToken(token: String) = prefs.edit().putString(KEY_TOKEN, token).apply()

    fun clearToken() = prefs.edit().remove(KEY_TOKEN).apply()

    // ── Anonymous identity ──────────────────────────────────────────────────

    /** The stable device-scoped anonymous ID, or null if none has been created. */
    fun getAnonymousId(): String? = prefs.getString(KEY_ANONYMOUS_ID, null)

    fun saveAnonymousId(id: String) = prefs.edit().putString(KEY_ANONYMOUS_ID, id).apply()

    fun clearAnonymousId() = prefs.edit().remove(KEY_ANONYMOUS_ID).apply()

    /** Returns the existing anonymous ID or generates, stores and returns a new one. */
    fun getOrCreateAnonymousId(): String =
        getAnonymousId() ?: ("anon_" + java.util.UUID.randomUUID().toString().lowercase()).also(::saveAnonymousId)
}
