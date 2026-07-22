package com.bioacupunt.biblioteca.data.search

import com.bioacupunt.ai.embedding.EmbeddingService
import com.bioacupunt.biblioteca.domain.ingestion.Provenance
import com.bioacupunt.biblioteca.domain.search.ArticleSearchBackend
import com.bioacupunt.biblioteca.domain.search.MtcSearchEngine
import com.bioacupunt.biblioteca.domain.search.RetrievedArticle
import com.bioacupunt.data.local.database.Fts5SearchResult
import com.bioacupunt.data.local.database.VecKnowledgeNodeRepository
import com.bioacupunt.data.local.database.VecSearchResult
import com.bioacupunt.observability.AppLogger

/**
 * SERVIÇO DE BUSCA HÍBRIDA — funde resultados de FTS5 (textual) com sqlite-vec
 * (semântico/vetorial) usando Reciprocal Rank Fusion (RRF).
 *
 * ## Arquitetura
 * ```text
 * Consulta do usuário
 *      │
 *      ├─→ FTS5 (BM25) — captura matches de termos exatos + sinônimos PT↔ZH
 *      │
 *      ├─→ sqlite-vec (e5-small 384d) — captura similaridade semântica
 *      │     (apenas se o EmbeddingService estiver pronto)
 *      │
 *      └─→ RRF Fusion — combina rankings normalizados
 *           ↓
 *      Top-K resultados → RetrievedArticle
 * ```
 *
 * ## Reciprocal Rank Fusion (RRF)
 * ```text
 * score(item) = Σ 1 / (k + rank_r(item))
 *   onde r = cada sistema de busca (FTS5, vec)
 *         k = constante de fusão (default 60)
 * ```
 *
 * ## Degradação graciosa
 * - Se o EmbeddingService não está pronto → busca apenas FTS5
 * - Se a FTS5 não retorna nada → busca apenas vetorial
 * - Se ambos falham → lista vazia (R2 gate intacto)
 *
 * ## Thread safety
 * - [search] é suspenso e thread-safe
 * - Acesso ao VecKnowledgeNodeRepository é bloqueante (SQLite), mas rápido
 * - EmbeddingService usa mutex interno para serializar inferência
 */
