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

class FakeProvider(
    override val id: String = "fake",
    override val displayName: String = "Fake",
    private val responseText: String = "Fake resposta",
    private val throwOnGenerate: Boolean = false
) : AiProvider {
    override val capabilities = AiProviderCapabilities(
        capabilities = setOf(AiCapability.Chat),
        supportsStream = false,
        supportsLocalExecution = true
    )
    override val metadata = AiProviderMetadata(
        providerType = id,
        executionType = AiExecutionType.Local,
        pricingModel = AiPricingModel.Free,
        supportsOffline = true
    )
    override val models = listOf(
        AiModelDescriptor(
            id = "fake-chat",
            providerId = id,
            displayName = "Fake Chat",
            capabilities = setOf(AiCapability.Chat),
            contextTokens = 1024,
            isLocal = true,
            metadata = metadata
        )
    )

    override suspend fun isAvailable(): Boolean = !throwOnGenerate
    override suspend fun capabilitiesForModel(modelId: String): AiProviderCapabilities = capabilities

    override suspend fun generate(request: AiRequest): Result<AiResult> {
        if (throwOnGenerate) return Result.failure(IllegalStateException("forced fake failure"))
        return Result.success(
            AiResult(
                text = responseText,
                providerId = id,
                modelId = request.modelId ?: models.first().id,
                capabilitiesUsed = request.requiredCapabilities,
                latencyMs = 1,
                cached = false,
                fallbackUsed = false
            )
        )
    }
}
