package com.bioacupunt.prontuario.presentation

import com.bioacupunt.prontuario.data.local.BucketCount
import com.bioacupunt.prontuario.data.local.MtcAssessmentDao
import com.bioacupunt.prontuario.data.local.MtcAssessmentEntity
import com.bioacupunt.prontuario.domain.usecase.MtcAssessmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Regressão do bug do override que era no-op.
 *
 * O CLAUDE.md exige: prosseguir sobre um veto FORBIDDEN é um EVENTO DE AUDITORIA
 * (LGPD/CFM) — precisa gravar razão (≥10 caracteres), quem prosseguiu e quando, e
 * precisa persistir na hora (não apenas no draft em memória, que se perde se a
 * médica sair do prontuário). Antes, o callback era no-op: nada era gravado.
 *
 * Estes testes fiam [SupremoViewModel.overrideVeto] no repositório real
 * ([MtcAssessmentRepository] + [com.bioacupunt.prontuario.domain.safety.ClinicalSafetyEngine]
 * reais) através de um DAO fake, e provam que o registro chega ao `save`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SupremoViewModelTest {

    /** DAO fake em memória: captura a última entidade persistida. */
    private class FakeDao : MtcAssessmentDao {
        var lastSaved: MtcAssessmentEntity? = null
            private set
        var saveCount = 0
            private set

        override fun observeForPatient(pid: Long): Flow<List<MtcAssessmentEntity>> = flowOf(emptyList())
        override fun observeLatest(pid: Long): Flow<MtcAssessmentEntity?> = flowOf(null)
        override suspend fun getById(id: Long): MtcAssessmentEntity? = lastSaved
        override suspend fun save(entity: MtcAssessmentEntity): Long {
            lastSaved = entity
            saveCount++
            return 1L
        }
        override suspend fun delete(id: Long) {}
        override suspend fun flagsHistory(pid: Long): List<String> = emptyList()
        override suspend fun latestGestationalWeeks(pid: Long): Int? = null
        override suspend fun count(pid: Long): Int = 0
        override suspend fun temperatureDistribution(): List<BucketCount> = emptyList()
        override suspend fun pendingSync(): List<MtcAssessmentEntity> = emptyList()
    }

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun overrideVeto_persistsReasonUserAndTimestamp() = runTest(dispatcher) {
        val dao = FakeDao()
        val vm = SupremoViewModel(MtcAssessmentRepository(dao), patientId = 42L)
        advanceUntilIdle() // deixa o init (standingFlags + rescreen) terminar

        vm.overrideVeto("Paciente ciente do risco e assume a decisão clínica", userId = "medica-7")
        advanceUntilIdle()

        val saved = dao.lastSaved
        assertTrue("Um override deve persistir na hora (não ficar só no draft)", dao.saveCount >= 1)
        assertEquals("Paciente ciente do risco e assume a decisão clínica", saved!!.overrideReason)
        assertEquals("medica-7", saved.overrideBy)
        assertTrue("O horário do override deve ser gravado (ISO-8601)", saved.overrideAt.isNotBlank())
        // Deve ser um instante parseável, não uma string qualquer.
        java.time.Instant.parse(saved.overrideAt)
    }

    @Test
    fun overrideVeto_trimsReasonBeforePersisting() = runTest(dispatcher) {
        val dao = FakeDao()
        val vm = SupremoViewModel(MtcAssessmentRepository(dao), patientId = 1L)
        advanceUntilIdle()

        vm.overrideVeto("   justificativa clínica com espaços   ", userId = "u1")
        advanceUntilIdle()

        assertEquals("justificativa clínica com espaços", dao.lastSaved!!.overrideReason)
    }

    @Test
    fun overrideVeto_belowTenChars_isRejected_andPersistsNothing() = runTest(dispatcher) {
        val dao = FakeDao()
        val vm = SupremoViewModel(MtcAssessmentRepository(dao), patientId = 1L)
        advanceUntilIdle()

        vm.overrideVeto("  curto  ", userId = "u1") // "curto" = 5 chars após trim
        advanceUntilIdle()

        assertNull("Justificativa < 10 caracteres não pode gravar nada", dao.lastSaved)
        assertEquals("Override inválido não pode disparar save", 0, dao.saveCount)
        assertEquals("", vm.state.value.draft.overrideReason)
    }
}
