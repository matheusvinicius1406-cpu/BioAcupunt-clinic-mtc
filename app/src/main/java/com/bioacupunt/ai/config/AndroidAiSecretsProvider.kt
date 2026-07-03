package com.bioacupunt.ai.config

import android.content.Context
import com.bioacupunt.security.SecurePreferences

class AndroidAiSecretsProvider(
    context: Context,
    private val securePreferences: SecurePreferences = SecurePreferences(context.applicationContext)
) : AiSecretsProvider {
    override suspend fun apiKeyFor(providerId: String): String? =
        securePreferences.getString("ai_api_key_$providerId", null)

    override suspend fun setApiKey(providerId: String, key: String) {
        securePreferences.putString("ai_api_key_$providerId", key)
    }
}
