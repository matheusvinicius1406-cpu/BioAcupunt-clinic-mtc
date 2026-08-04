package com.bioacupunt.biblioteca.data.local

import androidx.room.Entity

/**
 * Uma linha por (artigo, idioma de destino) — ver [com.bioacupunt.biblioteca.domain.model.ArticleTranslation].
 * Sem FK para `biblioteca_nodes`: reaprovar/reimportar um artigo nunca pode travar numa FK
 * (mesmo raciocínio já aplicado a `medicamentos`/`flashcards`).
 */
@Entity(tableName = "article_translations", primaryKeys = ["articleId", "targetLanguage"])
data class ArticleTranslationEntity(
    val articleId: String,
    val targetLanguage: String,
    val status: String,
    val title: String = "",
    val summary: String = "",
    val content: String = "",
    val tagsCsv: String = "",
    val errorMessage: String = "",
    val updatedAt: Long = 0L,
)
