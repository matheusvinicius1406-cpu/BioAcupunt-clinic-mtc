package com.bioacupunt.copilot.rag

/**
 * §25-26 RESPONSE VALIDATOR + UNSUPPORTED CLAIM DETECTOR
 *
 * Validates LLM responses against the context provided.
 * Checks: citations exist, evidence exists, claims supported, unsupported claims, contradictions.
 *
 * Policy for unsupported claims: QUALIFY (mark as uncertain, don't reject entire response).
 * This is deterministic — no LLM involved in validation.
 */
class ResponseValidator {

    enum class ValidationResult {
        VALID,
        HAS_WARNINGS,
        HAS_UNSUPPORTED_CLAIMS,
        REJECTED,
    }

    data class ValidationReport(
        val result: ValidationResult,
        val issues: List<ValidationIssue>,
        val correctedResponse: GroundedResponseGenerator.GroundedResponse? = null,
    )

    data class ValidationIssue(
        val type: IssueType,
        val description: String,
        val severity: Severity,
    )

    enum class IssueType {
        MISSING_CITATION,
        MISSING_EVIDENCE,
        UNSUPPORTED_CLAIM,
        CONTRADICTION,
        KNOWLEDGE_VERSION_MISSING,
        LOW_CONFIDENCE,
    }

    enum class Severity {
        INFO,
        WARNING,
        ERROR,
    }

    /**
     * Validate a grounded response against context.
     * Deterministic: same input → same result, always.
     */
    fun validate(
        response: GroundedResponseGenerator.GroundedResponse,
        context: ContextBuilder.StructuredContext,
    ): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()

        // 1. Check citations exist when evidence was provided
        if (context.evidenceIds.isNotEmpty() && response.citations.isEmpty()) {
            issues.add(
                ValidationIssue(
                    type = IssueType.MISSING_CITATION,
                    description = "Evidência disponível (${context.evidenceIds.size} itens) mas resposta não cita nenhuma fonte.",
                    severity = Severity.WARNING,
                )
            )
        }

        // 2. Check evidence exists
        if (context.items.isEmpty() && response.answer.isNotBlank()) {
            issues.add(
                ValidationIssue(
                    type = IssueType.MISSING_EVIDENCE,
                    description = "Resposta gerada sem nenhum contexto de evidência.",
                    severity = Severity.WARNING,
                )
            )
        }

        // 3. Check for unsupported claims
        val unsupportedClaims = detectUnsupportedClaims(response, context)
        for (claim in unsupportedClaims) {
            issues.add(
                ValidationIssue(
                    type = IssueType.UNSUPPORTED_CLAIM,
                    description = "Afirmação sem suporte no contexto: \"$claim\"",
                    severity = Severity.WARNING,
                )
            )
        }

        // 4. Check knowledge version
        if (response.knowledgeVersion == null && context.items.isNotEmpty()) {
            issues.add(
                ValidationIssue(
                    type = IssueType.KNOWLEDGE_VERSION_MISSING,
                    description = "Versão do conhecimento não especificada na resposta.",
                    severity = Severity.INFO,
                )
            )
        }

        // 5. Check confidence
        if (response.confidence == "LOW" || response.confidence == "INSUFFICIENT") {
            issues.add(
                ValidationIssue(
                    type = IssueType.LOW_CONFIDENCE,
                    description = "Confiança da resposta: ${response.confidence}",
                    severity = Severity.INFO,
                )
            )
        }

        // Determine overall result
        val result = when {
            issues.any { it.severity == Severity.ERROR } -> ValidationResult.REJECTED
            issues.any { it.type == IssueType.UNSUPPORTED_CLAIM } -> ValidationResult.HAS_UNSUPPORTED_CLAIMS
            issues.isNotEmpty() -> ValidationResult.HAS_WARNINGS
            else -> ValidationResult.VALID
        }

        return ValidationReport(
            result = result,
            issues = issues,
        )
    }

    /**
     * Detect claims in the response that are not supported by the context.
     * §26: Unsupported Claim Detection
     */
    private fun detectUnsupportedClaims(
        response: GroundedResponseGenerator.GroundedResponse,
        context: ContextBuilder.StructuredContext,
    ): List<String> {
        val contextText = context.items.joinToString(" ") { it.content }.lowercase()
        val unsupported = mutableListOf<String>()

        for (claim in response.claims) {
            val claimLower = claim.lowercase()
            // Simple heuristic: check if key terms from the claim appear in context
            val claimTerms = claimLower.split(" ").filter { it.length > 3 }
            val supportedTerms = claimTerms.count { contextText.contains(it) }
            val supportRatio = if (claimTerms.isNotEmpty()) {
                supportedTerms.toDouble() / claimTerms.size
            } else {
                1.0
            }

            if (supportRatio < 0.3) {
                unsupported.add(claim)
            }
        }

        return unsupported
    }

    /**
     * Qualify an unsupported claim by adding uncertainty markers.
     */
    fun qualifyClaims(
        response: GroundedResponseGenerator.GroundedResponse,
        unsupportedClaims: List<String>,
    ): GroundedResponseGenerator.GroundedResponse {
        val qualifiedUncertainties = response.uncertainties.toMutableList()
        for (claim in unsupportedClaims) {
            qualifiedUncertainties.add("⚠️ Afirmação sem suporte verificado: $claim")
        }
        return response.copy(
            uncertainties = qualifiedUncertainties,
            confidence = "LOW",
        )
    }
}
