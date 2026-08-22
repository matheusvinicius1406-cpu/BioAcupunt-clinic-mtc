package com.bioacupunt.clinic.domain.usecase

import com.bioacupunt.clinic.domain.model.MultimodalContext
import com.bioacupunt.clinic.domain.model.ObservationFactStatus
import com.bioacupunt.clinic.domain.model.PulseObservation
import com.bioacupunt.clinic.domain.model.TongueObservation
import com.bioacupunt.clinic.domain.repository.ObservationRepository
import com.bioacupunt.clinic.domain.repository.PulseObservationRepository
import com.bioacupunt.clinic.domain.repository.TongueObservationRepository
import java.time.Instant

/**
 * Builds a MultimodalContext for a patient at a specific encounter.
 *
 * Only CONFIRMED observations are treated as clinical facts.
 * AI_DETECTED drafts are included separately and clearly labeled.
 * This context feeds into Clinical Intelligence (Phase 3) — it does NOT
 * replace it.
 *
 * Architecture:
 * - Aggregates data from multiple observation repositories
 * - Filters by status (CONFIRMED vs DRAFT vs AI_DETECTED)
 * - Builds longitudinal comparison (previous encounter)
 * - Produces a structured context for Copilot and Clinical Intelligence
 */
class BuildMultimodalContextUseCase(
    private val tongueRepository: TongueObservationRepository,
    private val pulseRepository: PulseObservationRepository,
    private val observationRepository: ObservationRepository,
) {

    /**
     * Build multimodal context for a patient at a specific encounter.
     *
     * @param patientId Patient ID
     * @param encounterId Current encounter ID (null for latest)
     * @return MultimodalContext with confirmed facts and AI drafts separated
     */
    suspend fun execute(
        patientId: Long,
        encounterId: Long? = null,
    ): Result<MultimodalContext> = runCatching {
        // 1. Get confirmed tongue observations
        val confirmedTongue = tongueRepository.getByPatient(patientId)
            .getOrDefault(emptyList())
            .filter { it.status.name == "CONFIRMED" }

        // 2. Get confirmed pulse observations
        val confirmedPulse = pulseRepository.getByPatient(patientId)
            .getOrDefault(emptyList())
            .filter { it.status.name == "CONFIRMED" }

        // 3. Get confirmed clinical observations (StructuredObservation)
        val allObservations = observationRepository.getByPatientId(patientId)
        val confirmedClinical = allObservations.filter {
            it.status == com.bioacupunt.clinic.domain.model.ObservationStatus.CONFIRMED
        }

        // 4. Get AI drafts (not yet confirmed)
        val aiDrafts = buildAiDrafts(
            tongueRepository.getByPatient(patientId).getOrDefault(emptyList())
                .filter { it.status == com.bioacupunt.clinic.domain.model.TongueObservationStatus.DRAFT },
            pulseRepository.getByPatient(patientId).getOrDefault(emptyList())
                .filter { it.status == com.bioacupunt.clinic.domain.model.PulseObservationStatus.DRAFT },
        )

        // 5. Get previous encounter data for comparison
        val previousTongue = tongueRepository.getLatestConfirmed(patientId).getOrNull()
        val previousPulse = pulseRepository.getLatestConfirmed(patientId).getOrNull()

        MultimodalContext(
            patientId = patientId,
            encounterId = encounterId,
            confirmedTongueFindings = confirmedTongue,
            confirmedPulseFindings = confirmedPulse,
            confirmedClinicalObservations = confirmedClinical,
            aiDraftFindings = aiDrafts,
            previousEncounterTongue = previousTongue,
            previousEncounterPulse = previousPulse,
            generatedAt = Instant.now().toString(),
        )
    }

    private fun buildAiDrafts(
        draftTongue: List<TongueObservation>,
        draftPulse: List<PulseObservation>,
    ): List<com.bioacupunt.clinic.domain.model.MultimodalObservation> {
        val drafts = mutableListOf<com.bioacupunt.clinic.domain.model.MultimodalObservation>()

        draftTongue.forEach { obs ->
            drafts.add(
                com.bioacupunt.clinic.domain.model.MultimodalObservation(
                    sourceType = "TONGUE",
                    status = ObservationFactStatus.AI_DETECTED,
                    summary = buildTongueSummary(obs),
                    details = buildTongueDetails(obs),
                    mediaId = obs.mediaId,
                    confidence = obs.visionConfidence,
                    modelVersion = obs.visionModelVersion,
                )
            )
        }

        draftPulse.forEach { obs ->
            drafts.add(
                com.bioacupunt.clinic.domain.model.MultimodalObservation(
                    sourceType = "PULSE",
                    status = ObservationFactStatus.AI_DETECTED,
                    summary = buildPulseSummary(obs),
                    details = buildPulseDetails(obs),
                    confidence = null,
                )
            )
        }

        return drafts
    }

    private fun buildTongueSummary(obs: TongueObservation): String = buildString {
        obs.bodyColor?.let { append("${it.label}") }
        obs.shape?.let { append(" / ${it.label}") }
        obs.coating?.let { append(" / ${it.label}") }
        obs.moisture?.let { append(" / ${it.label}") }
    }.ifEmpty { "Observação de língua" }

    private fun buildTongueDetails(obs: TongueObservation): Map<String, String> = buildMap {
        obs.bodyColor?.let { put("cor", it.label) }
        obs.shape?.let { put("forma", it.label) }
        obs.coating?.let { put("revestimento", it.label) }
        obs.moisture?.let { put("umidade", it.label) }
        if (obs.cracks.isNotEmpty()) put("fissuras", obs.cracks)
        if (obs.marks.isNotEmpty()) put("marcas", obs.marks)
    }

    private fun buildPulseSummary(obs: PulseObservation): String = buildString {
        obs.rate?.let { append("${it}bpm") }
        if (obs.depth.isNotEmpty()) append(" / ${obs.depth}")
        if (obs.strength.isNotEmpty()) append(" / ${obs.strength}")
    }.ifEmpty { "Observação de pulso" }

    private fun buildPulseDetails(obs: PulseObservation): Map<String, String> = buildMap {
        obs.rate?.let { put("frequencia", "$it bpm") }
        if (obs.depth.isNotEmpty()) put("profundidade", obs.depth)
        if (obs.strength.isNotEmpty()) put("forca", obs.strength)
        if (obs.leftCun.isNotEmpty()) put("cun_esquerdo", obs.leftCun)
        if (obs.rightCun.isNotEmpty()) put("cun_direito", obs.rightCun)
    }
}
