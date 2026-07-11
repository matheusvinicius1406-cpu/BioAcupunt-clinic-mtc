package com.bioacupunt.ai.core.scoring

import com.bioacupunt.ai.core.AiRequest

interface AiScoringPolicy {
    suspend fun score(request: AiRequest, candidates: List<AiProviderScore>): List<AiProviderScore>
}
