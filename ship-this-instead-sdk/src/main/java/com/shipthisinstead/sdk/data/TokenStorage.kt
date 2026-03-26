package com.shipthisinstead.sdk.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val PREFS_FILE = "sti_secure_prefs"
private const val KEY_TOKEN = "sti_end_user_token"

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
}
