package com.bioacupunt.biblioteca.data.local.fts

import androidx.room.Entity
import androidx.room.Fts4
import com.bioacupunt.biblioteca.domain.ingestion.Provenance

/**
 * FTS4 VIRTUAL TABLE — índice de busca dos artigos da biblioteca.
 *
 * Ao contrário do BM25 em memória (que não escala para milhares de artigos),
 * o SQLite FTS4 processa buscas textuais com matching nativo + ranking,
 * suportando milhões de documentos sem carregar tudo em RAM.
 *
 * ## Como funciona
 * - FTS4 é uma tabela virtual que indexa o texto dos campos declarados
 * - Suporta busca por termos, frases, prefixos e operadores booleanos
 * - Ranking nativo via função `rank` (BM25 embutido no SQLite)
 * - Sem `contentEntity`: a tabela é independente, alimentada manualmente
 *
 * ## População
 * O [FtsSearchService] sincroniza:
 * 1. Os 16 artigos fixos da [MtcKnowledgeBase]
 * 2. Os artigos aprovados via curadoria (da [biblioteca_nodes])
 *
 * ## Limpeza
 * A tabela é recriada quando artigos são aprovados/rejeitados na curadoria.
 * Como é virtual, não há armazenamento duplicado significativo.
 */
@Fts4
@Entity(tableName = "article_fts")
data class ArticleFtsEntity(
    /** ID único do artigo (ex.: "mer_pulmao", "mtc_sind_001") */
    val articleId: String,
    /** Categoria MTC (MERIDIANOS, SINDROME_ORGAOS, etc.) */
    val category: String,
    val title: String,
    val summary: String,
    val content: String,
    /** Tags separadas por espaço — FTS4 tokeniza automaticamente */
    val tags: String,
    /**
     * Proveniência: "VERIFICAVEL" ou "RASCUNHO".
     * Itens VERIFICAVEL têm localizador de fonte (página/capítulo);
     * itens RASCUNHO são não-conferíveis (gerados por IA ou sem localizador).
     * Default "RASCUNHO" para artigos que existiam antes desta coluna.
     */
    val provenance: String = Provenance.RASCUNHO.name,
)
