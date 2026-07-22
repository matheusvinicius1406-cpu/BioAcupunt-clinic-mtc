package com.bioacupunt.biblioteca.data.search

import com.bioacupunt.biblioteca.data.MtcKnowledgeBase
import com.bioacupunt.biblioteca.data.local.dao.ArticleSearchDao
import com.bioacupunt.biblioteca.data.local.dao.ArticleFtsSearchResult
import com.bioacupunt.biblioteca.data.local.fts.ArticleFtsEntity
import com.bioacupunt.biblioteca.data.repository.LibraryStagingRepository
import com.bioacupunt.biblioteca.domain.model.MtcArticle
import com.bioacupunt.biblioteca.domain.search.ArticleSearchBackend
import com.bioacupunt.biblioteca.domain.ingestion.Provenance
import com.bioacupunt.biblioteca.domain.search.MtcSearchEngine
import com.bioacupunt.biblioteca.domain.search.RetrievedArticle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * SERVIÇO DE BUSCA FTS4 — substitui o BM25 em memória por busca no SQLite.
 *
 * ## Por que FTS4 e não BM25 em memória?
 * O índice BM25 do [MtcSearchEngine] carrega todos os artigos em RAM e reconstrói
 * o índice a cada mudança. Com 2.500+ artigos (fixos + aprovados via curadoria),
 * isso consome memória e CPU desnecessariamente.
 *
 * O SQLite FTS4 oferece:
 * - **Busca nativa** com match de termos, prefixos e frases
 * - **Ranking BM25** embutido (função `rank`)
 * - **Zero overhead de memória** — o índice fica no banco, não na heap
 * - **Comandos SQL** como interface (o que o usuário chama de "comando")
 *
 * ## Pré-processamento
 * A busca ainda usa [MtcSearchEngine] para:
 * - Normalizar acentos (Pulmão → pulmao)
 * - Expandir sinônimos bilíngues (baco → baco pi)
 * - Remover stopwords
 *
 * ## Ciclo de vida
 * - [ensureIndexed]: chamado na primeira busca, sincroniza artigos fixos + aprovados
 * - [rebuildIndex]: chamado após curadoria (novos artigos aprovados)
 * - [search]: busca textual com FTS4 + ranking
 */
class FtsSearchService(
    private val searchDao: ArticleSearchDao,
    private val stagingRepo: LibraryStagingRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : ArticleSearchBackend {
    @Volatile
    private var indexed = false

    @Volatile
    private var rebuilding = false

    /**
     * Garante que o índice FTS4 esteja populado.
     * Seguro chamar múltiplas vezes — só executa o sync na primeira.
     *
     * A construção do índice (suspend) acontece FORA da seção crítica;
     * apenas a escrita do flag `indexed` fica dentro do synchronized.
     */
    suspend fun ensureIndexed() {
        if (indexed) return
        // Constrói o índice fora do synchronized (pode conter suspend calls)
        rebuildIndex()
        synchronized(this) {
            indexed = true
        }
    }

    /**
     * Notifica que o conteúdo da biblioteca mudou (ex.: novos artigos
     * aprovados na curadoria). O índice é RECONSTRUÍDO IMEDIATAMENTE
     * em background — a próxima busca já encontra os novos artigos.
     */
    fun notifyContentChanged() {
        // Marca o índice como desatualizado e dispara rebuild imediato.
        // Sincronizado para evitar corrida na flag `rebuilding`:
        // apenas uma thread dispara o rebuild por vez.
        synchronized(this) {
            indexed = false
            if (rebuilding) return  // já tem um rebuild em andamento
            rebuilding = true
        }
        scope.launch {
            try {
                rebuildIndex()
                synchronized(this@FtsSearchService) {
                    indexed = true
                }
            } finally {
                synchronized(this@FtsSearchService) {
                    rebuilding = false
                }
            }
        }
    }

    /** Força a reconstrução completa do índice (após curadoria). */
    suspend fun rebuildIndex() {
        // 1. Artigos fixos da MtcKnowledgeBase (sempre disponíveis)
        val fixed = MtcKnowledgeBase.articles.map { it.toFtsEntity() }

        // 2. Artigos aprovados via curadoria
        val approved = runCatching { stagingRepo.approvedArticlesWithMeta() }
            .getOrDefault(emptyList())
            .map { (article, meta) ->
                article.toFtsEntity(provenance = meta.provenance.name)
            }

        // 3. Evita duplicatas: artigos fixos têm prioridade (id igual)
        val deduplicated = (fixed + approved)
            .distinctBy { it.articleId }

        // 4. Reinsere tudo no índice FTS4
        searchDao.clearAll()
        searchDao.insertAll(deduplicated)
    }

    /**
     * Busca textual com pré-processamento de query.
     *
     * Contrato de [ArticleSearchBackend]: sem resultado ⇒ lista vazia. Nunca um
     * match fraco de consolação — o [MtcRetriever] depende do vazio para decidir
     * não chamar o modelo (R2).
     *
     * @param query Texto livre do usuário
     * @param maxResults Máximo de resultados
     * @return Resultados ordenados por relevância BM25
     */
    override suspend fun search(query: String, maxResults: Int): List<RetrievedArticle> {
        if (query.isBlank()) return emptyList()

        ensureIndexed()

        // Pré-processa a query: expande sinônimos, normaliza
        val processed = preprocessQuery(query)
        if (processed.isBlank()) return emptyList()

        return searchDao.search(processed, maxResults).map { it.toDomain() }
    }

    // ── Internals ──────────────────────────────────────────

    /** Pré-processa a query: normaliza acentos + expande sinônimos. */
    private fun preprocessQuery(text: String): String {
        val tokens = MtcSearchEngine.tokenize(text)
        val expanded = MtcSearchEngine.expand(tokens)
        if (expanded.isEmpty()) return ""
        // Constrói query FTS4: junta termos com OR
        return expanded.joinToString(" OR ") { term -> "$term*" }
    }

}

/** Converte o resultado do FTS4 para o tipo que o domínio conhece. */
private fun ArticleFtsSearchResult.toDomain() = RetrievedArticle(
    articleId = articleId,
    title = title,
    summary = summary,
    content = content,
    provenance = runCatching { Provenance.valueOf(provenance) }
        .getOrDefault(Provenance.RASCUNHO),
)

/** Converte um [MtcArticle] para entidade FTS4. */
private fun MtcArticle.toFtsEntity(provenance: String = Provenance.RASCUNHO.name) = ArticleFtsEntity(
    articleId = id,
    category = category,
    title = title,
    summary = summary,
    content = content,
    tags = tags.joinToString(" "),
    provenance = provenance,
)
