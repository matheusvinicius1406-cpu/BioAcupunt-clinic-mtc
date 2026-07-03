package com.bioacupunt.ai.data.provider

import com.bioacupunt.ai.domain.model.AiCapabilities
import com.bioacupunt.ai.domain.model.AiProviderType

class FallbackProvider : AiProvider {
    override suspend fun generate(request: AiRequest): Result<AiResult> =
        Result.failure(UnsupportedOperationException("Fallback provider has no generation capability"))

    override suspend fun isReady(): Boolean = true
    override fun providerType(): AiProviderType = AiProviderType.Gemini
    override suspend fun capabilities(): AiCapabilities = AiCapabilities(
        providerType = AiProviderType.Gemini,
        supportsStream = false,
        supportsVision = false,
        supportsTools = false,
        supportsEmbeddings = false,
        maxContextTokens = 2048,
        isLocal = false
    )
}