class HybridSearchService(
    private val vecRepo: VecKnowledgeNodeRepository,
    private val embeddingService: EmbeddingService,
    private val config: HybridSearchConfig = HybridSearchConfig(),
) : ArticleSearchBackend {

    override suspend fun search(query: String, maxResults: Int): List<RetrievedArticle> {
        if (query.isBlank()) return emptyList()

        // 1. Pré-processar query: normalizar + expandir sinônimos
        val tokens = MtcSearchEngine.tokenize(query)
        if (tokens.isEmpty()) return emptyList()

        val expanded = MtcSearchEngine.expand(tokens)
        val fts5Query = buildFts5Query(expanded)
        if (fts5Query.isBlank()) return emptyList()

        // 2. Busca textual (FTS5)
        val fts5Results = searchFts5(fts5Query, config.fts5Limit)
        AppLogger.d(TAG, "FTS5 retornou ${fts5Results.size} resultados para query='$fts5Query'")

        // 3. Busca vetorial (sqlite-vec) — apenas se embedding estiver pronto
        val vecResults = if (embeddingService.isReady) {
            searchVector(query, config.vecLimit)
        } else {
            AppLogger.d(TAG, "EmbeddingService não pronto. Busca apenas FTS5.")
            emptyList()
        }

        // 4. Fusão RRF + normalização
        val fused = fuseResults(fts5Results, vecResults, maxResults)
        AppLogger.d(TAG, "Busca híbrida: FTS5=${fts5Results.size}, vec=${vecResults.size}, fused=${fused.size}")

        return fused
    }

    // ======================== FTS5 ========================

    /**
     * Busca textual no FTS5 com prefix matching + query expandida.
     */
    private fun searchFts5(query: String, limit: Int): List<RankedResult> {
        val results = vecRepo.searchFts5(query, limit)
        return results.map { it.toRankedResult() }
    }

    // ======================== Vetorial ========================

    /**
     * Busca semântica no sqlite-vec.
     * Gera embedding da query e busca vizinhos mais próximos.
     */
    private suspend fun searchVector(query: String, limit: Int): List<RankedResult> {
        return try {
            val embeddingBlob = embeddingService.embedToBlob(query, isQuery = true)
            val results = vecRepo.search(
                embeddingBlob = embeddingBlob,
                limit = limit,
                statusFilter = "aprovado", // R2: só nós aprovados entram na busca clínica
            )
            results.map { it.toRankedResult() }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Falha na busca vetorial", e)
            emptyList()
        }
    }

    // ======================== Fusão RRF ========================

    /**
     * Funde os resultados de FTS5 e busca vetorial usando RRF.
     *
     * @param fts5 Lista de resultados FTS5 ordenados por score (BM25) descendente
     * @param vec  Lista de resultados vetoriais ordenados por distance (coseno) ascendente
     * @param topK Número máximo de resultados finais
     * @return Lista fundida e reordenada
     */
    private fun fuseResults(
        fts5: List<RankedResult>,
        vec: List<RankedResult>,
        topK: Int,
    ): List<RetrievedArticle> {
        // Se apenas um sistema retornou resultados, usar ele diretamente
        if (fts5.isEmpty() && vec.isEmpty()) return emptyList()
        if (fts5.isEmpty()) return vec.take(topK).map { it.toRetrievedArticle() }
        if (vec.isEmpty()) return fts5.take(topK).map { it.toRetrievedArticle() }

        // RRF: cada resultado recebe score = 1/(k + rank) de cada sistema
        // onde rank é a posição (1-based) na lista de cada sistema
        val fts5Rank = fts5.withIndex().associate { (idx, r) -> r.id to (idx + 1) }
        val vecRank = vec.withIndex().associate { (idx, r) -> r.id to (idx + 1) }

        val allIds = (fts5Rank.keys + vecRank.keys).toSet()

        // Calcular RRF score para cada item
        data class FusedItem(
            val result: RankedResult,
            val score: Double,
        )

        val scored = allIds.map { id ->
            val rankFts5 = fts5Rank[id]
            val rankVec = vecRank[id]
            val result = fts5Rank[id]?.let { _ -> fts5.first { it.id == id } }
                ?: vec.first { it.id == id }

            val score = calculateRRF(rankFts5, rankVec)
            FusedItem(result, score)
        }

        // Ordenar por score descendente e retornar top-K
        return scored
            .sortedByDescending { it.score }
            .take(topK)
            .map { it.result.toRetrievedArticle() }
    }

    /**
     * Calcula o RRF score para um item.
     *
     * @param rankFts5 Posição (1-based) no ranking FTS5, ou null se não encontrado
     * @param rankVec  Posição (1-based) no ranking vetorial, ou null se não encontrado
     */
    private fun calculateRRF(rankFts5: Int?, rankVec: Int?): Double {
        val k = config.rrfConstant
        var score = 0.0
        if (rankFts5 != null) score += 1.0 / (k + rankFts5)
        if (rankVec != null) score += 1.0 / (k + rankVec)

        if (config.vecWeight != 1.0 || config.fts5Weight != 1.0) {
            score = score * (config.fts5Weight + config.vecWeight) / 2.0
        }
        return score
    }

    // ======================== Query Building ========================

    /**
     * Constrói query para FTS5 a partir dos tokens expandidos.
     * Usa prefix matching (termo*) + OR para capturar variantes.
     */
    private fun buildFts5Query(expandedTerms: List<String>): String {
        if (expandedTerms.isEmpty()) return ""

        // FTS5 prefix query: cada termo vira "termo*", unidos por OR
        // (AND implícito no FTS5 é restritivo demais com sinônimos expandidos:
        //  "baco pi" exigiria ambos os termos, enquanto OR captura qualquer variante)
        return expandedTerms
            .distinct()
            .joinToString(" OR ") { term ->
                val escaped = term.replace(Regex("[\"^\\\\-]"), " ")
                if (escaped.isNotBlank()) "$escaped*" else ""
            }
            .trim()
    }

    companion object {
        private const val TAG = "HybridSearch"
    }
}

// ======================== Modelos ========================

/**
 * Configuração da busca híbrida.
 */
data class HybridSearchConfig(
    /** Constante RRF (default 60, range típico 30-100). */
    val rrfConstant: Int = 60,
    /** Peso relativo da busca FTS5 na fusão. */
    val fts5Weight: Double = 1.0,
    /** Peso relativo da busca vetorial na fusão. */
    val vecWeight: Double = 1.0,
    /** Número máximo de resultados do FTS5 para fusão. */
    val fts5Limit: Int = 20,
    /** Número máximo de resultados da busca vetorial para fusão. */
    val vecLimit: Int = 20,
)

/**
 * Resultado intermediário normalizado de um dos sistemas de busca.
 * Ambos FTS5 e vec retornam o mesmo formato para fusão.
 */
data class RankedResult(
    val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val score: Double,
)

/** Converte [Fts5SearchResult] → [RankedResult]. */
private fun Fts5SearchResult.toRankedResult() = RankedResult(
    id = nodeId,
    title = title,
    summary = summary,
    content = content,
    score = score, // BM25: quanto maior, melhor
)

/** Converte [VecSearchResult] → [RankedResult]. */
private fun VecSearchResult.toRankedResult() = RankedResult(
    id = id,
    title = title,
    summary = summary,
    content = content,
    score = -distance, // distance coseno: quanto menor, melhor → inverter sinal
)

/** Converte [RankedResult] → [RetrievedArticle] (domínio). */
private fun RankedResult.toRetrievedArticle() = RetrievedArticle(
    articleId = id,
    title = title,
    summary = summary,
    content = content,
    provenance = Provenance.VERIFICAVEL, // nós do MKIS são verificáveis por padrão
)
