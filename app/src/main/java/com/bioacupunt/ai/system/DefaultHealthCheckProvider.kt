package com.bioacupunt.ai.system

import com.bioacupunt.ai.config.AiSecretsProvider
import com.bioacupunt.ai.core.AiCapability
import com.bioacupunt.ai.core.AiModelDescriptor
import com.bioacupunt.ai.core.AiProvider
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.ai.health.DefaultHealthRegistry
import com.bioacupunt.ai.health.HealthRegistry
import com.bioacupunt.ai.health.ProviderHealth
import com.bioacupunt.ai.health.ProviderStatus

class DefaultHealthCheckProvider(
    private val healthRegistry: HealthRegistry = DefaultHealthRegistry()
) : HealthCheckProvider {
    override suspend fun checkProvider(provider: AiProvider): ProviderHealth {
        val available = provider.isAvailable()
        val status = if (available) ProviderStatus.Healthy else ProviderStatus.Unavailable
        val health = ProviderHealth(
            providerId = provider.id,
            status = status,
            lastCheckedAt = System.currentTimeMillis(),
            availableCapabilities = provider.capabilities.capabilities,
            providerVersion = null,
            modelVersion = null,
            maxContextWindow = provider.models.maxOfOrNull { it.contextTokens },
            maxOutputTokens = provider.metadata.maxOutputTokens.takeIf { it > 0 },
            executionType = provider.metadata.executionType
        )
        healthRegistry.update(health)
        return health
    }

    override suspend fun checkModel(provider: AiProvider, model: AiModelDescriptor): ProviderHealth {
        val available = provider.isAvailable()
        val status = if (available) ProviderStatus.Healthy else ProviderStatus.Unavailable
        val health = ProviderHealth(
            providerId = provider.id,
            modelId = model.id,
            status = status,
            lastCheckedAt = System.currentTimeMillis(),
            availableCapabilities = model.capabilities,
            maxContextWindow = model.contextTokens,
            executionType = provider.metadata.executionType
        )
        healthRegistry.update(health)
        return health
    }
}
