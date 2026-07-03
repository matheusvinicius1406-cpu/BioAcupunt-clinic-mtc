package com.bioacupunt.ai.system

import com.bioacupunt.ai.core.AiCapability
import com.bioacupunt.ai.core.AiModelDescriptor
import com.bioacupunt.ai.core.AiProvider

interface HealthCheckProvider {
    suspend fun checkProvider(provider: AiProvider): ProviderHealth
    suspend fun checkModel(provider: AiProvider, model: AiModelDescriptor): ProviderHealth
}
