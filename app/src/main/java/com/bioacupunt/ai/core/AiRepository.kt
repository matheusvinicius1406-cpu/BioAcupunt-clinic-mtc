package com.bioacupunt.ai.core

import kotlinx.coroutines.flow.Flow

interface AiRepository {
    suspend fun generate(request: AiRequest): Result<AiResult>
    suspend fun stream(request: AiRequest): Flow<String>
}
