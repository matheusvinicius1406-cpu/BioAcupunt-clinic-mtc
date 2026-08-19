package com.bioacupunt.copilot.rag

import com.bioacupunt.copilot.retrieval.RetrievalHit
import com.bioacupunt.copilot.retrieval.RetrievalSource
import com.bioacupunt.copilot.retrieval.UnifiedRetrievalResult
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * §17-18 CONTEXT BUILDER TEST
 *
 * Tests that the context builder respects budget limits:
 * - maxDocuments
 * - maxCharacters
 * - maxTokens
 * - maxEvidenceItems
 *
 * And that prioritization works correctly.
 */
class ContextBuilderTest {

    private lateinit var builder: ContextBuilder

    @Before
    fun setup() {
        builder = ContextBuilder()
    }

    // ── Empty retrieval ─────────────────────────────────────────────

    @Test
    fun build_emptyRetrieval_returnsEmptyContext() {
        val result = UnifiedRetrievalResult(
            results = emptyList(),
            totalCandidates = 0,
            retrievalLatencyMs = 0,
            sourceBreakdown = emptyMap(),
        )

        val context = builder.build(result, "test query")

        assertTrue(context.items.isEmpty())
        assertEquals(0, context.totalCharacters)
        assertEquals(0, context.totalTokens)
        assertFalse(context.truncated)
    }

    // ── Max documents ───────────────────────────────────────────────

    @Test
    fun build_respectsMaxDocuments() {
        val hits = (1..30).map { i ->
            RetrievalHit(
                entityId = "entity.$i",
                content = "Content for entity $i",
                score = 1.0,
                normalizedScore = 1.0,
                sourceType = RetrievalSource.LEXICAL,
            )
        }

        val result = UnifiedRetrievalResult(
            results = hits,
            totalCandidates = 30,
            retrievalLatencyMs = 0,
            sourceBreakdown = emptyMap(),
        )

        val context = builder.build(result, "test query")

        assertTrue(
            "Should not exceed maxDocuments (20)",
            context.items.size <= 20
        )
    }

    // ── Max characters ──────────────────────────────────────────────

    @Test
    fun build_respectsMaxCharacters() {
        val longContent = "A".repeat(5000)
        val hits = (1..5).map { i ->
            RetrievalHit(
                entityId = "entity.$i",
                content = longContent,
                score = 1.0,
                normalizedScore = 1.0,
                sourceType = RetrievalSource.LEXICAL,
            )
        }

        val result = UnifiedRetrievalResult(
            results = hits,
            totalCandidates = 5,
            retrievalLatencyMs = 0,
            sourceBreakdown = emptyMap(),
        )

        val context = builder.build(result, "test query")

        assertTrue(
            "Should not exceed maxCharacters (8000): got ${context.totalCharacters}",
            context.totalCharacters <= 8000
        )
    }

    // ── Truncation indicator ────────────────────────────────────────

    @Test
    fun build_indicatesTruncation() {
        val hits = (1..30).map { i ->
            RetrievalHit(
                entityId = "entity.$i",
                content = "Content for entity $i with enough text to fill characters",
                score = 1.0,
                normalizedScore = 1.0,
                sourceType = RetrievalSource.LEXICAL,
            )
        }

        val result = UnifiedRetrievalResult(
            results = hits,
            totalCandidates = 30,
            retrievalLatencyMs = 0,
            sourceBreakdown = emptyMap(),
        )

        val context = builder.build(result, "test query")

        assertTrue(
            "Context should indicate truncation when not all results fit",
            context.truncated
        )
    }

    // ── Evidence IDs preserved ──────────────────────────────────────

    @Test
    fun build_preservesEvidenceIds() {
        val hits = listOf(
            RetrievalHit(
                entityId = "entity.1",
                content = "Content",
                evidenceIds = listOf("ev.1", "ev.2"),
                score = 1.0,
                normalizedScore = 1.0,
                sourceType = RetrievalSource.LEXICAL,
            ),
        )

        val result = UnifiedRetrievalResult(
            results = hits,
            totalCandidates = 1,
            retrievalLatencyMs = 0,
            sourceBreakdown = emptyMap(),
        )

        val context = builder.build(result, "test query")

        assertTrue(context.evidenceIds.contains("ev.1"))
        assertTrue(context.evidenceIds.contains("ev.2"))
    }

    // ── Evidence IDs deduplicated ───────────────────────────────────

    @Test
    fun build_deduplicatesEvidenceIds() {
        val hits = listOf(
            RetrievalHit(
                entityId = "entity.1",
                content = "Content A",
                evidenceIds = listOf("ev.1", "ev.2"),
                score = 1.0,
                normalizedScore = 1.0,
                sourceType = RetrievalSource.LEXICAL,
            ),
            RetrievalHit(
                entityId = "entity.2",
                content = "Content B",
                evidenceIds = listOf("ev.2", "ev.3"),
                score = 0.8,
                normalizedScore = 0.8,
                sourceType = RetrievalSource.LEXICAL,
            ),
        )

        val result = UnifiedRetrievalResult(
            results = hits,
            totalCandidates = 2,
            retrievalLatencyMs = 0,
            sourceBreakdown = emptyMap(),
        )

        val context = builder.build(result, "test query")

        // ev.2 should appear only once
        assertEquals(1, context.evidenceIds.count { it == "ev.2" })
        assertEquals(3, context.evidenceIds.size) // ev.1, ev.2, ev.3
    }

