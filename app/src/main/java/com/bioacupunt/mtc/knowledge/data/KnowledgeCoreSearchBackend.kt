package com.bioacupunt.mtc.knowledge.data

import com.bioacupunt.biblioteca.domain.ingestion.Provenance
import com.bioacupunt.biblioteca.domain.search.ArticleSearchBackend
import com.bioacupunt.biblioteca.domain.search.RetrievedArticle
import com.bioacupunt.mtc.knowledge.domain.KnowledgeStatus
import com.bioacupunt.mtc.knowledge.repository.KnowledgeSearchRepository
import com.bioacupunt.observability.AppLogger

/**
 * ADAPTER — wraps [KnowledgeSearchRepository] as [ArticleSearchBackend].
 *
 * This allows [MtcRetriever] to search the Knowledge Core instead of
 * legacy tables, without changing the retriever's interface or the R2 gate.
 *
 * The adapter converts Knowledge Core entities to RetrievedArticle format,
 * preserving provenance information for the UI.
 *
 * Migration path:
 * ```text
 * BEFORE: MtcRetriever → FtsSearchService → article_fts + biblioteca_nodes
 * AFTER:  MtcRetriever → KnowledgeCoreSearchBackend → Knowledge Core
 * ```
 */
class KnowledgeCoreSearchBackend(
    private val searchRepo: KnowledgeSearchRepository,
) : ArticleSearchBackend {

    override suspend fun search(query: String, maxResults: Int): List<RetrievedArticle> {
        if (query.isBlank()) return emptyList()

        val results = searchRepo.search(query, maxResults)
        AppLogger.d(TAG, "Knowledge Core search: '${query}' → ${results.size} results")

        return results.map { result ->
            RetrievedArticle(
                articleId = result.entity.id,
                title = result.entity.canonicalName,
                summary = result.entity.summary,
                content = result.entity.content,
                provenance = if (result.entity.version.status == KnowledgeStatus.PUBLISHED) {
                    Provenance.VERIFICAVEL
                } else {
                    Provenance.RASCUNHO
                },
            )
        }
    }

    companion object {
        private const val TAG = "KnowledgeCoreSearch"
    }
}
