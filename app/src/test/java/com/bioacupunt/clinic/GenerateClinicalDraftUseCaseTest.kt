package com.bioacupunt.clinic

import com.bioacupunt.clinic.domain.model.Encounter
import com.bioacupunt.clinic.domain.model.EncounterStatus
import com.bioacupunt.clinic.domain.model.EncounterType
import com.bioacupunt.clinic.domain.model.NoteFormat
import com.bioacupunt.clinic.domain.model.ObservationSource
import com.bioacupunt.clinic.domain.model.ObservationStatus
import com.bioacupunt.clinic.domain.model.ObservationType
import com.bioacupunt.clinic.domain.model.StructuredObservation
import com.bioacupunt.clinic.domain.usecase.GenerateClinicalDraftUseCase
import com.bioacupunt.prontuario.domain.model.MtcAssessment
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GenerateClinicalDraftUseCaseTest {

    private lateinit var useCase: GenerateClinicalDraftUseCase

    @Before
    fun setup() {
        useCase = GenerateClinicalDraftUseCase()
    }

    @Test
    fun generate_withFullData_createsDraft() {
        val encounter = makeEncounter()
        val observations = listOf(
            makeObs("Insônia", ObservationType.SYMPTOM, ObservationSource.PATIENT_REPORTED),
            makeObs("Língua vermelha", ObservationType.TONGUE, ObservationSource.PRACTITIONER_OBSERVED),
        )
        val assessment = MtcAssessment(
            id = 1, patientId = 1,
            chiefComplaint = "Insônia",
            clinicalImpression = "Deficiência de Yin",
        )

        val draft = useCase.generate(encounter, observations, assessment)

        assertNotNull(draft)
        assertEquals(encounter.id, draft.encounterId)
        assertEquals(encounter.patientId, draft.patientId)
        assertEquals(NoteFormat.SOAP, draft.format)
        assertTrue(draft.subjective.contains("Insônia"))
        assertTrue(draft.objective.contains("Língua vermelha"))
        assertTrue(draft.assessment.contains("Insônia"))
    }

    @Test
    fun generate_noObservations_warningsPresent() {
        val encounter = makeEncounter()
        val draft = useCase.generate(encounter, emptyList(), null)
        assertTrue(draft.warnings.any { it.contains("observação") })
    }

    @Test
    fun generate_noAssessment_warningPresent() {
        val encounter = makeEncounter()
        val draft = useCase.generate(encounter, listOf(makeObs("Dor")), null)
        assertTrue(draft.warnings.any { it.contains("Avaliação MTC") })
    }

    @Test
    fun generate_statusIsDraft() {
        val draft = useCase.generate(makeEncounter(), emptyList(), null)
        assertEquals("INSUFFICIENT", draft.confidence)
    }

    @Test
    fun generate_withAssessment_hasHigherConfidence() {
        val assessment = MtcAssessment(
            id = 1, patientId = 1,
            chiefComplaint = "Dor",
            clinicalImpression = "Estagnação",
            patterns = listOf(),
        )
        val draft = useCase.generate(makeEncounter(), listOf(makeObs("Dor")), assessment)
        assertNotEquals("DRAFT", draft.confidence)
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
