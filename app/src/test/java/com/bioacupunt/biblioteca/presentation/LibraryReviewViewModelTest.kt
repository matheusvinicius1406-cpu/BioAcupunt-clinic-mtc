package com.bioacupunt.biblioteca.presentation

import com.bioacupunt.biblioteca.data.local.BibliotecaDao
import com.bioacupunt.biblioteca.data.local.BibliotecaNodeEntity
import com.bioacupunt.biblioteca.data.repository.LibraryStagingRepository
import com.bioacupunt.biblioteca.domain.ingestion.LibraryContentItem
import com.bioacupunt.biblioteca.domain.ingestion.LibraryContentPack
import com.bioacupunt.biblioteca.domain.model.MtcArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Esta classe nunca tinha teste. Cobre a única integração nova desta sessão: `approve()`
 * disparando o tradutor automático via `onArticleApproved` — mesmo gancho que já existia
 * para `triggerStudyMaterialGeneration`. O risco real é o anti-padrão #1 do CLAUDE.md ("UI
 * que promete e não cumpre") na sua forma de callback: fácil de declarar o parâmetro e
 * esquecer de invocá-lo, ou invocá-lo mesmo quando a aprovação falhou.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryReviewViewModelTest {

    /** Mesmo padrão de FakeDao do resto do projeto — estado em memória, sem Robolectric. */
    private class FakeBibliotecaDao : BibliotecaDao {
        private val state = MutableStateFlow<Map<String, BibliotecaNodeEntity>>(emptyMap())

        override fun observeAll(): Flow<List<BibliotecaNodeEntity>> = state.map { it.values.toList() }

        override suspend fun insertAll(nodes: List<BibliotecaNodeEntity>) {
            state.value = state.value + nodes.associateBy { it.id }
        }

        override suspend fun getAllOnce(): List<BibliotecaNodeEntity> = state.value.values.toList()

        override suspend fun getById(id: String): BibliotecaNodeEntity? = state.value[id]

        override fun search(query: String): Flow<List<BibliotecaNodeEntity>> =
            state.map { map -> map.values.filter { it.title.contains(query) || it.content.contains(query) } }
    }

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun pack() = LibraryContentPack(
        source = "Fonte de teste",
        items = listOf(
            LibraryContentItem(
                id = "art-1", title = "Título", category = "PONTOS", summary = "Resumo",
                content = "# A\nConteúdo.", citation = "Autor, 2026",
            ),
        ),
    )

    /** `pending` é `stateIn(..., WhileSubscribed(5_000), ...)` — sem um coletor ativo, o
     * flow de origem nunca começa a rodar e `pending.value` fica preso em emptyList(). */
    private fun kotlinx.coroutines.test.TestScope.collectPending(vm: LibraryReviewViewModel) =
        launch { vm.pending.collect {} }

    @Test
    fun `approve invokes onArticleApproved with the approved article`() = runTest(dispatcher) {
        val repo = LibraryStagingRepository(FakeBibliotecaDao())
        repo.stagePack(pack(), now = 1L)
        var approvedArticle: MtcArticle? = null
        val vm = LibraryReviewViewModel(repo = repo, onArticleApproved = { approvedArticle = it })
        val collector = collectPending(vm)
        advanceUntilIdle()

        vm.approve("art-1")
        advanceUntilIdle()

        assertEquals("art-1", approvedArticle?.id)
        collector.cancel()
    }

    @Test
    fun `reject never invokes onArticleApproved`() = runTest(dispatcher) {
        val repo = LibraryStagingRepository(FakeBibliotecaDao())
        repo.stagePack(pack(), now = 1L)
        var calls = 0
        val vm = LibraryReviewViewModel(repo = repo, onArticleApproved = { calls++ })
        val collector = collectPending(vm)
        advanceUntilIdle()

        vm.reject("art-1")
        advanceUntilIdle()

        assertEquals(0, calls)
        collector.cancel()
    }

    @Test
    fun `approving an unknown id never invokes onArticleApproved`() = runTest(dispatcher) {
        val repo = LibraryStagingRepository(FakeBibliotecaDao())
        var calls = 0
        val vm = LibraryReviewViewModel(repo = repo, onArticleApproved = { calls++ })
        val collector = collectPending(vm)
        advanceUntilIdle()

        vm.approve("does-not-exist")
        advanceUntilIdle()

        assertEquals(0, calls)
        collector.cancel()
    }
}
