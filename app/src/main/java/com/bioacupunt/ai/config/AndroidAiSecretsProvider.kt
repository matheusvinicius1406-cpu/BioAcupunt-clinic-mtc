package com.bioacupunt.ai.config

import android.content.Context
import com.bioacupunt.BuildConfig
import com.bioacupunt.security.SecurePreferences

class AndroidAiSecretsProvider(
    context: Context,
    private val securePreferences: SecurePreferences = SecurePreferences(context.applicationContext)
) : AiSecretsProvider {
    // "gemini" reads/writes the dedicated field the Settings screen actually
    // uses (SecurePreferences.geminiApiKey) — a generic "ai_api_key_gemini"
    // key here was never written by anything, so the provider could never
    // see a key the user entered in Settings. Falls back to the build-time
    // key (local.properties -> secrets-gradle-plugin -> BuildConfig) when
    // the user hasn't entered one in Settings, so local dev builds work
    // without manual setup; local.defaults.properties' placeholder value
    // never gets treated as a real key.
    override suspend fun apiKeyFor(providerId: String): String? =
        if (providerId == "gemini") {
            securePreferences.geminiApiKey?.ifBlank { null } ?: buildTimeGeminiKey()
        } else {
            securePreferences.getString("ai_api_key_$providerId", null)
        }

    private fun buildTimeGeminiKey(): String? =
        BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() && it != "REPLACE_ME" }

    override suspend fun setApiKey(providerId: String, key: String) {
        if (providerId == "gemini") securePreferences.geminiApiKey = key
        else securePreferences.putString("ai_api_key_$providerId", key)
    }
}
