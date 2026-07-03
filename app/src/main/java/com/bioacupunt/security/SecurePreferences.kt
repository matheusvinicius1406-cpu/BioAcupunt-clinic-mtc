package com.bioacupunt.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object SecurePreferences {
    private const val FILE = "bio_secure_prefs"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        prefs = EncryptedSharedPreferences.create(
            FILE,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun get(scope: (SharedPreferences.Editor) -> Unit) {
        check(::prefs.isInitialized) { "SecurePreferences not initialized" }
        prefs.edit().also(scope).apply()
    }

    fun read(default: String = "", key: String = ""): String =
        if (key.isBlank()) default else prefs.getString(key, default) ?: default

    var String?.authToken
        get() = this ?: ""
        set(value) = if (value.isNullOrBlank()) get { it.remove("auth_token") } else get { it.putString("auth_token", value) }
    var String?.refreshToken
        get() = this ?: ""
        set(value) = if (value.isNullOrBlank()) get { it.remove("refresh_token") } else get { it.putString("refresh_token", value) }
    var Long?.userId: Long
        get() = this ?: 0L
        set(value) = get { it.putLong("user_id", value) }
    var String?.userEmail
        get() = this ?: ""
        set(value) = if (value.isNullOrBlank()) get { it.remove("user_email") } else get { it.putString("user_email", value) }
    var Boolean?.isLoggedIn: Boolean
        get() = this ?: false
        set(value) = get { it.putBoolean("is_logged_in", value) }
    var String?.biometricEnabled
        get() = this ?: ""
        set(value) = if (value.isNullOrBlank()) get { it.remove("biometric_enabled") } else get { it.putString("biometric_enabled", value) }
    var Boolean?.hasOnboarded: Boolean
        get() = this ?: false
        set(value) = get { it.putBoolean("has_onboarded", value) }
    var String?.biometricPassword
        get() = this ?: ""
        set(value) = if (value.isBlank()) get { it.remove("biometric_password") } else get { it.putString("biometric_password", value) }

    fun clearAll() {
        get { it.clear() }
    }
}
