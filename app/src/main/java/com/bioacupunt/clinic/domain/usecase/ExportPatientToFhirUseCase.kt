package com.bioacupunt.clinic.domain.usecase

import com.bioacupunt.clinic.domain.model.Encounter
import com.bioacupunt.clinic.domain.model.FhirBundle
import com.bioacupunt.clinic.domain.model.FhirEntry
import com.bioacupunt.clinic.domain.model.FhirExportResult
import com.bioacupunt.clinic.domain.model.FhirResource
import com.bioacupunt.clinic.domain.model.StructuredObservation
import com.bioacupunt.clinic.domain.model.ClinicalNote
import com.bioacupunt.clinic.domain.model.TreatmentPlan
import com.bioacupunt.clinic.domain.model.FollowUp
import com.bioacupunt.patient.domain.model.Patient

/**
 * Export patient data to FHIR Bundle.
 *
 * This is a MAPPING LAYER — internal models remain the source of truth.
 * The FHIR Bundle is for export/interoperability only.
 *
 * Does NOT transmit automatically. The user must initiate export.
 */
class ExportPatientToFhirUseCase {

    fun export(
        patient: Patient,
        encounters: List<Encounter>,
        observations: List<StructuredObservation>,
        notes: List<ClinicalNote>,
        treatmentPlans: List<TreatmentPlan>,
        followUps: List<FollowUp>,
    ): FhirExportResult {
        val entries = mutableListOf<FhirEntry>()
        val warnings = mutableListOf<String>()

        // Patient resource
        entries.add(FhirEntry(mapPatient(patient)))

        // Encounter resources
        for (encounter in encounters) {
            entries.add(FhirEntry(mapEncounter(encounter, patient)))
        }

        // Observation resources
        for (observation in observations) {
            entries.add(FhirEntry(mapObservation(observation)))
        }

        // Note resources (DocumentReference)
        for (note in notes) {
            entries.add(FhirEntry(mapNote(note)))
        }

        // CarePlan resources
        for (plan in treatmentPlans) {
            entries.add(FhirEntry(mapTreatmentPlan(plan)))
        }

        // Flag resources for follow-ups
        for (followUp in followUps) {
            entries.add(FhirEntry(mapFollowUp(followUp)))
        }

        if (patient.document.isNullOrBlank()) {
            warnings.add("Patient document (CPF) not available — required for FHIR Patient.identifier")
        }

        return FhirExportResult(
            bundle = FhirBundle(entry = entries),
            resourceCount = entries.size,
            warnings = warnings,
            exportedAt = java.time.Instant.now().toString(),
        )
    }

    private fun mapPatient(patient: Patient) = FhirResource(
        resourceType = "Patient",
        id = "patient-${patient.id}",
        fields = mapOf(
            "name" to listOf(mapOf("text" to patient.name)),
            "gender" to "unknown",
            "active" to (patient.status == "ACTIVE"),
        ),
    )

    private fun mapEncounter(encounter: Encounter, patient: Patient) = FhirResource(
        resourceType = "Encounter",
        id = "encounter-${encounter.id}",
        fields = mapOf(
            "status" to mapEncounterStatus(encounter.status.name),
            "class" to mapOf("code" to "AMB"),
            "type" to listOf(mapOf("text" to encounter.type.label)),
            "subject" to mapOf("reference" to "patient-${patient.id}"),
            "period" to mapOf(
                "start" to encounter.startedAt,
                "end" to encounter.endedAt,
            ),
        ),
    )

    private fun mapObservation(observation: StructuredObservation) = FhirResource(
        resourceType = "Observation",
        id = "observation-${observation.id}",
        fields = mapOf(
            "status" to mapObservationStatus(observation.status.name),
            "category" to listOf(mapOf("text" to observation.type.label)),
            "code" to mapOf("text" to observation.type.label),
            "valueString" to observation.content,
        ),
    )

    private fun mapNote(note: ClinicalNote) = FhirResource(
        resourceType = "DocumentReference",
        id = "note-${note.id}",
        fields = mapOf(
            "status" to mapNoteStatus(note.status.name),
            "type" to mapOf("text" to note.format.label),
            "content" to listOf(mapOf(
                "attachment" to mapOf(
                    "contentType" to "text/plain",
                    "data" to buildNoteContent(note),
                ),
            )),
        ),
    )

    private fun mapTreatmentPlan(plan: TreatmentPlan) = FhirResource(
        resourceType = "CarePlan",
        id = "plan-${plan.id}",
        fields = mapOf(
            "status" to mapPlanStatus(plan.status.name),
            "intent" to "plan",
            "goal" to listOf(mapOf("description" to plan.goals)),
            "activity" to plan.items.map { mapOf("detail" to mapOf("description" to it.description)) },
        ),
    )

    private fun mapFollowUp(followUp: FollowUp) = FhirResource(
        resourceType = "Flag",
        id = "followup-${followUp.id}",
        fields = mapOf(
            "status" to mapFollowUpStatus(followUp.status.name),
            "code" to mapOf("text" to "Follow-up"),
            "note" to listOf(mapOf("text" to followUp.reason)),
        ),
    )

    private fun buildNoteContent(note: ClinicalNote) = buildString {
        appendLine("=== ${note.format.label} ===")
        if (note.subjective.isNotBlank()) appendLine("S: ${note.subjective}")
        if (note.objective.isNotBlank()) appendLine("O: ${note.objective}")
        if (note.assessment.isNotBlank()) appendLine("A: ${note.assessment}")
        if (note.plan.isNotBlank()) appendLine("P: ${note.plan}")
    }

    private fun mapEncounterStatus(status: String) = when (status) {
        "PLANNED" -> "planned"
        "IN_PROGRESS" -> "in-progress"
        "PAUSED" -> "onhold"
        "COMPLETED" -> "finished"
        "CANCELLED" -> "cancelled"
        else -> "unknown"
    }

    private fun mapObservationStatus(status: String) = when (status) {
        "DRAFT" -> "registered"
        "REVIEWED" -> "preliminary"
        "CONFIRMED" -> "final"
        "REJECTED" -> "entered-in-error"
        else -> "unknown"
    }

    private fun mapNoteStatus(status: String) = when (status) {
        "DRAFT" -> "current"
        "REVIEWED" -> "current"
        "FINAL" -> "final"
        else -> "unknown"
    }

    private fun mapPlanStatus(status: String) = when (status) {
        "DRAFT" -> "draft"
        "CONFIRMED" -> "active"
        "IN_PROGRESS" -> "active"
        "COMPLETED" -> "completed"
        else -> "unknown"
    }

    private fun mapFollowUpStatus(status: String) = when (status) {
        "SCHEDULED" -> "active"
        "COMPLETED" -> "inactive"
        "MISSED" -> "inactive"
        "CANCELLED" -> "inactive"
        else -> "unknown"
    }
}
