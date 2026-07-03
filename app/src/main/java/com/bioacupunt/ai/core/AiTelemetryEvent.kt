package com.bioacupunt.ai.core

data class AiTelemetryEvent(
    val providerId: String,
    val modelId: String,
    val capabilitiesUsed: Set<AiCapability>,
    val agentId: String? = null,
    val toolIds: List<String> = emptyList(),
    val latencyMs: Long,
    val tokensUsed: Int? = null,
    val memoryUsedBytes: Long? = null,
    val costEstimate: Double? = null,
    val success: Boolean,
    val fallbackUsed: Boolean = false,
    val preferredLocal: Boolean = false,
    val decisionReason: String? = null,
    val errorType: String? = null,
    val cached: Boolean = false
)
