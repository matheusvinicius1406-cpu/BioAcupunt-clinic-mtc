package com.bioacupunt.ai.health

interface HealthRegistry {
    suspend fun update(health: ProviderHealth)
    suspend fun get(providerId: String, modelId: String? = null): ProviderHealth?
    suspend fun all(): List<ProviderHealth>
    suspend fun bestFor(capabilities: Set<com.bioacupunt.ai.core.AiCapability>): ProviderHealth?
}
