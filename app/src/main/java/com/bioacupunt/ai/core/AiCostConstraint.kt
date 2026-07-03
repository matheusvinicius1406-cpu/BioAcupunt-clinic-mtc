package com.bioacupunt.ai.core

data class AiCostConstraint(
    val maxCostPerRequest: Double? = null,
    val maxCostPer1kTokens: Double? = null
)
