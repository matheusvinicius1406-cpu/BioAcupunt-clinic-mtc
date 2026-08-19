package com.bioacupunt.clinic

import com.bioacupunt.clinic.domain.model.ObservationSource
import com.bioacupunt.clinic.domain.model.ObservationType
import com.bioacupunt.clinic.domain.usecase.ClinicalNlpUseCase
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for ClinicalNlpUseCase — deterministic regex-based extraction.
 *
 * Critical: this test must verify that the NLP does NOT invent diagnoses.
 * It extracts observations, never infers patterns or diagnoses.
 */
class ClinicalNlpUseCaseTest {

    private lateinit var useCase: ClinicalNlpUseCase

    @Before
    fun setup() {
        useCase = ClinicalNlpUseCase()
    }

    @Test
    fun extract_patientReported_symptoms_detected() {
        val result = useCase.extract("Paciente relata insônia há 2 semanas.")
        assertTrue("Should detect patient-reported symptom", result.symptoms.isNotEmpty())
        assertTrue("Symptom should contain 'insônia'", result.symptoms.any { it.name.lowercase().contains("insônia") })
    }

    @Test
    fun extract_temporalPatterns_detected() {
        val result = useCase.extract("Paciente relata dor que piora à noite.")
        assertTrue("Should detect temporal patterns", result.temporalPatterns.isNotEmpty())
        assertTrue("Should detect 'à noite'", result.temporalPatterns.any { it.description.contains("à noite") })
    }

    @Test
    fun extract_tongueFindings_detected() {
        val result = useCase.extract("Língua vermelha com saburra amarela.")
        assertTrue("Should detect tongue findings", result.findings.isNotEmpty())
        assertTrue("Should have tongue type", result.findings.any { it.type == ObservationType.TONGUE })
    }

    @Test
    fun extract_pulseFindings_detected() {
        val result = useCase.extract("Pulso rápido e fino.")
        assertTrue("Should detect pulse findings", result.findings.any { it.type == ObservationType.PULSE })
    }

    @Test
    fun extract_mtcPatterns_recognized() {
        val result = useCase.extract("Padrão de deficiência de qi e estagnação de qi.")
        assertTrue("Should recognize MTC patterns", result.recognizedEntities.isNotEmpty())
        assertTrue("Should find qi deficiency", result.recognizedEntities.any {
            it.text.lowercase().contains("deficiência de qi")
        })
    }

    @Test
    fun extract_acupoints_recognized() {
        val result = useCase.extract("Prescrito LI4, ST36, SP6.")
        assertTrue("Should recognize acupoint codes", result.recognizedEntities.isNotEmpty())
        assertTrue("Should find LI4", result.recognizedEntities.any { it.text == "LI4" })
        assertTrue("Should find ST36", result.recognizedEntities.any { it.text == "ST36" })
    }

    @Test
    fun extract_emptyText_noObservations() {
        val result = useCase.extract("")
        assertTrue("Empty text should have no observations", result.observations.isEmpty())
        assertTrue("Empty text should have no symptoms", result.symptoms.isEmpty())
    }

    @Test
    fun extract_negationDetected() {
        val result = useCase.extract("Paciente nega dor torácica.")
        assertTrue("Should detect negation", result.uncertainties.isNotEmpty())
    }

    @Test
    fun extract_neverGeneratesDiagnosis() {
        // Critical: NLP should extract observations, never patterns or diagnoses
        val result = useCase.extract("Paciente relata insônia, língua vermelha, pulso fino.")
        // Should have observations but no pattern diagnoses (pattern inference is ClinicalIntelligence's job)
        assertFalse("NLP should not infer ZangFu patterns",
            result.recognizedEntities.any { it.entityType == "DIAGNOSIS" })
    }

    @Test
    fun extract_confidenceScoresPresent() {
        val result = useCase.extract("Paciente relata cefaleia há 3 dias.")
        assertTrue("Should have confidence scores", result.symptoms.all { it.confidence > 0.0 })
    }
}
