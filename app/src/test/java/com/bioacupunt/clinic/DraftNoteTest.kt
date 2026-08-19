package com.bioacupunt.clinic

import com.bioacupunt.clinic.domain.model.*
import com.bioacupunt.clinic.domain.usecase.GenerateClinicalDraftUseCase
import com.bioacupunt.prontuario.domain.model.MtcAssessment
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for DraftNote generation.
 *
 * Critical rule: AI → DRAFT → PROFESSIONAL REVIEW → FINAL
 * Never AI → FINAL directly.
 */
class DraftNoteTest {

    private lateinit var useCase: GenerateClinicalDraftUseCase

    @Before
    fun setup() {
        useCase = GenerateClinicalDraftUseCase()
    }

    @Test
    fun draft_neverAutoFinalizes() {
        val encounter = makeEncounter()
        val draft = useCase.generate(encounter, emptyList(), null)
        // Draft should be DRAFT status, never FINAL
        assertNotEquals(NoteStatus.FINAL.name, draft.confidence)
    }

    @Test
    fun draft_subjectiveFromPatientReported() {
        val encounter = makeEncounter()
        val observations = listOf(
            makeObs("Insônia", ObservationType.SLEEP, ObservationSource.PATIENT_REPORTED),
            makeObs("Dor lombar", ObservationType.PAIN, ObservationSource.PRACTITIONER_OBSERVED),
        )
        val draft = useCase.generate(encounter, observations, null)
        assertTrue("Subjective should contain patient-reported", draft.subjective.contains("Insônia"))
        assertFalse("Subjective should not contain practitioner-observed", draft.subjective.contains("Dor lombar"))
    }

    @Test
    fun draft_objectiveFromPractitionerObserved() {
        val encounter = makeEncounter()
        val observations = listOf(
            makeObs("Dor lombar", ObservationType.PAIN, ObservationSource.PRACTITIONER_OBSERVED),
        )
        val draft = useCase.generate(encounter, observations, null)
        assertTrue("Objective should contain practitioner-observed", draft.objective.contains("Dor lombar"))
    }

    @Test
    fun draft_warningsForMissingData() {
        val encounter = makeEncounter()
        val draft = useCase.generate(encounter, emptyList(), null)
        assertTrue("Should warn about missing observations", draft.warnings.any { it.contains("observação") })
        assertTrue("Should warn about missing assessment", draft.warnings.any { it.contains("Avaliação MTC") })
    }

    @Test
    fun draft_assessmentFromMtcAssessment() {
        val encounter = makeEncounter()
        val assessment = MtcAssessment(
            id = 1, patientId = 1,
            chiefComplaint = "Insônia",
            clinicalImpression = "Deficiência de Yin",
        )
        val draft = useCase.generate(encounter, emptyList(), assessment)
        assertTrue("Assessment should contain chief complaint", draft.assessment.contains("Insônia"))
        assertTrue("Assessment should contain clinical impression", draft.assessment.contains("Deficiência de Yin"))
    }

    private fun makeEncounter() = Encounter(
        id = 1,
        tenantId = 1,
        patientId = 1,
        status = EncounterStatus.COMPLETED,
        type = EncounterType.ACUPUNCTURE,
        startedAt = "2026-01-01T10:00:00Z",
        endedAt = "2026-01-01T11:00:00Z",
    )

    private fun makeObs(
        content: String,
        type: ObservationType = ObservationType.SYMPTOM,
        source: ObservationSource = ObservationSource.PATIENT_REPORTED,
    ) = StructuredObservation(
        id = 0,
        tenantId = 1,
        encounterId = 1,
        patientId = 1,
        type = type,
        content = content,
        status = ObservationStatus.CONFIRMED,
        source = source,
        createdAt = "2026-01-01",
    )
}
