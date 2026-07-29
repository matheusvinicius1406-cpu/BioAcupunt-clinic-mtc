package com.bioacupunt.ai.presentation

import com.bioacupunt.ai.core.AiRepository
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.ai.core.AiResult
import com.bioacupunt.ai.domain.usecase.GenerateAiResponseUseCase
import com.bioacupunt.biblioteca.domain.ingestion.Provenance
import com.bioacupunt.biblioteca.domain.search.ArticleSearchBackend
import com.bioacupunt.biblioteca.domain.search.MtcRetriever
import com.bioacupunt.biblioteca.domain.search.MtcSearchEngine
import com.bioacupunt.biblioteca.domain.search.RetrievedArticle
import com.bioacupunt.biblioteca.domain.usecase.AskLibraryUseCase
import com.bioacupunt.core.util.Result as AppResult
import com.bioacupunt.crm.domain.model.CrmPatient
import com.bioacupunt.crm.domain.repository.CrmPatientRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Achado da auditoria de 2026-07-29: o ViewModel mais visível do app (a tela
 * "Inteligência") nunca teve teste. Cobre a garantia que mais importa depois da
 * troca patient-aware: o contexto de paciente só entra no caminho de fallback
 * ([AiRequest.taskHint] == "general-chat") — o caminho RAG ([AskLibraryUseCase],
 * `taskHint` == "library-rag") continua exatamente como era, sem paciente
 * misturado no prompt. R2 não foi alterado — só quem chama.
 *
 * JUnit puro (sem Robolectric): [AppContextSource] é a interface mínima que o
 * ViewModel depende, então [FakeAppContextSource] substitui [AppContextBuilder]
 * sem precisar de [com.bioacupunt.security.SecurePreferences] — que exige um
 * `AndroidKeyStore` real e não constrói nem sob o shadow do Robolectric.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UnifiedAiChatViewModelTest {

    /** Captura toda requisição feita ao modelo, na ordem, para inspecionar por caminho. */
    private class SpyAiRepository(
        private val response: Result<AiResult> = Result.success(AiResult(text = "resposta", providerId = "fake", modelId = "fake")),
    ) : AiRepository {
        val requests = mutableListOf<AiRequest>()

        override suspend fun generate(request: AiRequest): Result<AiResult> {
            requests.add(request)
            return response
        }

        override suspend fun stream(request: AiRequest): Flow<String> = flowOf("")
    }

    private val bacoArticle = RetrievedArticle(
        articleId = "org_baco",
        title = "Síndromes do Baço",
        summary = "Deficiência de Qi do Baço.",
        content = """
            # Baço
            ## Deficiência de Qi do Baço
            Cansaço, fezes amolecidas, língua pálida e pulso fraco.
        """.trimIndent(),
        provenance = Provenance.VERIFICAVEL,
    )

    /** Backend sem nenhum artigo: qualquer pergunta cai para o fallback. */
    private class EmptyBackend : ArticleSearchBackend {
        override suspend fun search(query: String, maxResults: Int): List<RetrievedArticle> = emptyList()
    }

    private class MatchingBackend(private val corpus: List<RetrievedArticle>) : ArticleSearchBackend {
        override suspend fun search(query: String, maxResults: Int): List<RetrievedArticle> {
            val terms = MtcSearchEngine.expand(MtcSearchEngine.tokenize(query))
            if (terms.isEmpty()) return emptyList()
            return corpus.filter { article ->
                val haystack = MtcSearchEngine
                    .tokenize("${article.title} ${article.summary} ${article.content}").toSet()
                terms.any { it in haystack }
            }.take(maxResults)
        }
    }

    /** Substitui AppContextBuilder — não precisa de SecurePreferences/AppointmentRepository. */
    private class FakeAppContextSource(private val context: String = "") : AppContextSource {
        override suspend fun build(): String = context
    }

    private class FakeCrmPatientRepository(private val patient: CrmPatient?) : CrmPatientRepository {
        override fun observeAll(): Flow<List<CrmPatient>> = flowOf(emptyList())
        override fun observeByStage(stage: String): Flow<List<CrmPatient>> = flowOf(emptyList())
        override fun search(query: String): Flow<List<CrmPatient>> = flowOf(emptyList())
        override suspend fun getById(id: Long): AppResult<CrmPatient> =
            patient?.let { AppResult.Success(it) } ?: AppResult.Error(com.bioacupunt.core.util.AppError.DatabaseError())
        override suspend fun save(entity: CrmPatient): AppResult<CrmPatient> = AppResult.Success(entity)
        override suspend fun saveAll(entities: List<CrmPatient>): AppResult<Int> = AppResult.Success(entities.size)
        override suspend fun stageCount(stage: String): AppResult<Int> = AppResult.Success(0)
        override suspend fun getPendingSync(since: String): AppResult<List<CrmPatient>> = AppResult.Success(emptyList())
        override suspend fun deleteById(id: Long): AppResult<Unit> = AppResult.Success(Unit)
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

    private fun buildViewModel(
        patientId: Long,
        crmPatientRepository: CrmPatientRepository,
        backend: ArticleSearchBackend,
        ai: SpyAiRepository,
    ): UnifiedAiChatViewModel {
        return UnifiedAiChatViewModel(
            askLibrary = AskLibraryUseCase(MtcRetriever(backend), ai),
            generateAiResponse = GenerateAiResponseUseCase(ai),
            contextBuilder = FakeAppContextSource(),
            crmPatientRepository = crmPatientRepository,
            patientId = patientId,
        )
    }

    // -- patientId = 0: comportamento de hoje, sem contexto de paciente --------

    @Test
    fun noPatient_fallbackRequestCarriesNoPatientContext() = runTest(dispatcher) {
        val ai = SpyAiRepository()
        val vm = buildViewModel(patientId = 0L, crmPatientRepository = FakeCrmPatientRepository(null), backend = EmptyBackend(), ai = ai)
        advanceUntilIdle()

        vm.send("pergunta qualquer sem evidência na biblioteca")
        advanceUntilIdle()

        val fallbackRequest = ai.requests.single { it.taskHint == "general-chat" }
        assertFalse("Sem paciente, o contexto não deve carregar 'Paciente em foco'", fallbackRequest.context.containsKey("Paciente em foco"))
        assertEquals(null, vm.state.value.patientName)
    }

    // -- patientId > 0: nome real entra no contexto, só no fallback ------------

    @Test
    fun withPatient_fallbackRequestCarriesRealPatientName() = runTest(dispatcher) {
        val ai = SpyAiRepository()
        val patient = CrmPatient(id = 42L, name = "Maria Silva", mainComplaint = "lombalgia crônica")
        val vm = buildViewModel(patientId = 42L, crmPatientRepository = FakeCrmPatientRepository(patient), backend = EmptyBackend(), ai = ai)
        advanceUntilIdle()

        assertEquals("Maria Silva", vm.state.value.patientName)

        vm.send("o que você acha desse caso?")
        advanceUntilIdle()

        val fallbackRequest = ai.requests.single { it.taskHint == "general-chat" }
        assertEquals("Maria Silva", fallbackRequest.context["Paciente em foco"])
    }

    // -- Caminho RAG nunca recebe contexto de paciente — R2 intacto ------------

    @Test
    fun withPatient_groundedRequestNeverCarriesPatientContext() = runTest(dispatcher) {
        val ai = SpyAiRepository()
        val patient = CrmPatient(id = 42L, name = "Maria Silva")
        val vm = buildViewModel(
            patientId = 42L,
            crmPatientRepository = FakeCrmPatientRepository(patient),
            backend = MatchingBackend(listOf(bacoArticle)),
            ai = ai,
        )
        advanceUntilIdle()

        vm.send("cansaço e fezes amolecidas, deficiência de qi do baço")
        advanceUntilIdle()

        val groundedRequest = ai.requests.single { it.taskHint == "library-rag" }
        assertTrue(
            "O caminho RAG (R2) nunca deve carregar contexto de paciente — prompt segue definido só por MtcRetriever",
            groundedRequest.context.isEmpty(),
        )
    }

    // -- Memória de sessão: turnos recentes entram no fallback ------------------

    @Test
    fun fallback_includesRecentSessionHistory() = runTest(dispatcher) {
        val ai = SpyAiRepository()
        val vm = buildViewModel(patientId = 0L, crmPatientRepository = FakeCrmPatientRepository(null), backend = EmptyBackend(), ai = ai)
        advanceUntilIdle()

        vm.send("primeira pergunta sem evidência")
        advanceUntilIdle()
        vm.send("segunda pergunta sem evidência")
        advanceUntilIdle()

        val secondFallbackRequest = ai.requests.filter { it.taskHint == "general-chat" }.last()
        val history = secondFallbackRequest.context["Historico recente da conversa"]
        assertTrue(
            "O segundo turno deve carregar a primeira pergunta como histórico de sessão",
            history?.contains("primeira pergunta sem evidência") == true,
        )
    }
}
