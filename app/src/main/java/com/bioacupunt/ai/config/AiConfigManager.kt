package com.bioacupunt.ai.core

interface AiConfigManager {
    suspend fun isProviderEnabled(providerId: String): Boolean
    suspend fun preferredProviderId(): String?
    suspend fun preferredModelId(): String?
    suspend fun setProviderEnabled(providerId: String, enabled: Boolean)
    suspend fun setPreferredProvider(providerId: String?)
    suspend fun setPreferredModel(modelId: String?)
}
