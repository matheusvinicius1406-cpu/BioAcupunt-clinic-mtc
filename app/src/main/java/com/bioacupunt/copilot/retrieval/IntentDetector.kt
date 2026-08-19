package com.bioacupunt.copilot.retrieval

/**
 * §5 INTENT DETECTOR
 *
 * Deterministic intent classification — no LLM required for routing.
 * Uses keyword/pattern matching against MTC domain knowledge.
 *
 * Returns [IntentResult] with intent, confidence, and extracted hints.
 */
class IntentDetector {

    data class IntentResult(
        val intent: IntentType,
        val confidence: Double,
        val entities: List<RecognizedEntity> = emptyList(),
        val contextRequirements: Set<String> = emptySet(),
    )

    /**
     * Classify user query into an intent type.
     * Deterministic: same query → same result, always.
     */
    fun detect(query: String): IntentResult {
        val lower = query.lowercase().trim()

        // Priority-ordered pattern matching
        return when {
            // Patient summary
            lower.containsAny("resumo do paciente", "resumo clínico", "evolução do paciente",
                "histórico do paciente", "timeline") -> IntentResult(
                intent = IntentType.PATIENT_SUMMARY,
                confidence = 0.9,
                contextRequirements = setOf("patientId", "timeline"),
            )

            // Differential explanation
            lower.containsAny("por que", "porque", "diferença entre", "diferenciar",
                "por que está acima", "por que primeiro") -> IntentResult(
                intent = IntentType.DIFFERENTIAL_EXPLANATION,
                confidence = 0.85,
                contextRequirements = setOf("differentialResults"),
            )

            // Missing data
            lower.containsAny("o que falta", "faltando", "dados incompletos",
                "preciso de mais", "falta informação") -> IntentResult(
                intent = IntentType.MISSING_DATA,
                confidence = 0.85,
                contextRequirements = setOf("missingDataResults"),
            )

            // Evidence lookup
            lower.containsAny("fonte", "evidência", "referência", "artigo",
                "onde diz", "qual a fonte") -> IntentResult(
                intent = IntentType.EVIDENCE_LOOKUP,
                confidence = 0.85,
                contextRequirements = setOf("entityId"),
            )

            // Point/acupoint lookup
            lower.containsAny("ponto", "acupuntura", "meridiano", "canal",
                "li4", "st36", "sp6", "pc6", "gb34") -> IntentResult(
                intent = IntentType.POINT_LOOKUP,
                confidence = 0.9,
                contextRequirements = setOf("acupointId"),
            )

            // Formula lookup
            lower.containsAny("fórmula", "formula", "prescrição", "fitoterápico",
                "erva", "chá", "decocto") -> IntentResult(
                intent = IntentType.FORMULA_LOOKUP,
                confidence = 0.85,
                contextRequirements = setOf("formulaId"),
            )

            // Protocol lookup
            lower.containsAny("protocolo", "tratamento", "protoclo",
                "como tratar", "conduta") -> IntentResult(
                intent = IntentType.PROTOCOL_LOOKUP,
                confidence = 0.85,
                contextRequirements = setOf("conditionId"),
            )

            // Clinical analysis
            lower.containsAny("diagnóstico", "diagnostico", "síndrome",
                "padrão", "pattern", "zang fu", "ba gang", "qi",
                "sangue", "yin", "yang") -> IntentResult(
                intent = IntentType.CLINICAL_ANALYSIS,
                confidence = 0.8,
                contextRequirements = setOf("assessmentData"),
            )

            // Research query
            lower.containsAny("pesquise", "busque", "procure", "estude",
                "artigo sobre", "estudo sobre", "literatura") -> IntentResult(
                intent = IntentType.RESEARCH_QUERY,
                confidence = 0.7,
                contextRequirements = emptySet(),
            )

            // Default: knowledge search
            else -> IntentResult(
                intent = IntentType.KNOWLEDGE_SEARCH,
                confidence = 0.5,
                contextRequirements = emptySet(),
            )
        }
    }

    private fun String.containsAny(vararg patterns: String): Boolean =
        patterns.any { this.contains(it) }
}
