package com.bioacupunt.clinic.domain.model

/**
 * Filter for which observation statuses are included in multimodal context.
 * Only CONFIRMED observations are treated as clinical facts.
 * DRAFT, AI_DETECTED, and REJECTED are never automatically facts.
 */
enum class ObservationFactStatus {
    OBSERVED,
    AI_DETECTED,
    REVIEWED,
    CONFIRMED,
}

/**
 * A confirmed multimodal observation from any source (tongue, pulse, clinical).
 * Used to build the multimodal context for Copilot and Clinical Intelligence.
 */
data class MultimodalObservation(
    val sourceType: String,
    val status: ObservationFactStatus,
    val summary: String,
    val details: Map<String, String> = emptyMap(),
    val mediaId: Long? = null,
    val confidence: Double? = null,
    val reviewedBy: String? = null,
    val confirmedBy: String? = null,
    val confirmedAt: String? = null,
    val modelVersion: String? = null,
)

/**
 * Complete multimodal context for a patient at a point in time.
 *
 * Only CONFIRMED observations appear in `confirmedFindings`.
 * AI_DETECTED items appear separately in `aiDraftFindings` and are
 * clearly labeled as such — never mixed with confirmed facts.
 */
data class MultimodalContext(
    val patientId: Long,
    val encounterId: Long? = null,

    // --- Confirmed findings (clinical facts) ---
    val confirmedTongueFindings: List<TongueObservation> = emptyList(),
    val confirmedPulseFindings: List<PulseObservation> = emptyList(),
    val confirmedClinicalObservations: List<StructuredObservation> = emptyList(),

    // --- AI drafts (NOT clinical facts) ---
    val aiDraftFindings: List<MultimodalObservation> = emptyList(),

    // --- Longitudinal comparison support ---
    val previousEncounterTongue: TongueObservation? = null,
    val previousEncounterPulse: PulseObservation? = null,

    // --- Provenance ---
    val generatedAt: String = "",
) {
    val hasConfirmedFindings: Boolean
        get() = confirmedTongueFindings.isNotEmpty() ||
                confirmedPulseFindings.isNotEmpty() ||
                confirmedClinicalObservations.isNotEmpty()

    val hasAiDrafts: Boolean
        get() = aiDraftFindings.isNotEmpty()

    val hasComparisonData: Boolean
        get() = previousEncounterTongue != null || previousEncounterPulse != null

    /**
     * Build a summary suitable for the Copilot context.
     * Only confirmed observations are included as facts.
     */
    fun buildCopilotContext(): String = buildString {
        if (confirmedTongueFindings.isNotEmpty()) {
            appendLine("Achados confirmados de língua:")
            confirmedTongueFindings.forEach { obs ->
                obs.bodyColor?.let { appendLine("  - Cor: ${it.label}") }
                obs.shape?.let { appendLine("  - Forma: ${it.label}") }
                obs.coating?.let { appendLine("  - Língua: ${it.label}") }
                obs.moisture?.let { appendLine("  - Umidade: ${it.label}") }
                if (obs.cracks.isNotEmpty()) appendLine("  - Fissuras: ${obs.cracks}")
                if (obs.marks.isNotEmpty()) appendLine("  - Marcas: ${obs.marks}")
            }
        }

        if (confirmedPulseFindings.isNotEmpty()) {
            appendLine("Achados confirmados de pulso:")
            confirmedPulseFindings.forEach { obs ->
                if (obs.rate != null) appendLine("  - Frequência: ${obs.rate} bpm")
                if (obs.depth.isNotEmpty()) appendLine("  - Profundidade: ${obs.depth}")
                if (obs.strength.isNotEmpty()) appendLine("  - Força: ${obs.strength}")
                if (obs.leftCun.isNotEmpty()) appendLine("  - Cun esquerdo: ${obs.leftCun}")
                if (obs.rightCun.isNotEmpty()) appendLine("  - Cun direito: ${obs.rightCun}")
            }
        }

        if (confirmedClinicalObservations.isNotEmpty()) {
            appendLine("Observações clínicas confirmadas:")
            confirmedClinicalObservations.forEach { obs ->
                appendLine("  - ${obs.type.label}: ${obs.content}")
            }
        }

        if (aiDraftFindings.isNotEmpty()) {
            appendLine("Sugestões de IA (não confirmadas):")
            aiDraftFindings.forEach { obs ->
                appendLine("  - [${obs.sourceType}] ${obs.summary}")
            }
        }
    }
}
