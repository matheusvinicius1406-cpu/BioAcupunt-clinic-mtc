package com.bioacupunt.educacao.presentation

import com.bioacupunt.biblioteca.domain.model.MtcArticle
import com.bioacupunt.core.util.MarkdownSections
import com.bioacupunt.educacao.data.local.FlashcardDao
import com.bioacupunt.educacao.data.local.FlashcardEntity
import com.bioacupunt.educacao.data.local.FlashcardProgressEntity
import com.bioacupunt.educacao.data.repository.FlashcardRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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

@OptIn(ExperimentalCoroutinesApi::class)
class FlashcardsViewModelTest {

    /** Mesmo padrão de FakeDao de FlashcardRepositoryTest — sem infra compartilhada entre arquivos de teste. */
    private class FakeFlashcardDao : FlashcardDao {
        private val cardsFlow = MutableStateFlow<List<FlashcardEntity>>(emptyList())
        private val progressFlow = MutableStateFlow<List<FlashcardProgressEntity>>(emptyList())
        private var nextCardId = 1L
        private var nextProgressId = 1L

        fun seedProgress(entity: FlashcardProgressEntity) {
            val id = if (entity.id == 0L) nextProgressId++ else entity.id
            progressFlow.update { it.filterNot { p -> p.id == id } + entity.copy(id = id) }
        }

        override fun observeCards(tenantId: Long): Flow<List<FlashcardEntity>> =
            cardsFlow.map { list -> list.filter { it.tenantId == tenantId } }

        override suspend fun saveCard(entity: FlashcardEntity): Long {
            val id = if (entity.id == 0L) nextCardId++ else entity.id
            val saved = entity.copy(id = id)
            cardsFlow.update { list -> list.filterNot { it.id == id } + saved }
            return id
        }

        override suspend fun deleteCard(id: Long, tenantId: Long) {
            cardsFlow.update { list -> list.filterNot { it.id == id && it.tenantId == tenantId } }
        }

        override fun observeProgress(tenantId: Long): Flow<List<FlashcardProgressEntity>> =
            progressFlow.map { list -> list.filter { it.tenantId == tenantId } }

        override suspend fun getProgress(tenantId: Long, cardKey: String): FlashcardProgressEntity? =
            progressFlow.value.firstOrNull { it.tenantId == tenantId && it.cardKey == cardKey }

        override suspend fun saveProgress(entity: FlashcardProgressEntity): Long {
            val id = if (entity.id == 0L) nextProgressId++ else entity.id
            val saved = entity.copy(id = id)
            progressFlow.update { list -> list.filterNot { it.id == id } + saved }
            return id
        }

        override suspend fun deleteProgress(tenantId: Long, cardKey: String) {
            progressFlow.update { list -> list.filterNot { it.tenantId == tenantId && it.cardKey == cardKey } }
        }
    }

    private val dispatcher = StandardTestDispatcher()
    private val tenantId = 1L
    private val nowMs = 1_700_000_000_000L
    private val dayMs = 24L * 60 * 60 * 1000
    private lateinit var dao: FakeFlashcardDao

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dao = FakeFlashcardDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(sourceArticles: suspend () -> List<MtcArticle> = { emptyList() }): FlashcardsViewModel {
        val repository = FlashcardRepositoryImpl(dao, tenantId = { tenantId })
        return FlashcardsViewModel(repository, sourceArticles, clock = { nowMs })
    }

    @Test
    fun `queue is ordered box ascending, then due date ascending`() = runTest(dispatcher) {
        // "Teoria" tem exatamente 2 fixos: builtin_qi_sangue e builtin_shen.
        dao.seedProgress(FlashcardProgressEntity(tenantId = tenantId, cardKey = "builtin_qi_sangue", box = 1, dueAtEpochMs = nowMs - 500, lastReviewedAtEpochMs = 0, totalReviews = 1, totalLapses = 0))
        dao.seedProgress(FlashcardProgressEntity(tenantId = tenantId, cardKey = "builtin_shen", box = 0, dueAtEpochMs = nowMs - 10, lastReviewedAtEpochMs = 0, totalReviews = 1, totalLapses = 0))

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onCategorySelected("Teoria")
        advanceUntilIdle()

        val keys = vm.state.value.queue.map { it.card.key }
        assertEquals("box 0 vem antes de box 1, mesmo vencido há menos tempo", listOf("builtin_shen", "builtin_qi_sangue"), keys)
    }

