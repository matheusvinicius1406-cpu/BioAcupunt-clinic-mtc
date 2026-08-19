package com.bioacupunt.copilot.rag

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * §4-5 EVIDENCE GATE TEST — R2 CRITICAL ENFORCEMENT
 *
 * The project rule is absolute:
 * RAG WITHOUT EVIDENCE = NO MODEL CALL
 *
 * These tests prove that the gate blocks when there is no evidence,
 * and allows when there is evidence.
 */
class EvidenceGateTest {

    private lateinit var gate: EvidenceGate

    @Before
    fun setup() {
        gate = EvidenceGate()
    }

    // ── BLOCK scenarios ─────────────────────────────────────────────

    @Test
    fun emptyContext_blocksNoEvidence() {
        val context = ContextBuilder.StructuredContext(
            items = emptyList(),
            totalCharacters = 0,
            totalTokens = 0,
            truncated = false,
            evidenceIds = emptyList(),
        )

        val result = gate.evaluate(context, requiredEvidence = true)

        assertEquals(EvidenceGate.GateDecision.BLOCK_NO_EVIDENCE, result.decision)
        assertEquals(0, result.evidenceCount)
        assertEquals(0, result.contextItemCount)
    }

    @Test
    fun emptyContext_insufficientEvidenceAlsoBlocks() {
        val context = ContextBuilder.StructuredContext(
            items = emptyList(),
            totalCharacters = 0,
            totalTokens = 0,
            truncated = false,
            evidenceIds = emptyList(),
        )

        val result = gate.evaluate(context, requiredEvidence = true)

        // Both BLOCK_NO_EVIDENCE and BLOCK_INSUFFICIENT_EVIDENCE should block
        assertTrue(
            "Gate should block when context is empty",
            result.decision == EvidenceGate.GateDecision.BLOCK_NO_EVIDENCE ||
                result.decision == EvidenceGate.GateDecision.BLOCK_INSUFFICIENT_EVIDENCE
        )
    }

    @Test
    fun blockedResponse_neverCallsLLM() {
        val context = ContextBuilder.StructuredContext(
            items = emptyList(),
            totalCharacters = 0,
            totalTokens = 0,
            truncated = false,
            evidenceIds = emptyList(),
        )

        val result = gate.evaluate(context, requiredEvidence = true)

        // When gate blocks, the response should indicate MODEL_NOT_CALLED
        val blockedResponse = gate.getBlockedResponse(result)
        assertTrue(
            "Blocked response should contain MODEL_NOT_CALLED warning",
            blockedResponse.warnings.contains("MODEL_NOT_CALLED")
        )
        assertTrue(
            "Blocked response confidence should be INSUFFICIENT",
            blockedResponse.confidence == "INSUFFICIENT"
        )
    }

    @Test
    fun blockedResponse_noAnswerContent() {
        val context = ContextBuilder.StructuredContext(
            items = emptyList(),
            totalCharacters = 0,
            totalTokens = 0,
            truncated = false,
            evidenceIds = emptyList(),
        )

        val result = gate.evaluate(context, requiredEvidence = true)
        val blockedResponse = gate.getBlockedResponse(result)

        // The answer should be a generic "no evidence" message, not a generated answer
        assertTrue(
            "Blocked response should mention no evidence",
            blockedResponse.answer.contains("evidência") || blockedResponse.answer.contains("insuficiente")
        )
    }

    // ── ALLOW scenarios ─────────────────────────────────────────────

    @Test
    fun contextWithItemsAndEvidence_allowsLLM() {
        val context = ContextBuilder.StructuredContext(
            items = listOf(
                ContextBuilder.ContextItem(
                    entity = "Insônia",
                    content = "A insônia é um distúrbio do sono...",
                    source = "source.1",
                    citation = "Insônia (SYMPTOM)",
                    evidence = listOf("ev.1"),
                    provenance = "Maciocia, The Foundations of Chinese Medicine",
                ),
            ),
            totalCharacters = 100,
            totalTokens = 25,
            truncated = false,
            evidenceIds = listOf("ev.1"),
        )

        val result = gate.evaluate(context, requiredEvidence = true)

        assertEquals(EvidenceGate.GateDecision.ALLOW, result.decision)
        assertEquals(1, result.evidenceCount)
        assertEquals(1, result.contextItemCount)
    }

    @Test
    fun requiredEvidenceFalse_alwaysAllows() {
        val context = ContextBuilder.StructuredContext(
            items = emptyList(),
            totalCharacters = 0,
            totalTokens = 0,
            truncated = false,
            evidenceIds = emptyList(),
        )

        val result = gate.evaluate(context, requiredEvidence = false)

        assertEquals(EvidenceGate.GateDecision.ALLOW, result.decision)
    }

    // ── allowsLLMCall convenience ───────────────────────────────────

    @Test
    fun allowsLLMCall_emptyContext_returnsFalse() {
        val context = ContextBuilder.StructuredContext(
            items = emptyList(),
            totalCharacters = 0,
            totalTokens = 0,
            truncated = false,
            evidenceIds = emptyList(),
        )

        assertFalse(gate.allowsLLMCall(context, requiredEvidence = true))
    }

