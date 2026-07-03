package com.bioacupunt.ai.domain.repository

import com.bioacupunt.ai.domain.model.AiRequest
import com.bioacupunt.ai.domain.model.AiResult

interface AiProvider {
    suspend fun generate(request: AiRequest): Result<AiResult>
    suspend fun isReady(): Boolean
}
