package com.bioacupunt.ai.registry

import com.bioacupunt.ai.core.AiCapability
import com.bioacupunt.ai.core.AiProvider

interface ProviderRegistry {
    suspend fun allProviders(): List<AiProvider>
    suspend fun providerById(id: String): AiProvider?
    suspend fun register(provider: AiProvider): Boolean
    suspend fun availableProviders(): List<AiProvider>
    suspend fun providersForCapabilities(required: Set<AiCapability>): List<AiProvider>
}