    // ── Priority: evidence first ────────────────────────────────────

    @Test
    fun build_prioritizesHitsWithEvidence() {
        val hitWithoutEvidence = RetrievalHit(
            entityId = "entity.no-ev",
            content = "No evidence",
            evidenceIds = emptyList(),
            score = 0.9,
            normalizedScore = 0.9,
            sourceType = RetrievalSource.LEXICAL,
        )
        val hitWithEvidence = RetrievalHit(
            entityId = "entity.with-ev",
            content = "Has evidence",
            evidenceIds = listOf("ev.1"),
            score = 0.5,
            normalizedScore = 0.5,
            sourceType = RetrievalSource.LEXICAL,
        )

        val result = UnifiedRetrievalResult(
            results = listOf(hitWithoutEvidence, hitWithEvidence),
            totalCandidates = 2,
            retrievalLatencyMs = 0,
            sourceBreakdown = emptyMap(),
        )

        val context = builder.build(result, "test query")

        // Hit with evidence should come first (lower priority number = higher priority)
        assertTrue(
            "Hit with evidence should be prioritized",
            context.items.first().entity == "entity.with-ev"
        )
    }

    // ── Source type preserved ───────────────────────────────────────

    @Test
    fun build_preservesSourceType() {
        val hits = listOf(
            RetrievalHit(
                entityId = "entity.1",
                content = "Graph result",
                score = 1.0,
                normalizedScore = 1.0,
                sourceType = RetrievalSource.GRAPH,
            ),
        )

        val result = UnifiedRetrievalResult(
            results = hits,
            totalCandidates = 1,
            retrievalLatencyMs = 0,
            sourceBreakdown = emptyMap(),
        )

        val context = builder.build(result, "test query")

        assertEquals("GRAPH", context.items.first().sourceType)
    }

    // ── Format for prompt ───────────────────────────────────────────

    @Test
    fun formatForPrompt_containsHeaders() {
        val result = UnifiedRetrievalResult(
            results = listOf(
                RetrievalHit(
                    entityId = "entity.1",
                    content = "Test content",
                    score = 1.0,
                    normalizedScore = 1.0,
                    sourceType = RetrievalSource.LEXICAL,
                ),
            ),
            totalCandidates = 1,
            retrievalLatencyMs = 0,
            sourceBreakdown = emptyMap(),
        )

        val context = builder.build(result, "test query")
        val formatted = builder.formatForPrompt(context)

        assertTrue(formatted.contains("CONTEXTO CLÍNICO"))
        assertTrue(formatted.contains("Test content"))
    }

    // ── Deterministic ───────────────────────────────────────────────

    @Test
    fun build_deterministic_sameInputSameOutput() {
        val hits = listOf(
            RetrievalHit(
                entityId = "entity.1",
                content = "Content",
                evidenceIds = listOf("ev.1"),
                score = 1.0,
                normalizedScore = 1.0,
                sourceType = RetrievalSource.LEXICAL,
            ),
        )

        val result = UnifiedRetrievalResult(
            results = hits,
            totalCandidates = 1,
            retrievalLatencyMs = 0,
            sourceBreakdown = emptyMap(),
        )

        val ctx1 = builder.build(result, "test query")
        val ctx2 = builder.build(result, "test query")

        assertEquals(ctx1.items.size, ctx2.items.size)
        assertEquals(ctx1.totalCharacters, ctx2.totalCharacters)
        assertEquals(ctx1.evidenceIds, ctx2.evidenceIds)
    }

    // ── Custom budget ───────────────────────────────────────────────

    @Test
    fun build_customBudget_respectsLimits() {
        val smallBuilder = ContextBuilder(
            ContextBuilder.ContextBudget(
                maxDocuments = 3,
                maxCharacters = 100,
                maxTokens = 30,
            )
        )

        val hits = (1..10).map { i ->
            RetrievalHit(
                entityId = "entity.$i",
                content = "Long content for entity $i that takes up space",
                score = 1.0,
                normalizedScore = 1.0,
                sourceType = RetrievalSource.LEXICAL,
            )
        }

        val result = UnifiedRetrievalResult(
            results = hits,
            totalCandidates = 10,
            retrievalLatencyMs = 0,
            sourceBreakdown = emptyMap(),
        )

        val context = smallBuilder.build(result, "test query")

        assertTrue(context.items.size <= 3)
        assertTrue(context.totalCharacters <= 100)
    }
}
