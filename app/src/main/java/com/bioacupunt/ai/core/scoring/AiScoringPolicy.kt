package com.bioacupunt.ai.core.scoring

interface AiScoringPolicy {
    suspend fun score(request: AiRequest, candidates: List<AiProviderScore>): List<AiProviderScore>
}
