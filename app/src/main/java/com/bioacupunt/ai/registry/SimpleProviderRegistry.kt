package com.bioacupunt.ai.registry

import com.bioacupunt.ai.core.AiProvider
import com.bioacupunt.ai.core.AiCapability

class SimpleProviderRegistry : ProviderRegistry {
    private val providers = mutableMapOf<String, AiProvider>()
    override suspend fun allProviders(): List<AiProvider> = providers.values.toList()
    override suspend fun providerById(id: String): AiProvider? = providers[id]
    override suspend fun register(provider: AiProvider): Boolean {
        providers[provider.id] = provider
        return true
    }
    override suspend fun availableProviders(): List<AiProvider> = providers.values.filter { runCatching { it.isAvailable() }.getOrDefault(false) }
    override suspend fun providersForCapabilities(required: Set<AiCapability>): List<AiProvider> =
        availableProviders().filter { p -> required.all { req -> p.capabilities.capabilities.contains(req) } }
}
