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

    // Default false: cloud is opt-in, gated by the first-access consent dialog
    // (CloudConsentDialog, shown once per SecurePreferences.cloudConsentAsked). The
    // product decision (2026-07-26) to make Gemini the definitive AI out of the box
    // still stands, but "out of the box" means "the doctor accepted the LGPD notice on
    // first launch," not "before she has ever seen it" — administrative chat context
    // (AppContextBuilder) includes real patient names/appointment times, so nothing may
    // reach a remote provider before she has explicitly answered that dialog. Once
    // answered (accept or decline), this flag is the durable record of her choice; she
    // can still flip it any time in Ajustes > IA. None of this touches R1
    // (ClinicalSafetyEngine never calls any LLM) or R2 (AskLibraryUseCase's
    // `if (!grounding.hasEvidence) return NoEvidence` gate runs before any provider,
    // cloud or local, is even considered) — it only changes which provider answers once
    // a question has already passed those gates.
    override suspend fun isCloudEnabled(): Boolean = prefs.getBoolean("cloud_ai_enabled", false)
    override suspend fun setCloudEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("cloud_ai_enabled", enabled).apply()
    }
}
