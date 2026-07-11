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
import com.bioacupunt.ai.gemini.GeminiEngine
import kotlin.coroutines.cancellation.CancellationException

class GeminiProvider(
    private val cache: com.bioacupunt.cache.AppCacheManager,
    private val secretsProvider: com.bioacupunt.ai.config.AiSecretsProvider
) : AiProvider {
    override val id = "gemini"
    override val displayName = "Gemini"
    override val capabilities = AiProviderCapabilities(
        capabilities = setOf(AiCapability.Chat, AiCapability.Vision, AiCapability.DocumentAnalysis, AiCapability.StructuredOutput, AiCapability.FunctionCalling, AiCapability.Streaming),
        supportsStream = true,
        supportsLocalExecution = false,
        preferredDevice = DevicePreference.Auto
    )
    override val metadata = AiProviderMetadata(
        providerType = "gemini",
        executionType = AiExecutionType.Remote,
        pricingModel = AiPricingModel.PerToken,
        estimatedCostPer1kTokens = 0.0,
        supportsOffline = false,
        supportsStreaming = true,
        supportsVision = true,
        supportsFunctionCalling = true,
        supportsToolCalling = true,
        supportsLongContext = true,
        supportsStructuredOutput = true,
        supportsMultimodal = true,
        maxContextWindow = 2_097_152,
        maxOutputTokens = 8192,
        recommendedDeviceClass = DevicePreference.Auto
    )
    override val models = listOf(
        AiModelDescriptor(
            id = "gemini-2.5-pro",
            providerId = id,
            displayName = "Gemini 2.5 Pro",
            capabilities = setOf(AiCapability.Chat, AiCapability.Vision, AiCapability.DocumentAnalysis, AiCapability.StructuredOutput, AiCapability.FunctionCalling, AiCapability.LongContext, AiCapability.Multimodal),
            contextTokens = 2_097_152,
            isLocal = false,
            metadata = metadata
        ),
        AiModelDescriptor(
            id = "gemini-2.5-flash",
            providerId = id,
            displayName = "Gemini 2.5 Flash",
            capabilities = setOf(AiCapability.Chat, AiCapability.Vision, AiCapability.DocumentAnalysis, AiCapability.StructuredOutput, AiCapability.FunctionCalling, AiCapability.LongContext, AiCapability.Multimodal),
            contextTokens = 1_048_576,
            isLocal = false,
            metadata = metadata
        )
    )

    override suspend fun isAvailable(): Boolean = secretsProvider.apiKeyFor(id)?.isNotBlank() ?: false

    override suspend fun generate(request: AiRequest): Result<AiResult> {
        val apiKey = secretsProvider.apiKeyFor(id)
            ?: return Result.failure(IllegalStateException("Missing Gemini API key"))
        val effectiveModel = request.modelId ?: models.last().id
        val started = System.currentTimeMillis()
        return runCatching {
            runWithTimeout(30_000L) {
                GeminiEngine.generate(
                    apiKey = apiKey,
                    prompt = request.prompt,
                    systemPrompt = request.systemPrompt,
                    temperature = request.temperature,
                    maxTokens = request.maxTokens,
                    cacheKey = null,
                    cache = cache
                ).getOrThrow()
            }
        }.mapCatching { text ->
            AiResult(
                text = text,
                providerId = id,
                modelId = effectiveModel,
                capabilitiesUsed = setOf(AiCapability.Chat),
                tokensUsed = coerceTokens(request.maxTokens),
                latencyMs = System.currentTimeMillis() - started
            )
        }.onFailure { error ->
            if (error is CancellationException) throw error
        }
    }

    private suspend fun <T> runWithTimeout(timeoutMs: Long, block: suspend () -> T): T =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            kotlinx.coroutines.withTimeout(timeoutMs) { block() }
        }

    private fun coerceTokens(maxTokens: Int): Int? = if (maxTokens > 0) maxTokens else null
}
