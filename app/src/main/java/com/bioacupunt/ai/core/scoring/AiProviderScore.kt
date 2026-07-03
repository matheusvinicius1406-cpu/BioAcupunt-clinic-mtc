package com.bioacupunt.ai.core.scoring

import com.bioacupunt.ai.core.AiCapability
import com.bioacupunt.ai.core.AiProviderMetadata

data class AiProviderScore(
    val providerId: String,
    val modelId: String,
    val score: Double,
    val estimatedCost: Double? = null,
    val estimatedLatencyMs: Long? = null,
    val contextFitRatio: Double? = null,
    val reasons: List<String> = emptyList()
)
