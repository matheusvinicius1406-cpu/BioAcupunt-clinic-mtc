package com.bioacupunt.clinic.domain.nlp

import com.bioacupunt.clinic.domain.model.ObservationSource
import com.bioacupunt.clinic.domain.model.ObservationStatus
import com.bioacupunt.clinic.domain.model.ObservationType
import com.bioacupunt.clinic.domain.model.Questionnaire
import com.bioacupunt.clinic.domain.model.QuestionnaireResponse
import com.bioacupunt.clinic.domain.model.StructuredObservation

/**
 * Maps QuestionnaireResponse answers to StructuredObservations.
 *
 * Only questions with an explicit observationMapping produce observations.
 * Questions without mapping are stored as-is but don't generate clinical data.
 *
 * All mapped observations start as AI_EXTRACTED_DRAFT status — they must be
 * reviewed and confirmed by the professional.
 */
class QuestionnaireToObservationMapper {

    /**
     * Map a questionnaire response to structured observations.
     * Only items with an explicit observationMapping produce observations.
     */
    fun map(
        questionnaire: Questionnaire,
        response: QuestionnaireResponse,
        tenantId: Long,
        patientId: Long,
        encounterId: Long,
    ): List<StructuredObservation> {
        val observations = mutableListOf<StructuredObservation>()

        for (section in questionnaire.sections) {
            for (item in section.items) {
                val answer = response.answers[item.id] ?: continue

                // Only map items that have an explicit observationMapping
                val observationType = item.observationMapping ?: continue

                // Skip empty answers for required fields
                if (answer.isBlank() && item.required) continue

                observations.add(StructuredObservation(
                    id = 0,
                    tenantId = tenantId,
                    encounterId = encounterId,
                    patientId = patientId,
                    type = observationType,
                    content = formatAnswer(item.label, answer, item.options),
                    structuredData = mapOf(
                        "questionnaireId" to questionnaire.id,
                        "questionnaireVersion" to questionnaire.version.toString(),
                        "itemId" to item.id,
                        "itemLabel" to item.label,
                    ),
                    status = ObservationStatus.DRAFT,
                    source = ObservationSource.AI_EXTRACTED_DRAFT,
                    createdAt = response.createdAt,
                ))
            }
        }

        return observations
    }

    private fun formatAnswer(itemLabel: String, answer: String, options: List<com.bioacupunt.clinic.domain.model.QuestionnaireOption>): String {
        // If the answer corresponds to an option ID, show the option label
        val matchedOption = options.firstOrNull { it.id == answer || it.value == answer }
        return if (matchedOption != null) {
            "$itemLabel: ${matchedOption.label}"
        } else {
            "$itemLabel: $answer"
        }
    }
}
