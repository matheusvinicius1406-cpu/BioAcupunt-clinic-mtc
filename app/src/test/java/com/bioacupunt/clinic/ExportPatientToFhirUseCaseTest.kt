package com.bioacupunt.clinic

import com.bioacupunt.clinic.domain.model.Encounter
import com.bioacupunt.clinic.domain.model.EncounterStatus
import com.bioacupunt.clinic.domain.model.EncounterType
import com.bioacupunt.clinic.domain.model.NoteFormat
import com.bioacupunt.clinic.domain.model.NoteStatus
import com.bioacupunt.clinic.domain.model.ClinicalNote
import com.bioacupunt.clinic.domain.model.ObservationSource
import com.bioacupunt.clinic.domain.model.ObservationStatus
import com.bioacupunt.clinic.domain.model.ObservationType
import com.bioacupunt.clinic.domain.model.StructuredObservation
import com.bioacupunt.clinic.domain.model.TreatmentPlan
import com.bioacupunt.clinic.domain.model.TreatmentPlanStatus
import com.bioacupunt.clinic.domain.model.FollowUp
import com.bioacupunt.clinic.domain.model.FollowUpStatus
import com.bioacupunt.clinic.domain.usecase.ExportPatientToFhirUseCase
import com.bioacupunt.patient.domain.model.Patient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ExportPatientToFhirUseCaseTest {

    private lateinit var useCase: ExportPatientToFhirUseCase

    @Before
    fun setup() {
        useCase = ExportPatientToFhirUseCase()
    }

    @Test
    fun export_withAllData_createsBundle() {
        val patient = Patient(id = 1, name = "Maria Silva")
        val encounters = listOf(
            Encounter(id = 1, tenantId = 1, patientId = 1, status = EncounterStatus.COMPLETED, type = EncounterType.ACUPUNCTURE)
        )
        val observations = listOf(
            StructuredObservation(id = 1, tenantId = 1, encounterId = 1, patientId = 1, type = ObservationType.SYMPTOM, content = "Insônia")
        )
        val notes = listOf(
            ClinicalNote(id = 1, tenantId = 1, encounterId = 1, patientId = 1, format = NoteFormat.SOAP, subjective = "Insônia", status = NoteStatus.FINAL)
        )
        val plans = listOf(
            TreatmentPlan(id = 1, tenantId = 1, encounterId = 1, patientId = 1, goals = "Melhorar sono", status = TreatmentPlanStatus.CONFIRMED)
        )
        val followUps = listOf(
            FollowUp(id = 1, tenantId = 1, patientId = 1, reason = "Retorno em 2 semanas", status = FollowUpStatus.SCHEDULED)
        )

        val result = useCase.export(patient, encounters, observations, notes, plans, followUps)

        assertNotNull(result.bundle)
        assertEquals("Bundle", result.bundle.resourceType)
        assertEquals("collection", result.bundle.type)
        assertTrue(result.resourceCount >= 5) // Patient + Encounter + Observation + DocumentReference + CarePlan + Flag
    }

    @Test
    fun export_emptyData_createsBundleWithPatientOnly() {
        val patient = Patient(id = 1, name = "Maria Silva")
        val result = useCase.export(patient, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        assertEquals(1, result.resourceCount) // Only Patient
    }

    @Test
    fun export_patientWithoutDocument_warns() {
        val patient = Patient(id = 1, name = "Maria Silva", document = null)
        val result = useCase.export(patient, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        assertTrue(result.warnings.any { it.contains("document") })
    }

    @Test
    fun export_deterministic_sameInputSameOutput() {
        val patient = Patient(id = 1, name = "Maria Silva")
        val r1 = useCase.export(patient, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        val r2 = useCase.export(patient, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        assertEquals(r1.resourceCount, r2.resourceCount)
    }

    @Test
    fun export_bundleHasCorrectResourceTypes() {
        val patient = Patient(id = 1, name = "Maria Silva")
        val encounters = listOf(Encounter(id = 1, tenantId = 1, patientId = 1, status = EncounterStatus.COMPLETED, type = EncounterType.ACUPUNCTURE))
        val result = useCase.export(patient, encounters, emptyList(), emptyList(), emptyList(), emptyList())
        val resourceTypes = result.bundle.entry.map { it.resource.resourceType }
        assertTrue(resourceTypes.contains("Patient"))
        assertTrue(resourceTypes.contains("Encounter"))
    }
}
