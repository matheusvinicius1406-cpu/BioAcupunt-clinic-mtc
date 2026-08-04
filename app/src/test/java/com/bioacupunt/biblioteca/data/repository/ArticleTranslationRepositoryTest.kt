package com.bioacupunt.biblioteca.data.repository

import com.bioacupunt.biblioteca.data.local.ArticleTranslationDao
import com.bioacupunt.biblioteca.data.local.ArticleTranslationEntity
import com.bioacupunt.biblioteca.domain.model.TranslationLanguage
import com.bioacupunt.biblioteca.domain.model.TranslationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArticleTranslationRepositoryTest {

    /** Mesmo padrão de FakeDao usado no resto do projeto — estado em memória, sem Robolectric. */
    private class FakeDao : ArticleTranslationDao {
        private val store = MutableStateFlow<Map<Pair<String, String>, ArticleTranslationEntity>>(emptyMap())

        override suspend fun upsert(entity: ArticleTranslationEntity) {
            store.value = store.value + ((entity.articleId to entity.targetLanguage) to entity)
        }

        override suspend fun getOnce(articleId: String, targetLanguage: String): ArticleTranslationEntity? =
            store.value[articleId to targetLanguage]

        override fun observe(articleId: String, targetLanguage: String): Flow<ArticleTranslationEntity?> =
            store.map { it[articleId to targetLanguage] }
    }

    @Test
    fun markPending_thenGet_returnsPendingWithNoContent() = runTest {
        val repo = ArticleTranslationRepository(FakeDao())

        repo.markPending("art-1", TranslationLanguage.EN, now = 100L)
        val result = repo.get("art-1", TranslationLanguage.EN)

        assertEquals(TranslationStatus.PENDING, result?.status)
        assertEquals("", result?.content)
    }

    @Test
    fun fullLifecycle_pendingThenProcessingThenCompleted() = runTest {
        val repo = ArticleTranslationRepository(FakeDao())

        repo.markPending("art-1", TranslationLanguage.EN, now = 1L)
        repo.markProcessing("art-1", TranslationLanguage.EN, now = 2L)
        repo.markCompleted(
            "art-1", TranslationLanguage.EN,
            title = "T", summary = "S", content = "C", tags = listOf("x", "y"),
            now = 3L,
        )

        val result = repo.get("art-1", TranslationLanguage.EN)
        assertEquals(TranslationStatus.COMPLETED, result?.status)
        assertEquals("T", result?.title)
        assertEquals(listOf("x", "y"), result?.tags)
        assertEquals("nenhum erro de tentativa anterior deve sobreviver a um sucesso", "", result?.errorMessage)
    }

    @Test
    fun markError_afterProcessing_keepsStatusErrorWithMessage() = runTest {
        val repo = ArticleTranslationRepository(FakeDao())

        repo.markProcessing("art-1", TranslationLanguage.EN, now = 1L)
        repo.markError("art-1", TranslationLanguage.EN, message = "modelo indisponível", now = 2L)

        val result = repo.get("art-1", TranslationLanguage.EN)
        assertEquals(TranslationStatus.ERROR, result?.status)
        assertEquals("modelo indisponível", result?.errorMessage)
    }

    @Test
    fun differentLanguagesForTheSameArticle_areIndependentRows() = runTest {
        val repo = ArticleTranslationRepository(FakeDao())

        repo.markCompleted("art-1", TranslationLanguage.EN, "EN title", "", "", emptyList(), now = 1L)
        repo.markPending("art-1", TranslationLanguage.ES, now = 2L)

        assertEquals(
            "trocar o idioma de destino em Ajustes não pode apagar a tradução anterior",
            TranslationStatus.COMPLETED,
            repo.get("art-1", TranslationLanguage.EN)?.status,
        )
        assertEquals(TranslationStatus.PENDING, repo.get("art-1", TranslationLanguage.ES)?.status)
    }

    @Test
    fun noRowYet_getReturnsNull() = runTest {
        val repo = ArticleTranslationRepository(FakeDao())

        assertNull(repo.get("unknown", TranslationLanguage.PT_BR))
    }
}
