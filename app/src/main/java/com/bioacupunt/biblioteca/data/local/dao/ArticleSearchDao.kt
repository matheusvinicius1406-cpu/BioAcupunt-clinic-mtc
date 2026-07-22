package com.bioacupunt.biblioteca.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bioacupunt.biblioteca.data.local.fts.ArticleFtsEntity

/**
 * DAO DE BUSCA TEXTUAL — opera sobre a tabela virtual FTS4 [ArticleFtsEntity].
 *
 * A busca usa a sintaxe FTS4 do SQLite, que suporta:
 * - Termos simples: `MATCH 'acupuntura'`
 * - Prefixos: `MATCH 'defici*'`
 * - Frases: `MATCH '"deficiencia de qi"'`
 * - Operadores: `MATCH 'baco AND figado'`
 *
 * O ranking é feito pela função `bm25()` do FTS4 (quanto menor, mais relevante).
 *
 * ## Pré-processamento
 * O [com.bioacupunt.biblioteca.domain.search.MtcSearchEngine] ainda é usado
 * para expandir sinônimos bilíngues (Português ↔ pinyin) e normalizar acentos
 * ANTES de montar a query FTS4.
 */
@Dao
interface ArticleSearchDao {

    /** Popula/atualiza o índice FTS4. Idempotente por articleId. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<ArticleFtsEntity>)

    /** Remove todos os registros (para rebuild completo do índice). */
    @Query("DELETE FROM article_fts")
    suspend fun clearAll()

    /**
     * Busca textual. O ranking é feito pelo próprio FTS4 (MATCH já ordena
     * por relevância aproximada). Para ordenação BM25 precisa, usaríamos
     * a função `bm25()` do FTS5 (não disponível em FTS4).
     *
     * A ordenação final por relevância é feita em Kotlin no
     * [com.bioacupunt.biblioteca.data.search.FtsSearchService].
     *
     * @param query Termos de busca já pré-processados (expandidos, normalizados)
     * @param limit Máximo de resultados
     */
    @Query("""
        SELECT * 
        FROM article_fts 
        WHERE article_fts MATCH :query 
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int = 20): List<ArticleFtsSearchResult>

    /** Retorna contagem total de documentos no índice. */
    @Query("SELECT count(*) FROM article_fts")
    suspend fun count(): Int
}

/** Resultado de busca textual do FTS4. */
data class ArticleFtsSearchResult(
    val articleId: String,
    val category: String,
    val title: String,
    val summary: String,
    val content: String,
    val tags: String,
    val provenance: String = "RASCUNHO",
)
