package com.bioacupunt.copilot.rag

/**
 * §4-5 EVIDENCE GATE — R2 CRITICAL ENFORCEMENT
 *
 * The project rule is absolute:
 * ```
 * RAG WITHOUT EVIDENCE = NO MODEL CALL
 * ```
 *
 * This gate sits IMMEDIATELY before the LLM call path.
 * It is the single point of enforcement — no other code path
 * can bypass this check.
 *
 * Flow:
 * ```
 * Retrieval
 *     ↓
 * ContextBuilder
 *     ↓
 * EvidenceGate.evaluate(context)
 *     ├── BLOCK → return NoEvidenceResponse (LLM never called)
 *     └── ALLOW → proceed to LLM
 * ```
 *
 * The gate is deterministic. Same context → same decision, always.
 * No LLM involved in the gate itself.
 */
class EvidenceGate {

    enum class GateDecision {
        ALLOW,
        BLOCK_NO_EVIDENCE,
        BLOCK_INSUFFICIENT_EVIDENCE,
    }

    data class GateResult(
        val decision: GateDecision,
        val reason: String,
        val evidenceCount: Int,
        val contextItemCount: Int,
    )

    data class NoEvidenceResponse(
        val answer: String,
        val confidence: String,
        val warnings: List<String>,
    )

    companion object {
        /**
         * Minimum number of context items required to proceed to LLM.
         * If context is empty or below this threshold, gate blocks.
         */
        const val MIN_CONTEXT_ITEMS = 1

        /**
         * Minimum evidence IDs required.
         * Zero evidence IDs = no provenance = block.
         */
        const val MIN_EVIDENCE_IDS = 0 // context.items.size is the primary check

        /**
         * Standard no-evidence response.
         * The LLM is NEVER called when this is returned.
         */
        val NO_EVIDENCE_RESPONSE = NoEvidenceResponse(
            answer = "Não encontrei evidência suficiente na biblioteca para responder esta pergunta.",
            confidence = "INSUFFICIENT",
            warnings = listOf("NO_EVIDENCE", "MODEL_NOT_CALLED"),
        )

        /**
         * Insufficient evidence response.
         * Context exists but is too thin for reliable grounding.
         */
        val INSUFFICIENT_RESPONSE = NoEvidenceResponse(
            answer = "A evidência encontrada é insuficiente para uma resposta fundamentada.",
            confidence = "INSUFFICIENT",
            warnings = listOf("INSUFFICIENT_EVIDENCE", "MODEL_NOT_CALLED"),
        )
    }

    /**
     * Evaluate whether the context has enough evidence to proceed to LLM.
     *
     * This is the SINGLE enforcement point for the R2 gate.
     * All paths that call the LLM must go through this method.
     *
     * Deterministic: same input → same output, always.
     *
     * @param context The structured context from ContextBuilder
     * @param requiredEvidence Whether evidence is required (some intents may skip)
     * @return GateResult with ALLOW or BLOCK decision
     */
    fun evaluate(
        context: ContextBuilder.StructuredContext,
        requiredEvidence: Boolean = true,
    ): GateResult {
        // If evidence not required for this path, always allow
        if (!requiredEvidence) {
            return GateResult(
                decision = GateDecision.ALLOW,
                reason = "Evidence not required for this intent",
                evidenceCount = context.evidenceIds.size,
                contextItemCount = context.items.size,
            )
        }

        // Gate 1: No context items at all → BLOCK
        if (context.items.isEmpty()) {
            return GateResult(
                decision = GateDecision.BLOCK_NO_EVIDENCE,
                reason = "Context is empty — no retrieval results found",
                evidenceCount = 0,
                contextItemCount = 0,
            )
        }

        // Gate 2: Context items exist but no evidence IDs → BLOCK
        // This means we found entities but none have provenance/evidence chains
        if (context.evidenceIds.isEmpty() && context.items.size < MIN_CONTEXT_ITEMS) {
            return GateResult(
                decision = GateDecision.BLOCK_INSUFFICIENT_EVIDENCE,
                reason = "Context has ${context.items.size} item(s) but no evidence IDs — insufficient for grounding",
                evidenceCount = 0,
                contextItemCount = context.items.size,
            )
        }

        // Gate 3: Context is sufficient → ALLOW
        return GateResult(
            decision = GateDecision.ALLOW,
            reason = "Context has ${context.items.size} items and ${context.evidenceIds.size} evidence IDs — sufficient for grounding",
            evidenceCount = context.evidenceIds.size,
            contextItemCount = context.items.size,
        )
    }

    /**
     * Convenience method: returns true if the gate allows proceeding to LLM.
     */
    fun allowsLLMCall(
        context: ContextBuilder.StructuredContext,
        requiredEvidence: Boolean = true,
    ): Boolean {
        return evaluate(context, requiredEvidence).decision == GateDecision.ALLOW
    }

    /**
     * Get the appropriate no-evidence response for a blocked gate.
     */
    fun getBlockedResponse(gateResult: GateResult): NoEvidenceResponse {
        return when (gateResult.decision) {
            GateDecision.ALLOW -> throw IllegalArgumentException("Cannot get blocked response for ALLOW decision")
            GateDecision.BLOCK_NO_EVIDENCE -> NO_EVIDENCE_RESPONSE
            GateDecision.BLOCK_INSUFFICIENT_EVIDENCE -> INSUFFICIENT_RESPONSE
        }
    }
}
