package com.bioacupunt.ai.domain.repository

import com.bioacupunt.ai.domain.model.AiCapabilities
import com.bioacupunt.ai.domain.model.AiError
import com.bioacupunt.ai.domain.model.AiRequest
import com.bioacupunt.ai.domain.model.AiResult
import com.bioacupunt.ai.domain.model.AiProviderType
import kotlinx.coroutines.flow.Flow

interface AiRepository {
    suspend fun generate(request: AiRequest): Result<AiResult>
    suspend fun generateStream(request: AiRequest): Flow<String>
    suspend fun capabilities(providerType: AiProviderType? = null): Result<List<AiCapabilities>>
    suspend fun resolveProvider(request: AiRequest): Result<AiProviderType>
}
