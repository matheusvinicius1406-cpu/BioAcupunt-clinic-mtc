package com.bioacupunt.ai.config

interface AiConfigManager {
    suspend fun isProviderEnabled(providerId: String): Boolean
    suspend fun preferredProviderId(): String?
    suspend fun preferredModelId(): String?
    suspend fun setProviderEnabled(providerId: String, enabled: Boolean)
    suspend fun setPreferredProvider(providerId: String?)
    suspend fun setPreferredModel(modelId: String?)

    /**
     * Master switch for the OPTIONAL cloud provider. Off by default — the app is
     * offline-first and a patient chart is dado pessoal sensível (LGPD Art. 5º, II).
     * The cloud provider reports `isAvailable() == false` until this is true AND an API
     * key exists, so no clinical text ever leaves the device unless the doctor has
     * explicitly turned it on. Wire this to the "IA na nuvem" toggle in Ajustes > IA.
     */
    suspend fun isCloudEnabled(): Boolean
    suspend fun setCloudEnabled(enabled: Boolean)
}
