package com.bioacupunt.copilot.retrieval

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * §5 INTENT DETECTOR TEST
 *
 * Tests deterministic intent classification for all MTC query types.
 */
class IntentDetectorTest {

    private lateinit var detector: IntentDetector

    @Before
    fun setup() {
        detector = IntentDetector()
    }

    // ── Patient Summary ─────────────────────────────────────────────

    @Test
    fun detect_resumoPaciente_patientSummary() {
        val result = detector.detect("resumo do paciente")
        assertEquals(IntentType.PATIENT_SUMMARY, result.intent)
        assertTrue(result.confidence >= 0.8)
    }

    @Test
    fun detect_evolucaoPaciente_patientSummary() {
        val result = detector.detect("evolução do paciente")
        assertEquals(IntentType.PATIENT_SUMMARY, result.intent)
    }

    @Test
    fun detect_timeline_patientSummary() {
        val result = detector.detect("timeline")
        assertEquals(IntentType.PATIENT_SUMMARY, result.intent)
    }

    // ── Differential Explanation ────────────────────────────────────

    @Test
    fun detect_porQueAcima_differential() {
        val result = detector.detect("por que A está acima de B?")
        assertEquals(IntentType.DIFFERENTIAL_EXPLANATION, result.intent)
    }

    @Test
    fun detect_diferencaEntre_differential() {
        val result = detector.detect("diferença entre insônia e ansiedade")
        assertEquals(IntentType.DIFFERENTIAL_EXPLANATION, result.intent)
    }

    @Test
    fun detect_porQuePrimeiro_differential() {
        val result = detector.detect("por que esse padrão aparece primeiro?")
        assertEquals(IntentType.DIFFERENTIAL_EXPLANATION, result.intent)
    }

    // ── Missing Data ────────────────────────────────────────────────

    @Test
    fun detect_oQueFalta_missingData() {
        val result = detector.detect("o que falta?")
        assertEquals(IntentType.MISSING_DATA, result.intent)
    }

    @Test
    fun detect_faltando_missingData() {
        val result = detector.detect("está faltando algum dado?")
        assertEquals(IntentType.MISSING_DATA, result.intent)
    }

    @Test
    fun detect_precisoDeMais_missingData() {
        val result = detector.detect("preciso de mais informações")
        assertEquals(IntentType.MISSING_DATA, result.intent)
    }

    // ── Evidence Lookup ─────────────────────────────────────────────

    @Test
    fun detect_qualAFonte_evidenceLookup() {
        val result = detector.detect("qual a fonte?")
        assertEquals(IntentType.EVIDENCE_LOOKUP, result.intent)
    }

    @Test
    fun detect_evidencia_evidenceLookup() {
        val result = detector.detect("evidência para isso")
        assertEquals(IntentType.EVIDENCE_LOOKUP, result.intent)
    }

    @Test
    fun detect_artigo_evidenceLookup() {
        val result = detector.detect("artigo sobre isso")
        assertEquals(IntentType.EVIDENCE_LOOKUP, result.intent)
    }

    // ── Point Lookup ────────────────────────────────────────────────

    @Test
    fun detect_ponto_pointLookup() {
        val result = detector.detect("ponto de acupuntura")
        assertEquals(IntentType.POINT_LOOKUP, result.intent)
    }

    @Test
    fun detect_meridiano_pointLookup() {
        val result = detector.detect("meridiano do fígado")
        assertEquals(IntentType.POINT_LOOKUP, result.intent)
    }

    @Test
    fun detect_li4_pointLookup() {
        val result = detector.detect("LI4")
        assertEquals(IntentType.POINT_LOOKUP, result.intent)
        assertTrue(result.confidence >= 0.8)
    }

    @Test
    fun detect_st36_pointLookup() {
        val result = detector.detect("ST36")
        assertEquals(IntentType.POINT_LOOKUP, result.intent)
    }

    // ── Formula Lookup ──────────────────────────────────────────────

    @Test
    fun detect_formula_formulaLookup() {
        val result = detector.detect("fórmula para insônia")
        assertEquals(IntentType.FORMULA_LOOKUP, result.intent)
    }

