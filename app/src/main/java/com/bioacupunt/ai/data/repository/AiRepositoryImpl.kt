package com.bioacupunt.ai.data.repository

import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.ai.core.AiResult
import com.bioacupunt.ai.core.AiRepository
import com.bioacupunt.ai.orchestrator.AiOrchestrator

class AiRepositoryImpl(
    private val orchestrator: AiOrchestrator
) : AiRepository {
    override suspend fun generate(request: AiRequest): Result<AiResult> = orchestrator.execute(request)
    override suspend fun stream(request: AiRequest): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.flow {
        generate(request).getOrNull()?.text?.let { emit(it) }
    }
}
