package com.bioacupunt.clinic.domain.usecase

import com.bioacupunt.clinic.domain.model.ClinicalNoteDraft
import com.bioacupunt.clinic.domain.model.Encounter
import com.bioacupunt.clinic.domain.model.NoteFormat
import com.bioacupunt.clinic.domain.model.StructuredObservation
import com.bioacupunt.prontuario.domain.model.MtcAssessment

/**
 * Generate a clinical note draft from encounter data.
 *
 * AI can generate DRAFT notes, but NEVER auto-finalize.
 * The professional must REVIEW → EDIT → CONFIRM → FINAL.
 *
 * This use case assembles structured data into a note format.
 * The LLM may later expand this into natural language, but the
 * structure exists before any LLM call.
 */
class GenerateClinicalDraftUseCase {

    fun generate(
        encounter: Encounter,
        observations: List<StructuredObservation>,
        assessment: MtcAssessment?,
        format: NoteFormat = NoteFormat.SOAP,
    ): ClinicalNoteDraft {
        val subjective = buildSubjective(observations)
        val objective = buildObjective(observations, assessment)
        val assessmentText = buildAssessment(assessment)
        val planText = buildPlan(assessment)

        return ClinicalNoteDraft(
            encounterId = encounter.id,
            patientId = encounter.patientId,
            format = format,
            subjective = subjective,
            objective = objective,
            assessment = assessmentText,
            plan = planText,
            mtcAssessmentSummary = assessmentText,
            evidenceSources = emptyList(),
            warnings = generateWarnings(observations, assessment),
            confidence = calculateConfidence(observations, assessment),
            generatedAt = java.time.Instant.now().toString(),
        )
    }

    private fun buildSubjective(observations: List<StructuredObservation>): String {
        val patientReported = observations.filter {
            it.source == com.bioacupunt.clinic.domain.model.ObservationSource.PATIENT_REPORTED
        }
        if (patientReported.isEmpty()) return "Sem dados subjetivos registrados."

        return patientReported.joinToString("\n") { "- ${it.content}" }
    }

    private fun buildObjective(
        observations: List<StructuredObservation>,
        assessment: MtcAssessment?,
    ): String {
        val parts = mutableListOf<String>()

        val practitionerObs = observations.filter {
            it.source == com.bioacupunt.clinic.domain.model.ObservationSource.PRACTITIONER_OBSERVED
        }
        if (practitionerObs.isNotEmpty()) {
            parts.add("Observações clínicas:")
            parts.addAll(practitionerObs.map { "  - ${it.content}" })
        }

        if (assessment != null) {
            if (assessment.tongue.bodyColor != com.bioacupunt.prontuario.domain.model.TongueBodyColor.UNSET) {
                parts.add("Língua: ${assessment.tongue.bodyColor.label}")
            }
            if (assessment.pulse.readings.isNotEmpty()) {
                parts.add("Pulso: ${assessment.pulse.readings.size} leituras")
            }
            if (assessment.baGang.isComplete) {
                parts.add("Ba Gang: ${assessment.baGang.polarity.name}/${assessment.baGang.depth.name}/${assessment.baGang.temperature.name}/${assessment.baGang.strength.name}")
            }
        }

        return parts.joinToString("\n").ifBlank { "Sem dados objetivos registrados." }
    }

    private fun buildAssessment(assessment: MtcAssessment?): String {
        if (assessment == null) return "Avaliação pendente."

        val parts = mutableListOf<String>()
        if (assessment.chiefComplaint.isNotBlank()) {
            parts.add("Queixa: ${assessment.chiefComplaint}")
        }
        if (assessment.clinicalImpression.isNotBlank()) {
            parts.add("Impressão clínica: ${assessment.clinicalImpression}")
        }
        if (assessment.patterns.isNotEmpty()) {
            parts.add("Padrões: ${assessment.patterns.joinToString { "${it.organ.label}: ${it.factors.joinToString { f -> f.label }}" }}")
        }

        return parts.joinToString("\n").ifBlank { "Avaliação pendente." }
    }

    private fun buildPlan(assessment: MtcAssessment?): String {
        if (assessment == null) return "Plano pendente."

        val parts = mutableListOf<String>()
        if (assessment.orientations.isNotBlank()) {
            parts.add("Orientações: ${assessment.orientations}")
        }
        if (assessment.flags.isNotEmpty()) {
            parts.add("Restrições: ${assessment.flags.joinToString { it.label }}")
        }

        return parts.joinToString("\n").ifBlank { "Plano pendente." }
    }

    private fun generateWarnings(
        observations: List<StructuredObservation>,
        assessment: MtcAssessment?,
    ): List<String> {
        val warnings = mutableListOf<String>()
        if (observations.isEmpty()) {
            warnings.add("Nenhuma observação estruturada registrada.")
        }
        if (assessment == null) {
            warnings.add("Avaliação MTC não disponível.")
        }
        if (assessment?.clinicalImpression.isNullOrBlank()) {
            warnings.add("Impressão clínica não registrada.")
        }
        return warnings
    }

    private fun calculateConfidence(
        observations: List<StructuredObservation>,
        assessment: MtcAssessment?,
    ): String {
        var score = 0
        if (observations.isNotEmpty()) score++
        if (assessment != null) score++
        if (assessment?.clinicalImpression?.isNotBlank() == true) score++
        if (assessment?.patterns?.isNotEmpty() == true) score++

        return when {
            score >= 3 -> "MODERATE"
            score >= 2 -> "LOW"
            else -> "INSUFFICIENT"
        }
    }
}