    @Test
    fun detect_fitoterapico_formulaLookup() {
        val result = detector.detect("fitoterápico para dor")
        assertEquals(IntentType.FORMULA_LOOKUP, result.intent)
    }

    // ── Protocol Lookup ─────────────────────────────────────────────

    @Test
    fun detect_protocolo_protocolLookup() {
        val result = detector.detect("protocolo de tratamento")
        assertEquals(IntentType.PROTOCOL_LOOKUP, result.intent)
    }

    @Test
    fun detect_comoTratar_protocolLookup() {
        val result = detector.detect("como tratar insônia")
        assertEquals(IntentType.PROTOCOL_LOOKUP, result.intent)
    }

    // ── Clinical Analysis ───────────────────────────────────────────

    @Test
    fun detect_diagnostico_clinicalAnalysis() {
        val result = detector.detect("diagnóstico MTC")
        assertEquals(IntentType.CLINICAL_ANALYSIS, result.intent)
    }

    @Test
    fun detect_sindrome_clinicalAnalysis() {
        val result = detector.detect("síndrome de estagnação")
        assertEquals(IntentType.CLINICAL_ANALYSIS, result.intent)
    }

    @Test
    fun detect_zangFu_clinicalAnalysis() {
        val result = detector.detect("zang fu")
        assertEquals(IntentType.CLINICAL_ANALYSIS, result.intent)
    }

    // ── Research Query ──────────────────────────────────────────────

    @Test
    fun detect_pesquise_researchQuery() {
        val result = detector.detect("pesquise sobre acupuncture")
        assertEquals(IntentType.RESEARCH_QUERY, result.intent)
    }

    @Test
    fun detect_estude_researchQuery() {
        val result = detector.detect("estude a literatura sobre dor")
        assertEquals(IntentType.RESEARCH_QUERY, result.intent)
    }

    // ── Default: Knowledge Search ───────────────────────────────────

    @Test
    fun detect_genericQuery_knowledgeSearch() {
        val result = detector.detect("o que é isso?")
        assertEquals(IntentType.KNOWLEDGE_SEARCH, result.intent)
    }

    @Test
    fun detect_unknownTerm_knowledgeSearch() {
        val result = detector.detect("xyzabc123")
        assertEquals(IntentType.KNOWLEDGE_SEARCH, result.intent)
    }

    // ── Deterministic ───────────────────────────────────────────────

    @Test
    fun detect_deterministic_sameInputSameOutput() {
        val query = "por que A está acima de B?"
        val result1 = detector.detect(query)
        val result2 = detector.detect(query)

        assertEquals(result1.intent, result2.intent)
        assertEquals(result1.confidence, result2.confidence, 0.001)
    }

    // ── Case insensitivity ──────────────────────────────────────────

    @Test
    fun detect_caseInsensitive() {
        val lower = detector.detect("resumo do paciente")
        val upper = detector.detect("RESUMO DO PACIENTE")
        val mixed = detector.detect("Resumo Do Paciente")

        assertEquals(lower.intent, upper.intent)
        assertEquals(lower.intent, mixed.intent)
    }

    // ── Empty query ─────────────────────────────────────────────────

    @Test
    fun detect_emptyQuery_knowledgeSearch() {
        val result = detector.detect("")
        assertEquals(IntentType.KNOWLEDGE_SEARCH, result.intent)
    }

    @Test
    fun detect_blankQuery_knowledgeSearch() {
        val result = detector.detect("   ")
        assertEquals(IntentType.KNOWLEDGE_SEARCH, result.intent)
    }

    // ── Context requirements ────────────────────────────────────────

    @Test
    fun detect_patientSummary_requiresPatientId() {
        val result = detector.detect("resumo do paciente")
        assertTrue(result.contextRequirements.contains("patientId"))
    }

    @Test
    fun detect_differential_requiresDifferentialResults() {
        val result = detector.detect("por que A está acima de B?")
        assertTrue(result.contextRequirements.contains("differentialResults"))
    }
}
