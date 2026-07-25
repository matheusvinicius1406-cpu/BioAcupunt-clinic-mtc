package com.bioacupunt.educacao.data.repository

import com.bioacupunt.core.util.Result
import com.bioacupunt.educacao.data.local.FlashcardDao
import com.bioacupunt.educacao.data.local.FlashcardEntity
import com.bioacupunt.educacao.data.local.FlashcardProgressEntity
import com.bioacupunt.educacao.domain.model.Flashcard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlashcardRepositoryTest {

    /** DAO fake em memória, estilo SupremoViewModelTest — sem Robolectric. */
    private class FakeFlashcardDao : FlashcardDao {
        private val cardsFlow = MutableStateFlow<List<FlashcardEntity>>(emptyList())
        private val progressFlow = MutableStateFlow<List<FlashcardProgressEntity>>(emptyList())
        private var nextCardId = 1L
        private var nextProgressId = 1L
        var throwOnSaveCard = false

        override fun observeCards(tenantId: Long): Flow<List<FlashcardEntity>> =
            cardsFlow.map { list -> list.filter { it.tenantId == tenantId } }

        override suspend fun saveCard(entity: FlashcardEntity): Long {
            if (throwOnSaveCard) throw IllegalStateException("db indisponível")
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

    private lateinit var dao: FakeFlashcardDao
    private lateinit var repository: FlashcardRepositoryImpl
    private val tenantId = 1L
    private val nowMs = 1_700_000_000_000L

    @Before
    fun setUp() {
        dao = FakeFlashcardDao()
        repository = FlashcardRepositoryImpl(dao, tenantId = { tenantId })
    }

    @Test
    fun `saveCard refuses builtin cards`() = runTest {
        val builtin = Flashcard(key = "builtin_x", front = "f", back = "b", category = "c", builtin = true)

        val result = repository.saveCard(builtin)

        assertTrue(result is Result.Error)
        assertEquals(0, dao.observeCards(tenantId).first().size)
    }

    @Test
    fun `saveCard stamps the tenant and returns a user_ key with the new row id`() = runTest {
        val draft = Flashcard(key = "", front = "Pergunta", back = "Resposta", category = "Teoria", builtin = false)

        val result = repository.saveCard(draft)

        assertTrue(result is Result.Success)
        val saved = (result as Result.Success).data
        assertNotNull(saved.userRowId)
        assertEquals("user_${saved.userRowId}", saved.key)
        val entity = dao.observeCards(tenantId).first().single()
        assertEquals(tenantId, entity.tenantId)
    }

    @Test
    fun `observeDeck is the union of the 12 builtins and the doctor's cards, each paired with its progress`() = runTest {
        val saved = (repository.saveCard(
            Flashcard(key = "", front = "f", back = "b", category = "c", builtin = false)
        ) as Result.Success).data

        repository.recordReview(saved.key, remembered = true, nowMs = nowMs)

        val deck = repository.observeDeck().first()

        assertEquals(13, deck.size) // 12 fixos + 1 da médica
        val studyCard = deck.first { it.card.key == saved.key }
        assertNotNull("o card recém-revisado deve carregar seu progresso", studyCard.progress)
        assertEquals(1, studyCard.progress!!.box)
    }

    @Test
    fun `reviewing a builtin card creates progress that did not exist before`() = runTest {
        assertNull(dao.getProgress(tenantId, "builtin_de_qi"))

        val result = repository.recordReview("builtin_de_qi", remembered = true, nowMs = nowMs)

        assertTrue(result is Result.Success)
        assertNotNull(dao.getProgress(tenantId, "builtin_de_qi"))
    }

    @Test
    fun `recordReview preserves the row id across subsequent reviews`() = runTest {
        repository.recordReview("builtin_de_qi", remembered = true, nowMs = nowMs)
        val idAfterFirst = dao.getProgress(tenantId, "builtin_de_qi")!!.id

        repository.recordReview("builtin_de_qi", remembered = true, nowMs = nowMs + 1)
        val idAfterSecond = dao.getProgress(tenantId, "builtin_de_qi")!!.id

        assertEquals("REPLACE pelo índice único trocaria o autoincrement", idAfterFirst, idAfterSecond)
    }

    @Test
    fun `deleteCard removes the card and its progress together`() = runTest {
        val saved = (repository.saveCard(
            Flashcard(key = "", front = "f", back = "b", category = "c", builtin = false)
        ) as Result.Success).data
        repository.recordReview(saved.key, remembered = true, nowMs = nowMs)
        assertNotNull(dao.getProgress(tenantId, saved.key))

        val result = repository.deleteCard(saved.userRowId!!)

        assertTrue(result is Result.Success)
        assertTrue(dao.observeCards(tenantId).first().isEmpty())
        assertNull("sem FK — o repositório apaga o progresso órfão manualmente", dao.getProgress(tenantId, saved.key))
    }

    @Test
    fun `a DAO exception becomes Result Error, never an uncaught throw`() = runTest {
        dao.throwOnSaveCard = true

        val result = repository.saveCard(Flashcard(key = "", front = "f", back = "b", category = "c", builtin = false))

        assertTrue(result is Result.Error)
    }
}
