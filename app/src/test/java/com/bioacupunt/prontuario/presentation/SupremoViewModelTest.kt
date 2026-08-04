package com.bioacupunt.prontuario.presentation

import com.bioacupunt.ai.core.AiRepository
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.ai.core.AiResult
import com.bioacupunt.prontuario.data.local.BucketCount
import com.bioacupunt.prontuario.data.local.MtcAssessmentDao
import com.bioacupunt.prontuario.data.local.MtcAssessmentEntity
import com.bioacupunt.prontuario.domain.usecase.MtcAssessmentRepository
import com.bioacupunt.prontuario.domain.usecase.StructureChiefComplaintUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
 *
 * Sob Robolectric (não JUnit puro) porque os testes da sugestão extrativa da IA
 * exercitam [StructureChiefComplaintUseCase], que usa `org.json.JSONObject` — só o
 * shadow do Robolectric devolve o comportamento real dessa classe.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SupremoViewModelTest {

    /** DAO fake em memória: captura a última entidade persistida. */
    private class FakeDao : MtcAssessmentDao {
        var lastSaved: MtcAssessmentEntity? = null
            private set
        var saveCount = 0
            private set

        /** `MtcAssessmentRepository.screen()` sempre lê `flagsHistory` — contar isto
         * conta quantas vezes a triagem de segurança de fato rodou contra o Room. */
        var screenCalls = 0
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
        override suspend fun flagsHistory(pid: Long): List<String> {
            screenCalls++
            return emptyList()
        }
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

    // -- Sugestão extrativa da IA sobre o Motivo da Consulta -------------------

    /** Espião: conta chamadas e devolve sempre o mesmo JSON de extração. */
    private class SpyAiRepository(private val json: String) : AiRepository {
        var generateCalls = 0
            private set

        override suspend fun generate(request: AiRequest): Result<AiResult> {
            generateCalls++
            return Result.success(AiResult(text = json, providerId = "fake", modelId = "fake"))
        }

        override suspend fun stream(request: AiRequest): Flow<String> = flowOf(json)
    }

    @Test
    fun acceptingASuggestionChip_writesViaTheSameToggleFunction_andPrunesItFromTheSuggestion() = runTest(dispatcher) {
        val ai = SpyAiRepository("""{"aggravating": [], "relieving": ["repouso"], "reviewOfSystems": []}""")
        val dao = FakeDao()
        val vm = SupremoViewModel(MtcAssessmentRepository(dao), patientId = 1L, StructureChiefComplaintUseCase(ai))
        advanceUntilIdle()

        vm.updateChiefComplaint("Dor lombar que melhora muito com repouso, texto longo o bastante.")
        advanceTimeBy(1300)
        advanceUntilIdle()

        assertEquals(listOf("repouso"), vm.state.value.chiefComplaintSuggestion?.relieving)

        vm.acceptRelievingSuggestion("repouso")
        advanceUntilIdle()

        assertTrue("Aceitar deve gravar no MESMO campo que um toque manual usaria", "repouso" in vm.state.value.draft.relievingFactors)
        assertNull("O item aceito deve sumir da sugestão pendente", vm.state.value.chiefComplaintSuggestion)
    }

    @Test
    fun chiefComplaintDebounce_settlesToASingleModelCall_notOnePerKeystroke() = runTest(dispatcher) {
        val ai = SpyAiRepository("""{"aggravating": [], "relieving": [], "reviewOfSystems": []}""")
        val dao = FakeDao()
        val vm = SupremoViewModel(MtcAssessmentRepository(dao), patientId = 1L, StructureChiefComplaintUseCase(ai))
        advanceUntilIdle()

        vm.updateChiefComplaint("Dor lombar que")
        advanceTimeBy(400)
        vm.updateChiefComplaint("Dor lombar que piora")
        advanceTimeBy(400)
        vm.updateChiefComplaint("Dor lombar que piora com frio, texto grande o bastante.")
        advanceTimeBy(1300)
        advanceUntilIdle()

        assertEquals("Três digitações em sequência rápida devem virar UMA chamada, não três", 1, ai.generateCalls)
    }

    // -- Debounce da re-triagem (rescreen) --------------------------------

    /**
     * Antes, `edit{}`/`updateProposal` disparavam `rescreenAsync()` sem debounce — cada
     * tecla digitada em Motivo da Consulta/Plano/notas virava uma query no Room. Isto
     * trava a regressão: uma rajada de edições rápidas deve settar numa única execução
     * da triagem, não uma por edição.
     */
    @Test
    fun rapidEdits_settleToASingleScreenRun_notOnePerKeystroke() = runTest(dispatcher) {
        val dao = FakeDao()
        val vm = SupremoViewModel(MtcAssessmentRepository(dao), patientId = 1L)
        advanceUntilIdle() // init roda rescreen() uma vez — baseline
        val callsAfterInit = dao.screenCalls

        vm.updateChiefComplaint("D")
        advanceTimeBy(100)
        vm.updateChiefComplaint("Do")
        advanceTimeBy(100)
        vm.updateChiefComplaint("Dor lombar")
        advanceTimeBy(500)
        advanceUntilIdle()

        assertEquals(
            "Três edições em sequência rápida devem virar UMA re-triagem, não três",
            callsAfterInit + 1,
            dao.screenCalls,
        )
    }

    /** A triagem continua rodando de verdade após a médica parar de digitar — o
     * debounce adia a query, nunca a cancela pra sempre. */
    @Test
    fun editThenPause_stillRunsTheScreenEventually() = runTest(dispatcher) {
        val dao = FakeDao()
        val vm = SupremoViewModel(MtcAssessmentRepository(dao), patientId = 1L)
        advanceUntilIdle()
        val callsAfterInit = dao.screenCalls

        vm.updateChiefComplaint("Dor lombar há duas semanas")
        advanceUntilIdle()

        assertTrue("A edição precisa disparar a triagem depois do debounce assentar", dao.screenCalls > callsAfterInit)
    }

    @Test
    fun toggleTechnique_alsoDebouncesTheScreenRun() = runTest(dispatcher) {
        val dao = FakeDao()
        val vm = SupremoViewModel(MtcAssessmentRepository(dao), patientId = 1L)
        advanceUntilIdle()
        val callsAfterInit = dao.screenCalls

        vm.toggleTechnique(com.bioacupunt.prontuario.domain.safety.Technique.NEEDLING)
        advanceTimeBy(100)
        vm.toggleTechnique(com.bioacupunt.prontuario.domain.safety.Technique.MOXIBUSTION)
        advanceUntilIdle()

        assertEquals(
            "Duas mudanças de proposta em sequência rápida também devem settar numa re-triagem só",
            callsAfterInit + 1,
            dao.screenCalls,
        )
    }

    @Test
    fun shortChiefComplaint_neverTriggersTheModel() = runTest(dispatcher) {
        val ai = SpyAiRepository("""{"aggravating": [], "relieving": [], "reviewOfSystems": []}""")
        val dao = FakeDao()
        val vm = SupremoViewModel(MtcAssessmentRepository(dao), patientId = 1L, StructureChiefComplaintUseCase(ai))
        advanceUntilIdle()

        vm.updateChiefComplaint("dor")
        advanceTimeBy(1300)
        advanceUntilIdle()

        assertEquals(0, ai.generateCalls)
        assertNull(vm.state.value.chiefComplaintSuggestion)
    }
}
