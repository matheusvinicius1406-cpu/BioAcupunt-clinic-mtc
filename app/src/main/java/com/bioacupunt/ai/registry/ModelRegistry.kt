package com.bioacupunt.ai.registry

import com.bioacupunt.ai.core.AiCapability
import com.bioacupunt.ai.core.AiModelDescriptor
import com.bioacupunt.ai.core.AiProvider

interface ModelRegistry {
    suspend fun allModels(): List<AiModelDescriptor>
    suspend fun modelsForProvider(providerId: String): List<AiModelDescriptor>
    suspend fun findByCapabilities(required: Set<AiCapability>): List<AiModelDescriptor>
    suspend fun register(provider: AiProvider): Boolean
}
