package com.bioacupunt.ai.domain.usecase

import com.bioacupunt.ai.domain.model.AiError
import com.bioacupunt.ai.domain.model.AiError.NoProviderAvailable
import com.bioacupunt.ai.domain.model.AiError.Provider
import com.bioacupunt.ai.domain.model.AiProviderType
import com.bioacupunt.ai.domain.model.AiRequest
import com.bioacupunt.ai.domain.model.AiResult
import com.bioacupunt.ai.domain.repository.AiProvider

class AiOrchestrator(private val providers: List<AiProvider>) {

    init {
        require(providers.isNotEmpty()) { "AiOrchestrator requires at least one provider" }
    }

    suspend fun generate(request: AiRequest): Result<AiResult> {
        val ordered = orderedProviders(request)
        var last: Throwable? = null

        for (provider in ordered) {
            if (!provider.isReady()) continue
            val result = runCatching { provider.generate(request) }
            result.onSuccess { return it }
                .onFailure { last = it; continue }
        }

        return Result.failure(
            Provider(
                providerType = providers.firstOrNull()?.let { resolveType(it) } ?: AiProviderType.Gemini,
                cause = last ?: NoProviderAvailable
            )
        )
    }

    private fun orderedProviders(request: AiRequest): List<AiProvider> {
        val hint = request.capabilitiesHint?.providerType
        if (hint != null) {
            val primary = providers.firstOrNull { resolveType(it) == hint }
            if (primary != null) return listOf(primary) + providers - primary
        }
        return providers
    }

    private fun resolveType(provider: AiProvider): AiProviderType {
        return when (provider) {
            is com.bioacupunt.ai.data.provider.GeminiProvider -> AiProviderType.Gemini
            is com.bioacupunt.ai.data.provider.FallbackProvider -> AiProviderType.Gemini
            else -> AiProviderType.Gemini
        }
    }
}
