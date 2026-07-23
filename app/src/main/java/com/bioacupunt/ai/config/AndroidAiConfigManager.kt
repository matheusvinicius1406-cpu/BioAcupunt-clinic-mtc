package com.bioacupunt.ai.config

import android.content.Context
import com.bioacupunt.ai.core.AiCapability
import com.bioacupunt.ai.core.AiModelDescriptor
import com.bioacupunt.ai.core.AiProvider
import com.bioacupunt.security.AppHardening

class AndroidAiConfigManager(
    context: Context,
    private val prefs: android.content.SharedPreferences = context.getSharedPreferences("ai_config", Context.MODE_PRIVATE)
) : AiConfigManager {
    override suspend fun isProviderEnabled(providerId: String): Boolean =
        prefs.getBoolean("provider_enabled_$providerId", true)

    override suspend fun preferredProviderId(): String? = prefs.getString("preferred_provider", null)
    override suspend fun preferredModelId(): String? = prefs.getString("preferred_model", null)
    override suspend fun setProviderEnabled(providerId: String, enabled: Boolean) {
        prefs.edit().putBoolean("provider_enabled_$providerId", enabled).apply()
    }
    override suspend fun setPreferredProvider(providerId: String?) {
        prefs.edit().putString("preferred_provider", providerId).apply()
    }
    override suspend fun setPreferredModel(modelId: String?) {
        prefs.edit().putString("preferred_model", modelId).apply()
    }

    // Default false: offline-first. Cloud only runs after the doctor flips this in
    // Ajustes > IA AND provides an API key.
    override suspend fun isCloudEnabled(): Boolean = prefs.getBoolean("cloud_ai_enabled", false)
    override suspend fun setCloudEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("cloud_ai_enabled", enabled).apply()
    }
}
