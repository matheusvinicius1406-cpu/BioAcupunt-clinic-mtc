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

    // Default true: Gemini is the app's definitive AI out of the box, on every fresh
    // install — a deliberate product decision (2026-07-26), not an accidental flip. The
    // doctor can still turn it off in Ajustes > IA for a 100% offline posture; when she
    // does, the on-device model (when downloaded) keeps working as before. This does not
    // touch R1 (ClinicalSafetyEngine never calls any LLM) or R2 (AskLibraryUseCase's
    // `if (!grounding.hasEvidence) return NoEvidence` gate runs before any provider,
    // cloud or local, is even considered) — it only changes which provider answers once
    // a question has already passed those gates.
    override suspend fun isCloudEnabled(): Boolean = prefs.getBoolean("cloud_ai_enabled", true)
    override suspend fun setCloudEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("cloud_ai_enabled", enabled).apply()
    }
}
