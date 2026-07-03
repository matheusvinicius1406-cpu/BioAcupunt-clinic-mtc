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
import com.bioacupunt.ai.core.DevicePreference
import kotlin.coroutines.cancellation.CancellationException

class MockProvider : AiProvider {
    override val id = "mock"
    override val displayName = "Mock"
    override val capabilities = AiProviderCapabilities(
        capabilities = setOf(AiCapability.Chat, AiCapability.Streaming),
        supportsStream = true,
        supportsLocalExecution = true,
        preferredDevice = DevicePreference.Auto
    )
    override val metadata = AiProviderMetadata(
        providerType = "mock",
        executionType = AiExecutionType.Local,
        pricingModel = AiPricingModel.Free,
        supportsOffline = true,
        supportsStreaming = true,
        recommendedDeviceClass = DevicePreference.Auto
    )
    override val models = listOf(
        AiModelDescriptor(
            id = "mock-chat",
            providerId = id,
            displayName = "Mock Chat",
            capabilities = setOf(AiCapability.Chat, AiCapability.Streaming),
            contextTokens = 2048,
            isLocal = true,
            metadata = metadata
        )
    )

    override suspend fun isAvailable(): Boolean = true
    override suspend fun capabilitiesForModel(modelId: String): AiProviderCapabilities = capabilities

    override suspend fun generate(request: AiRequest): Result<AiResult> {
        val modelId = request.modelId ?: models.first().id
        val started = System.currentTimeMillis()
        val text = if (request.attachments.isNotEmpty()) {
            "Mock: processei ${request.attachments.size} anexo(s)."
        } else {
            "Mock resposta para: ${request.prompt.take(80)}"
        }
        return Result.success(
            AiResult(
                text = text,
                providerId = id,
                modelId = modelId,
                capabilitiesUsed = request.requiredCapabilities,
                latencyMs = System.currentTimeMillis() - started,
                cached = false,
                fallbackUsed = false
            )
        )
    }
}