    @Test
    fun allowsLLMCall_withEvidence_returnsTrue() {
        val context = ContextBuilder.StructuredContext(
            items = listOf(
                ContextBuilder.ContextItem(
                    entity = "Ponto LI4",
                    content = "LI4 é o ponto He-Gu...",
                    evidence = listOf("ev.1"),
                ),
            ),
            totalCharacters = 50,
            totalTokens = 12,
            truncated = false,
            evidenceIds = listOf("ev.1"),
        )

        assertTrue(gate.allowsLLMCall(context, requiredEvidence = true))
    }

    // ── Deterministic ───────────────────────────────────────────────

    @Test
    fun evaluate_deterministic_sameInputSameOutput() {
        val context = ContextBuilder.StructuredContext(
            items = listOf(
                ContextBuilder.ContextItem(
                    entity = "Test",
                    content = "Content",
                    evidence = listOf("ev.1"),
                ),
            ),
            totalCharacters = 7,
            totalTokens = 1,
            truncated = false,
            evidenceIds = listOf("ev.1"),
        )

        val result1 = gate.evaluate(context, requiredEvidence = true)
        val result2 = gate.evaluate(context, requiredEvidence = true)

        assertEquals(result1.decision, result2.decision)
        assertEquals(result1.reason, result2.reason)
        assertEquals(result1.evidenceCount, result2.evidenceCount)
        assertEquals(result1.contextItemCount, result2.contextItemCount)
    }

    // ── Edge cases ──────────────────────────────────────────────────

    @Test
    fun contextWithItemsButNoEvidenceIds_allowsIfBelowThreshold() {
        // Context has 1 item but no evidence IDs
        // Gate 2 checks: evidenceIds.isEmpty() && items.size < MIN_CONTEXT_ITEMS
        // MIN_CONTEXT_ITEMS = 1, so items.size (1) is NOT < 1 → Gate 2 doesn't trigger
        val context = ContextBuilder.StructuredContext(
            items = listOf(
                ContextBuilder.ContextItem(
                    entity = "Test",
                    content = "Content",
                ),
            ),
            totalCharacters = 7,
            totalTokens = 1,
            truncated = false,
            evidenceIds = emptyList(),
        )

        val result = gate.evaluate(context, requiredEvidence = true)

        // Gate 1: items not empty → pass
        // Gate 2: evidenceIds empty but items.size (1) is NOT < MIN_CONTEXT_ITEMS (1) → pass
        // Gate 3: ALLOW
        assertEquals(EvidenceGate.GateDecision.ALLOW, result.decision)
    }

    @Test
    fun getBlockedResponse_throwsForAllowDecision() {
        val context = ContextBuilder.StructuredContext(
            items = listOf(
                ContextBuilder.ContextItem(entity = "X", content = "Y", evidence = listOf("ev.1")),
            ),
            totalCharacters = 1,
            totalTokens = 0,
            truncated = false,
            evidenceIds = listOf("ev.1"),
        )

        val result = gate.evaluate(context, requiredEvidence = true)
        assertEquals(EvidenceGate.GateDecision.ALLOW, result.decision)

        try {
            gate.getBlockedResponse(result)
            fail("Should throw for ALLOW decision")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    // ── R2 PROOF: zero LLM calls without evidence ──────────────────

    @Test
    fun r2_proof_gateBlocksWhenNoRetrievalResults() {
        // Simulates the full flow: retrieval returns nothing → gate blocks → LLM never called
        val emptyContext = ContextBuilder.StructuredContext(
            items = emptyList(),
            totalCharacters = 0,
            totalTokens = 0,
            truncated = false,
            evidenceIds = emptyList(),
        )

        val gateResult = gate.evaluate(emptyContext, requiredEvidence = true)

        // THE CRITICAL ASSERTION: gate must block
        assertNotEquals(
            "R2 VIOLATION: Gate must NOT allow LLM call when context is empty",
            EvidenceGate.GateDecision.ALLOW,
            gateResult.decision
        )

        // The blocked response must indicate MODEL_NOT_CALLED
        val blockedResponse = gate.getBlockedResponse(gateResult)
        assertTrue(
            "R2 VIOLATION: Blocked response must contain MODEL_NOT_CALLED",
            blockedResponse.warnings.contains("MODEL_NOT_CALLED")
        )
    }

    @Test
    fun r2_proof_gateAllowsWhenEvidenceExists() {
        // Simulates: retrieval found results with evidence → gate allows → LLM may be called
        val contextWithEvidence = ContextBuilder.StructuredContext(
            items = listOf(
                ContextBuilder.ContextItem(
                    entity = "Insônia por Deficiência de Yin",
                    content = "A insônia por Deficiência de Yin se manifesta com dificuldade para conciliar o sono...",
                    evidence = listOf("ev.1", "ev.2"),
                    provenance = "Maciocia",
                ),
            ),
            totalCharacters = 100,
            totalTokens = 25,
            truncated = false,
            evidenceIds = listOf("ev.1", "ev.2"),
        )

        val gateResult = gate.evaluate(contextWithEvidence, requiredEvidence = true)

        assertEquals(
            "Gate must ALLOW when evidence exists",
            EvidenceGate.GateDecision.ALLOW,
            gateResult.decision
        )
        assertEquals(2, gateResult.evidenceCount)
    }
}
