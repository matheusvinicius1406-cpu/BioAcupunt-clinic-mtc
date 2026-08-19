package com.bioacupunt.copilot.rag

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * §25-26 RESPONSE VALIDATOR TEST
 *
 * Tests unsupported claim detection, citation checks, and validation reporting.
 */
class ResponseValidatorTest {

    private lateinit var validator: ResponseValidator

    @Before
    fun setup() {
        validator = ResponseValidator()
    }

    // ── Valid response ──────────────────────────────────────────────

    @Test
    fun validate_supportedClaims_returnsValid() {
        val response = GroundedResponseGenerator.GroundedResponse(
            answer = "A insônia por Deficiência de Yin é tratada com pontos como SP6 e HT7.",
            claims = listOf("insônia deficiência yin"),
            citations = listOf("Maciocia"),
            evidenceIds = listOf("ev.1"),
            knowledgeVersion = "1.0",
        )

        val context = ContextBuilder.StructuredContext(
            items = listOf(
                ContextBuilder.ContextItem(
                    entity = "Insônia por Deficiência de Yin",
                    content = "A insônia por deficiência de yin é uma condição comum tratada com acupuntura.",
                ),
            ),
            totalCharacters = 100,
            totalTokens = 25,
            truncated = false,
            evidenceIds = listOf("ev.1"),
        )

        val report = validator.validate(response, context)

        assertEquals(ResponseValidator.ValidationResult.VALID, report.result)
        assertTrue(report.issues.isEmpty())
    }

    // ── Missing citation ────────────────────────────────────────────

    @Test
    fun validate_evidenceExistsButNoCitation_warns() {
        val response = GroundedResponseGenerator.GroundedResponse(
            answer = "Resposta genérica.",
            claims = emptyList(),
            citations = emptyList(), // No citations!
            evidenceIds = listOf("ev.1"),
        )

        val context = ContextBuilder.StructuredContext(
            items = listOf(
                ContextBuilder.ContextItem(
                    entity = "Test",
                    content = "Content",
                    evidence = listOf("ev.1"),
                ),
            ),
            totalCharacters = 10,
            totalTokens = 2,
            truncated = false,
            evidenceIds = listOf("ev.1"),
        )

        val report = validator.validate(response, context)

        assertTrue(
            "Should warn about missing citation",
            report.issues.any { it.type == ResponseValidator.IssueType.MISSING_CITATION }
        )
    }

    // ── Unsupported claims ──────────────────────────────────────────

    @Test
    fun validate_unsupportedClaim_detected() {
        val response = GroundedResponseGenerator.GroundedResponse(
            answer = "Resposta com afirmação inventada.",
            claims = listOf("xyzabc123 terms not in context at all"),
            citations = listOf("Fonte"),
            evidenceIds = listOf("ev.1"),
        )

        val context = ContextBuilder.StructuredContext(
            items = listOf(
                ContextBuilder.ContextItem(
                    entity = "Test",
                    content = "Only contains simple words here",
                ),
            ),
            totalCharacters = 10,
            totalTokens = 2,
            truncated = false,
            evidenceIds = listOf("ev.1"),
        )

        val report = validator.validate(response, context)

        assertTrue(
            "Should detect unsupported claim",
            report.issues.any { it.type == ResponseValidator.IssueType.UNSUPPORTED_CLAIM }
        )
        assertEquals(
            ResponseValidator.ValidationResult.HAS_UNSUPPORTED_CLAIMS,
            report.result
        )
    }

    // ── Missing evidence ────────────────────────────────────────────

    @Test
    fun validate_emptyContextButAnswer_warns() {
        val response = GroundedResponseGenerator.GroundedResponse(
            answer = "Resposta gerada sem contexto.",
            claims = emptyList(),
            citations = emptyList(),
        )

        val context = ContextBuilder.StructuredContext(
            items = emptyList(),
            totalCharacters = 0,
            totalTokens = 0,
            truncated = false,
            evidenceIds = emptyList(),
        )

        val report = validator.validate(response, context)

        assertTrue(
            "Should warn about missing evidence",
            report.issues.any { it.type == ResponseValidator.IssueType.MISSING_EVIDENCE }
        )
    }

    // ── Low confidence ──────────────────────────────────────────────

    @Test
    fun validate_lowConfidence_reported() {
        val response = GroundedResponseGenerator.GroundedResponse(
            answer = "Resposta com baixa confiança.",
            confidence = "LOW",
        )

        val context = ContextBuilder.StructuredContext(
            items = emptyList(),
            totalCharacters = 0,
            totalTokens = 0,
            truncated = false,
            evidenceIds = emptyList(),
        )

        val report = validator.validate(response, context)

        assertTrue(
            "Should report low confidence",
            report.issues.any { it.type == ResponseValidator.IssueType.LOW_CONFIDENCE }
        )
    }

    // ── Qualify claims ──────────────────────────────────────────────

    @Test
    fun qualifyClaims_addsUncertaintyMarkers() {
        val response = GroundedResponseGenerator.GroundedResponse(
            answer = "Resposta",
            claims = listOf("Unsupported claim"),
            confidence = "MODERATE",
        )

        val qualified = validator.qualifyClaims(response, listOf("Unsupported claim"))

        assertTrue(
            "Should add uncertainty for unsupported claim",
            qualified.uncertainties.any { it.contains("Unsupported claim") }
        )
        assertEquals("LOW", qualified.confidence)
    }

    // ── Deterministic ───────────────────────────────────────────────

    @Test
    fun validate_deterministic_sameInputSameOutput() {
        val response = GroundedResponseGenerator.GroundedResponse(
            answer = "Test",
            claims = listOf("claim"),
            citations = listOf("citation"),
            evidenceIds = listOf("ev.1"),
        )

        val context = ContextBuilder.StructuredContext(
            items = listOf(
                ContextBuilder.ContextItem(entity = "X", content = "Y"),
            ),
            totalCharacters = 1,
            totalTokens = 0,
            truncated = false,
            evidenceIds = listOf("ev.1"),
        )

        val report1 = validator.validate(response, context)
        val report2 = validator.validate(response, context)

        assertEquals(report1.result, report2.result)
        assertEquals(report1.issues.size, report2.issues.size)
    }

    // ── No issues on clean response ─────────────────────────────────

    @Test
    fun validate_cleanResponse_noIssues() {
        val response = GroundedResponseGenerator.GroundedResponse(
            answer = "Resposta fundamentada.",
            claims = listOf("insônia é tratada com acupuntura"),
            citations = listOf("Maciocia, Foundations"),
            evidenceIds = listOf("ev.1"),
            confidence = "HIGH",
            knowledgeVersion = "1.0",
        )

        val context = ContextBuilder.StructuredContext(
            items = listOf(
                ContextBuilder.ContextItem(
                    entity = "Insônia",
                    content = "Insônia é tratada com acupuntura em pontos como SP6 e HT7.",
                    evidence = listOf("ev.1"),
                ),
            ),
            totalCharacters = 50,
            totalTokens = 12,
            truncated = false,
            evidenceIds = listOf("ev.1"),
        )

        val report = validator.validate(response, context)

        assertEquals(ResponseValidator.ValidationResult.VALID, report.result)
    }
}
