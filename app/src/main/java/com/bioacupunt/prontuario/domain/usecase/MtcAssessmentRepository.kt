package com.bioacupunt.prontuario.domain.usecase

import com.bioacupunt.prontuario.data.local.MtcAssessmentDao
import com.bioacupunt.prontuario.data.local.decodeFlags
import com.bioacupunt.prontuario.data.local.toDomain
import com.bioacupunt.prontuario.data.local.toEntity
import com.bioacupunt.prontuario.domain.model.ClinicalFlag
import com.bioacupunt.prontuario.domain.model.MtcAssessment
import com.bioacupunt.prontuario.domain.safety.ClinicalSafetyEngine
import com.bioacupunt.prontuario.domain.safety.SafetyVerdict
import com.bioacupunt.prontuario.domain.safety.TreatmentProposal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Data access + clinical screening for the structured TCM chart.
 */
class MtcAssessmentRepository(
    private val dao: MtcAssessmentDao,
    private val safetyEngine: ClinicalSafetyEngine = ClinicalSafetyEngine(),
) {

    fun observeHistory(patientId: Long): Flow<List<MtcAssessment>> =
        dao.observeForPatient(patientId).map { rows -> rows.map { it.toDomain() } }

    fun observeLatest(patientId: Long): Flow<MtcAssessment?> =
        dao.observeLatest(patientId).map { it?.toDomain() }

    suspend fun save(assessment: MtcAssessment): Long = dao.save(assessment.toEntity())

    suspend fun delete(id: Long) = dao.delete(id)

    /**
     * The patient's **standing** risk profile: every clinical flag ever recorded,
     * across every session, unioned together.
     *
     * This is deliberately not "the flags on today's form". A pacemaker recorded in
     * March is still a pacemaker in July, whether or not the practitioner remembered
     * to re-tick the box while running late between patients. Forgetting is precisely
     * the failure this software exists to catch, so the safety screen must not depend
     * on the practitioner not forgetting.
     *
     * Flags are additive here. Removing one is an explicit clinical act
     * (a resolved condition) and belongs in a dedicated review flow, not in the
     * silent absence of a checkbox.
     */
    suspend fun standingFlags(patientId: Long): Set<ClinicalFlag> =
        dao.flagsHistory(patientId)
            .flatMap { decodeFlags(it) }
            .toSet()

    /**
     * Screens a proposed protocol against the patient's standing risk profile,
     * merged with whatever the practitioner has entered in the current draft.
     *
     * Nothing generative is in this path. See [ClinicalSafetyEngine].
     */
    suspend fun screen(
        patientId: Long,
        proposal: TreatmentProposal,
        draft: MtcAssessment? = null,
    ): SafetyVerdict {
        val standing = standingFlags(patientId)
        val merged = (draft ?: MtcAssessment(patientId = patientId)).let { base ->
            base.copy(
                flags = base.flags + standing,
                gestationalWeeks = base.gestationalWeeks
                    ?: dao.latestGestationalWeeks(patientId),
            )
        }
        return safetyEngine.evaluate(proposal, merged)
    }
}
