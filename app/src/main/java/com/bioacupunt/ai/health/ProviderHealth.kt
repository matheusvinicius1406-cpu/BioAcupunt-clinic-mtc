package com.bioacupunt.ai.health

data class ProviderHealth(
    val providerId: String,
    val modelId: String? = null,
    val status: ProviderStatus,
    val lastCheckedAt: Long,
    val avgLatencyMs: Long? = null,
    val successRate: Double? = null,
    val lastFailureAt: Long? = null,
    val lastFailureReason: String? = null,
    val availableCapabilities: Set<com.bioacupunt.ai.core.AiCapability> = emptySet(),
    val providerVersion: String? = null,
    val modelVersion: String? = null,
    val maxContextWindow: Int? = null,
    val maxOutputTokens: Int? = null,
    val executionType: com.bioacupunt.ai.core.AiExecutionType? = null
)

enum class ProviderStatus { Healthy, Degraded, Unavailable }
