package com.bioacupunt.ai.data.legacy

import com.bioacupunt.ai.core.AiCapability
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.ai.core.AiResult

object LegacyAiAgentBridge {
    suspend fun clinicalAgentAnswer(request: AiRequest): Result<AiResult> = Result.failure(UnsupportedOperationException("legacy bridge stub"))
}
