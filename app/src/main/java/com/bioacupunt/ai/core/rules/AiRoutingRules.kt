package com.bioacupunt.ai.core.rules

import com.bioacupunt.ai.core.AiCapability

data class AiRoutingRules(
    val blockedProviders: Set<String> = emptySet(),
    val blockedModels: Set<String> = emptySet(),
    val requiredCapabilities: Set<AiCapability> = emptySet(),
    val allowLocalOnly: Boolean = false,
    val allowRemoteOnly: Boolean = false,
    val maxCostPer1kTokens: Double? = null,
    val maxResponseTimeMs: Long? = null,
    val maxContextTokens: Int? = null,
    val preferLocal: Boolean = false,
    val privacyRestriction: AiPrivacyRestriction = AiPrivacyRestriction.None
)
