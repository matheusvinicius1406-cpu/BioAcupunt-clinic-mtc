package com.bioacupunt.ai.core

data class AiResult(
    val text: String,
    val providerId: String,
    val modelId: String,
    val capabilitiesUsed: Set<AiCapability> = emptySet(),
    val toolsUsed: List<String> = emptyList(),
    val agentId: String? = null,
    val tokensUsed: Int? = null,
    val latencyMs: Long? = null,
    val cached: Boolean = false,
    val fallbackUsed: Boolean = false,
    val decisionReason: String? = null,
    val metadata: Map<String, String> = emptyMap()
)
