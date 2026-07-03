package com.bioacupunt.ai.registry

import com.bioacupunt.ai.core.AiProvider
import com.bioacupunt.ai.core.AiModelDescriptor
import com.bioacupunt.ai.core.AiCapability

class DefaultModelRegistry(
    private val providerRegistry: ProviderRegistry = SimpleProviderRegistry()
) : ModelRegistry {
    private val modelMap = mutableMapOf<String, AiModelDescriptor>()
    private val providerModelMap = mutableMapOf<String, MutableList<AiModelDescriptor>>()

    override suspend fun allModels(): List<AiModelDescriptor> = modelMap.values.toList()

    override suspend fun modelsForProvider(providerId: String): List<AiModelDescriptor> =
        providerModelMap[providerId].orEmpty()

    override suspend fun findByCapabilities(required: Set<AiCapability>): List<AiModelDescriptor> =
        modelMap.values.filter { it.capabilities.containsAll(required) }

    override suspend fun register(provider: AiProvider): Boolean {
        provider.models.forEach { model ->
            modelMap[model.id] = model
            providerModelMap.getOrPut(provider.id) { mutableListOf() } += model
        }
        return true
    }
}
