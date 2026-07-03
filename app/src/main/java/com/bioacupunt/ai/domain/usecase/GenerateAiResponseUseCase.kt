package com.bioacupunt.ai.domain.usecase

import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.ai.core.AiRepository
import com.bioacupunt.ai.core.AiResult

class GenerateAiResponseUseCase(private val repo: AiRepository) {
    suspend operator fun invoke(request: AiRequest): Result<AiResult> = repo.generate(request)
}
