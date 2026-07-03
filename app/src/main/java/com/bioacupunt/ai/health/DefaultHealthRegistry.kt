package com.bioacupunt.ai.health

import com.bioacupunt.ai.core.AiCapability
import com.bioacupunt.ai.core.AiModelDescriptor
import com.bioacupunt.ai.core.AiProvider

class DefaultHealthRegistry : HealthRegistry {
    private val store = mutableMapOf<String, ProviderHealth>()

    private fun key(providerId: String, modelId: String?): String = if (modelId == null) providerId else "$providerId::$modelId"

    override suspend fun update(health: ProviderHealth) {
        store[key(health.providerId, health.modelId)] = health
    }

    override suspend fun get(providerId: String, modelId: String?): ProviderHealth? = store[key(providerId, modelId)]

    override suspend fun all(): List<ProviderHealth> = store.values.toList()

    override suspend fun bestFor(capabilities: Set<AiCapability>): ProviderHealth? {
        val now = System.currentTimeMillis()
        return store.values
            .filter { it.status != ProviderStatus.Unavailable }
            .filter { capabilities.isEmpty() || it.availableCapabilities.containsAll(capabilities) }
            .minByOrNull { it.avgLatencyMs ?: Long.MAX_VALUE }
    }
}
