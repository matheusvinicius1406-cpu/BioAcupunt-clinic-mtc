package com.bioacupunt.biblioteca.data.repository

import com.bioacupunt.biblioteca.data.local.ArticleTranslationDao
import com.bioacupunt.biblioteca.data.local.ArticleTranslationEntity
import com.bioacupunt.biblioteca.domain.model.ArticleTranslation
import com.bioacupunt.biblioteca.domain.model.TranslationLanguage
import com.bioacupunt.biblioteca.domain.model.TranslationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persistência do tradutor automático. Uma linha por (artigo, idioma) — trocar o idioma de
 * destino em Ajustes não apaga traduções antigas feitas para o idioma anterior; elas só
 * deixam de aparecer, porque [observe]/[get] procuram pela chave (articleId, idioma ATUAL).
 * Ver [com.bioacupunt.ui.screens.ArticleDetailSheet] para o botão "Traduzir para X" que
 * cobre esse caso sob demanda.
 */
class ArticleTranslationRepository(
    private val dao: ArticleTranslationDao,
) {
    private fun toDomain(e: ArticleTranslationEntity): ArticleTranslation? {
        val language = TranslationLanguage.byCode(e.targetLanguage) ?: return null
        val status = runCatching { TranslationStatus.valueOf(e.status) }.getOrDefault(TranslationStatus.ERROR)
        return ArticleTranslation(
            articleId = e.articleId,
            targetLanguage = language,
            status = status,
            title = e.title,
            summary = e.summary,
            content = e.content,
            tags = e.tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            errorMessage = e.errorMessage,
            updatedAt = e.updatedAt,
        )
    }

    fun observe(articleId: String, targetLanguage: TranslationLanguage): Flow<ArticleTranslation?> =
        dao.observe(articleId, targetLanguage.code).map { it?.let(::toDomain) }

    suspend fun get(articleId: String, targetLanguage: TranslationLanguage): ArticleTranslation? =
        dao.getOnce(articleId, targetLanguage.code)?.let(::toDomain)

    /** Escrito na hora em que a tradução é enfileirada — antes do worker sequer começar a rodar. */
    suspend fun markPending(articleId: String, targetLanguage: TranslationLanguage, now: Long) {
        dao.upsert(
            ArticleTranslationEntity(
                articleId = articleId,
                targetLanguage = targetLanguage.code,
                status = TranslationStatus.PENDING.name,
                updatedAt = now,
            ),
        )
    }

    suspend fun markProcessing(articleId: String, targetLanguage: TranslationLanguage, now: Long) {
        val existing = dao.getOnce(articleId, targetLanguage.code)
            ?: ArticleTranslationEntity(articleId, targetLanguage.code, TranslationStatus.PENDING.name)
        dao.upsert(existing.copy(status = TranslationStatus.PROCESSING.name, updatedAt = now))
    }

    suspend fun markCompleted(
        articleId: String,
        targetLanguage: TranslationLanguage,
        title: String,
        summary: String,
        content: String,
        tags: List<String>,
        now: Long,
    ) {
        dao.upsert(
            ArticleTranslationEntity(
                articleId = articleId,
                targetLanguage = targetLanguage.code,
                status = TranslationStatus.COMPLETED.name,
                title = title,
                summary = summary,
                content = content,
                tagsCsv = tags.joinToString(","),
                updatedAt = now,
            ),
        )
    }

    suspend fun markError(articleId: String, targetLanguage: TranslationLanguage, message: String, now: Long) {
        val existing = dao.getOnce(articleId, targetLanguage.code)
            ?: ArticleTranslationEntity(articleId, targetLanguage.code, TranslationStatus.ERROR.name)
        dao.upsert(existing.copy(status = TranslationStatus.ERROR.name, errorMessage = message, updatedAt = now))
    }
}
