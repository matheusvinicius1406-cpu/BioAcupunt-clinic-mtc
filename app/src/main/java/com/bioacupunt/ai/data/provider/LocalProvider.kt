package com.bioacupunt.ai.data.provider

import com.bioacupunt.ai.core.AiCapability
import com.bioacupunt.ai.core.AiModelDescriptor
import com.bioacupunt.ai.core.AiProvider
import com.bioacupunt.ai.core.AiProviderCapabilities
import com.bioacupunt.ai.core.AiProviderMetadata
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.ai.core.AiResult
import com.bioacupunt.ai.core.AiExecutionType
import com.bioacupunt.ai.core.AiPricingModel

interface LocalProvider : AiProvider {
    suspend fun healthCheck(): Result<HealthStatus>
}

data class HealthStatus(
    val available: Boolean,
    val version: String? = null,
    val latencyMs: Long? = null,
    val successRate: Double? = null,
    val lastFailureReason: String? = null,
    val capabilities: Set<AiCapability> = emptySet()
)
