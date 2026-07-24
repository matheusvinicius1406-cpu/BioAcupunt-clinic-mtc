package com.bioacupunt.prontuario.domain.usecase

import com.bioacupunt.prontuario.data.local.BucketCount
import com.bioacupunt.prontuario.data.local.MtcAssessmentDao
import com.bioacupunt.prontuario.data.local.MtcAssessmentEntity
import com.bioacupunt.prontuario.data.local.encodeFlags
import com.bioacupunt.prontuario.domain.model.ClinicalFlag
import com.bioacupunt.prontuario.domain.model.MtcAssessment
import com.bioacupunt.prontuario.domain.safety.Technique
import com.bioacupunt.prontuario.domain.safety.TreatmentProposal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the **standing risk profile** invariant (CLAUDE.md → "Perfil de risco
 * permanente"): [MtcAssessmentRepository.standingFlags] unions **every** clinical flag
 * ever recorded across **every** session. A pacemaker recorded in March must still veto
 * electroacupuncture in July even if the practitioner, running late between patients,
 * forgets to re-tick the box on the July form. Forgetting is exactly the failure this
 * software exists to catch, so the safety screen must not depend on nobody forgetting.
 *
 * The DAO is faked (London-school: we verify the collaboration/contract, not Room), so
 * these run as fast, deterministic JVM unit tests with no Android/Room on the path.
 */
class MtcAssessmentRepositoryStandingFlagsTest {

    /**
     * Minimal in-memory fake of [MtcAssessmentDao]. Only the two methods the safety
     * path actually reads are meaningful; the rest return inert defaults.
     *
     * [flagRowsByPatient] models the `flagsCsv` column of past sessions — one CSV string
     * per session, exactly as the real `flagsHistory` query returns (rows with empty
     * flagsCsv are excluded by SQL, so we never add empty strings here).
     */
    private class FakeDao(
        private val flagRowsByPatient: Map<Long, List<String>> = emptyMap(),
        private val gestationalWeeksByPatient: Map<Long, Int?> = emptyMap(),
    ) : MtcAssessmentDao {
        override fun observeForPatient(pid: Long): Flow<List<MtcAssessmentEntity>> = flowOf(emptyList())
        override fun observeLatest(pid: Long): Flow<MtcAssessmentEntity?> = emptyFlow()
        override suspend fun getById(id: Long): MtcAssessmentEntity? = null
        override suspend fun save(entity: MtcAssessmentEntity): Long = 1L
        override suspend fun delete(id: Long) = Unit
        override suspend fun flagsHistory(pid: Long): List<String> = flagRowsByPatient[pid] ?: emptyList()
        override suspend fun latestGestationalWeeks(pid: Long): Int? = gestationalWeeksByPatient[pid]
        override suspend fun count(pid: Long): Int = 0
        override suspend fun temperatureDistribution(): List<BucketCount> = emptyList()
        override suspend fun pendingSync(): List<MtcAssessmentEntity> = emptyList()
    }

    private val patientId = 42L

    @Test
    fun standingFlags_unionsEveryFlagAcrossEverySession() = runTest {
        // March session recorded a pacemaker; July session recorded pregnancy. Neither
        // row mentions the other's flag — exactly the real-world case.
        val dao = FakeDao(
            flagRowsByPatient = mapOf(
                patientId to listOf(
                    encodeFlags(setOf(ClinicalFlag.PACEMAKER)),                 // March
                    encodeFlags(setOf(ClinicalFlag.PREGNANCY, ClinicalFlag.DIABETES)), // July
                ),
            ),
        )
        val repo = MtcAssessmentRepository(dao)

        val standing = repo.standingFlags(patientId)

        assertTrue("Pacemaker de março persiste", ClinicalFlag.PACEMAKER in standing)
        assertTrue("Gestação de julho presente", ClinicalFlag.PREGNANCY in standing)
        assertTrue("Diabetes de julho presente", ClinicalFlag.DIABETES in standing)
    }

    @Test
    fun screen_vetoesFromAPriorSession_evenWhenTodaysDraftOmitsTheFlag() = runTest {
        // The invariant, end-to-end through the real ClinicalSafetyEngine: the pacemaker
        // lives only in a PAST session's flagsCsv. Today's draft has NO flags at all —
        // the practitioner forgot to re-tick it — yet an electroacupuncture proposal must
        // still be blocked.
        val dao = FakeDao(
            flagRowsByPatient = mapOf(
                patientId to listOf(encodeFlags(setOf(ClinicalFlag.PACEMAKER))), // months ago
            ),
        )
        val repo = MtcAssessmentRepository(dao)

        val todaysDraft = MtcAssessment(patientId = patientId, flags = emptySet())
        val verdict = repo.screen(
            patientId = patientId,
            proposal = TreatmentProposal(techniques = setOf(Technique.ELECTROACUPUNCTURE)),
            draft = todaysDraft,
        )

        assertTrue(
            "Marca-passo de sessão anterior deve vetar eletroacupuntura hoje",
            verdict.isBlocked,
        )
        assertTrue(verdict.blocking.any { it.flag == ClinicalFlag.PACEMAKER })
    }

    @Test
    fun screen_isClearWhenNoFlagWasEverRecorded() = runTest {
        // Counter-case, guarding against a screen that blocks unconditionally: a patient
        // with a clean history and a clean draft yields a clear verdict.
        val repo = MtcAssessmentRepository(FakeDao())

        val verdict = repo.screen(
            patientId = patientId,
            proposal = TreatmentProposal(techniques = setOf(Technique.ELECTROACUPUNCTURE)),
            draft = MtcAssessment(patientId = patientId),
        )

        assertFalse("Sem qualquer flag histórica, nada deve vetar", verdict.isBlocked)
    }
}
