package com.bioacupunt.copilot.routing

import com.bioacupunt.ai.core.AiRequest

/**
 * §38-39 MODEL ROUTER
 *
 * Routes AI requests based on capability requirements:
 * - offline → local only
 * - latency → local preferred
 * - context size → depends on model capacity
 * - privacy → local for clinical data
 * - task complexity → local for simple, cloud for complex
 * - model availability → fallback
 *
 * Currently: only local model (Phi-4 Mini). Cloud was removed per 2026-07-29 decision.
 * Architecture preserved for future cloud re-addition.
 */
class ModelRouter {

    enum class ModelCapability {
        OFFLINE,
        LOW_LATENCY,
        LARGE_CONTEXT,
        HIGH_PRIVACY,
        COMPLEX_REASONING,
    }

    data class RoutingDecision(
        val preferLocal: Boolean,
        val reason: String,
        val requiredCapabilities: Set<ModelCapability>,
    )

    /**
     * Decide whether to use local or cloud model.
     * Currently always returns local — cloud was removed.
     */
    fun route(
        request: AiRequest,
        availableCapabilities: Set<ModelCapability> = setOf(ModelCapability.OFFLINE),
    ): RoutingDecision {
        // Clinical data always goes local (privacy)
        if (request.context?.contains("paciente") == true) {
            return RoutingDecision(
                preferLocal = true,
                reason = "CLINICAL_PRIVACY",
                requiredCapabilities = setOf(ModelCapability.HIGH_PRIVACY),
            )
        }

        // Offline mode
        if (!availableCapabilities.contains(ModelCapability.OFFLINE)) {
            return RoutingDecision(
                preferLocal = true,
                reason = "OFFLINE_MODE",
                requiredCapabilities = setOf(ModelCapability.OFFLINE),
            )
        }

        // Default: local
        return RoutingDecision(
            preferLocal = true,
            reason = "LOCAL_DEFAULT",
            requiredCapabilities = availableCapabilities,
        )
    }
}
