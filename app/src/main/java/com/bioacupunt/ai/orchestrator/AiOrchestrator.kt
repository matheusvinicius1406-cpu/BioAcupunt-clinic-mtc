package com.bioacupunt.ai.orchestrator

import com.bioacupunt.ai.core.AiCapability
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.ai.core.AiResult
import com.bioacupunt.ai.core.AiError
import com.bioacupunt.ai.registry.ProviderRegistry

interface AiOrchestrator {
    suspend fun execute(request: AiRequest): Result<AiResult>
}