    @Test
    fun `answering persists the review and advances the queue`() = runTest(dispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        val firstKey = vm.state.value.currentCard!!.card.key

        vm.onAnswer(true)
        advanceUntilIdle()

        assertEquals(1, vm.state.value.queueIndex)
        assertEquals(1, vm.state.value.knownCount)
        val persisted = dao.getProgress(tenantId, firstKey)
        assertEquals(1, persisted!!.box)
    }

    @Test
    fun `the queue never reorders mid-session, even after a review changes a card's box`() = runTest(dispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        val queueBefore = vm.state.value.queue.map { it.card.key }

        vm.onAnswer(true)
        advanceUntilIdle()

        val queueAfter = vm.state.value.queue.map { it.card.key }
        assertEquals("a ordem/composição da fila não pode mudar debaixo do dedo da médica", queueBefore, queueAfter)
    }

    @Test
    fun `dueCount drops reactively once a card is answered and pushed out of the due window`() = runTest(dispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        val dueBefore = vm.state.value.dueCount
        assertEquals(12, dueBefore) // 12 builtins, nenhum com progresso ainda

        vm.onAnswer(true) // box 0 -> 1, due amanhã: sai da janela "vencido"
        advanceUntilIdle()

        assertEquals(dueBefore - 1, vm.state.value.dueCount)
    }

    @Test
    fun `no cards at all vs no cards due are distinguishable states`() = runTest(dispatcher) {
        // "Ba Gang" tem 1 fixo (builtin_ba_gang); marca como vencendo só daqui a 5 dias.
        dao.seedProgress(FlashcardProgressEntity(tenantId = tenantId, cardKey = "builtin_ba_gang", box = 1, dueAtEpochMs = nowMs + 5 * dayMs, lastReviewedAtEpochMs = 0, totalReviews = 1, totalLapses = 0))
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onCategorySelected("Categoria Que Não Existe")
        advanceUntilIdle()
        assertTrue("nenhum card: deck filtrado vazio", vm.state.value.deckForCategory().isEmpty())
        assertTrue(vm.state.value.queue.isEmpty())

        vm.onCategorySelected("Ba Gang")
        advanceUntilIdle()
        assertTrue("nada vencido: deck filtrado tem card, fila não", vm.state.value.deckForCategory().isNotEmpty())
        assertTrue(vm.state.value.queue.isEmpty())
        assertEquals(nowMs + 5 * dayMs, vm.state.value.nextDueAtEpochMs)

        vm.studyAnyway()
        advanceUntilIdle()
        assertEquals(1, vm.state.value.queue.size)
    }

    @Test
    fun `create-from-article prefills front and back verbatim, and saves nothing before confirm`() = runTest(dispatcher) {
        val article = MtcArticle(
            id = "art1",
            title = "Artigo de Teste",
            category = "PONTOS",
            summary = "resumo",
            content = "# Seção Um\nCorpo da seção um.",
        )
        val vm = buildViewModel(sourceArticles = { listOf(article) })
        advanceUntilIdle()

        vm.startCreateFromArticle()
        advanceUntilIdle()
        assertEquals(listOf(article), vm.state.value.createFromArticle!!.articles)

        vm.selectArticleForCreate(article)
        val sections = vm.state.value.createFromArticle!!.sections
        assertEquals(1, sections.size)

        vm.selectSectionForCreate(sections.first())

        val editor = vm.state.value.editor!!
        assertEquals(MarkdownSections.titleOf(sections.first()), editor.front)
        assertEquals(MarkdownSections.bodyOf(sections.first()), editor.back)
        assertEquals("art1", editor.sourceArticleId)
        assertNull("o picker fecha ao abrir o editor", vm.state.value.createFromArticle)
        assertTrue("nada foi persistido só de pré-preencher", vm.state.value.deck.none { it.card.sourceArticleId == "art1" })

        vm.confirmEditor()
        advanceUntilIdle()

        assertTrue(vm.state.value.deck.any { it.card.sourceArticleId == "art1" })
    }
}
