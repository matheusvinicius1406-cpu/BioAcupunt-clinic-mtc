package com.bioacupunt.ai.core

interface ModelRegistry {
    suspend fun allModels(): List<AiModelDescriptor>
    suspend fun modelsForProvider(providerId: String): List<AiModelDescriptor>
    suspend fun findByCapabilities(required: Set<AiCapability>): List<AiModelDescriptor>
    suspend fun register(provider: AiProvider): Boolean
}
