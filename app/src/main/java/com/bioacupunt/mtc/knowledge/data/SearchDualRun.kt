package com.bioacupunt.mtc.knowledge.data

import com.bioacupunt.biblioteca.domain.search.ArticleSearchBackend
import com.bioacupunt.biblioteca.domain.search.RetrievedArticle
import com.bioacupunt.observability.AppLogger

/**
 * DUAL-RUN COMPARISON — runs both legacy and canonical search for the same
 * query and compares results.
 *
 * Used during Phase 2 migration to verify that the Knowledge Core produces
 * equivalent results to the legacy search before switching consumers.
 *
 * Produces a comparison report showing:
 * - Result count difference
 * - Top-N overlap
 * - Missing documents (in legacy but not in core)
 * - Core-only documents (in core but not in legacy)
 * - Ranking differences
 */
class SearchDualRun(
    private val legacyBackend: ArticleSearchBackend,
    private val coreBackend: ArticleSearchBackend,
) {

    data class ComparisonResult(
        val query: String,
        val legacyCount: Int,
        val coreCount: Int,
        val overlapCount: Int,
        val legacyOnlyIds: List<String>,
        val coreOnlyIds: List<String>,
        val overlapRatio: Double,
    )

    data class DualRunReport(
        val results: List<ComparisonResult>,
        val totalQueries: Int,
        val averageOverlapRatio: Double,
        val totalLegacyOnly: Int,
        val totalCoreOnly: Int,
    ) {
        fun format(): String = buildString {
            appendLine("=== DUAL-RUN SEARCH COMPARISON ===")
            appendLine("Queries tested: $totalQueries")
            appendLine("Average overlap: ${(averageOverlapRatio * 100).toInt()}%")
            appendLine("Legacy-only entities: $totalLegacyOnly")
            appendLine("Core-only entities: $totalCoreOnly")
            appendLine()
            results.forEach { r ->
                appendLine("Query: '${r.query}'")
                appendLine("  Legacy: ${r.legacyCount} | Core: ${r.coreCount} | Overlap: ${r.overlapCount}/${maxOf(r.legacyCount, r.coreCount, 1)} (${(r.overlapRatio * 100).toInt()}%)")
                if (r.legacyOnlyIds.isNotEmpty()) {
                    appendLine("  Legacy-only: ${r.legacyOnlyIds.take(5).joinToString(", ")}")
                }
                if (r.coreOnlyIds.isNotEmpty()) {
                    appendLine("  Core-only: ${r.coreOnlyIds.take(5).joinToString(", ")}")
                }
            }
            appendLine("=== END REPORT ===")
        }
    }

    /**
     * Run comparison for a single query.
     */
    suspend fun compare(query: String, limit: Int = 10): ComparisonResult {
        val legacyResults = legacyBackend.search(query, limit)
        val coreResults = coreBackend.search(query, limit)

        val legacyIds = legacyResults.map { it.articleId }.toSet()
        val coreIds = coreResults.map { it.articleId }.toSet()

        val overlap = legacyIds.intersect(coreIds)
        val legacyOnly = legacyIds.minus(coreIds).toList()
        val coreOnly = coreIds.minus(legacyIds).toList()

        val maxCount = maxOf(legacyIds.size, coreIds.size, 1)
        val overlapRatio = overlap.size.toDouble() / maxCount

        return ComparisonResult(
            query = query,
            legacyCount = legacyIds.size,
            coreCount = coreIds.size,
            overlapCount = overlap.size,
            legacyOnlyIds = legacyOnly,
            coreOnlyIds = coreOnly,
            overlapRatio = overlapRatio,
        )
    }

    /**
     * Run comparison for multiple queries and produce a summary report.
     */
    suspend fun runAll(queries: List<String>, limit: Int = 10): DualRunReport {
        val results = queries.map { query ->
            try {
                compare(query, limit)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Dual-run failed for query: $query", e)
                ComparisonResult(query, 0, 0, 0, emptyList(), emptyList(), 0.0)
            }
        }

        val totalQueries = results.size
        val averageOverlap = if (totalQueries > 0) {
            results.map { it.overlapRatio }.average()
        } else 0.0
        val totalLegacyOnly = results.sumOf { it.legacyOnlyIds.size }
        val totalCoreOnly = results.sumOf { it.coreOnlyIds.size }

        return DualRunReport(
            results = results,
            totalQueries = totalQueries,
            averageOverlapRatio = averageOverlap,
            totalLegacyOnly = totalLegacyOnly,
            totalCoreOnly = totalCoreOnly,
        )
    }

    companion object {
        private const val TAG = "SearchDualRun"

        /** MTC search regression dataset */
        val MTC_QUERIES = listOf(
            // Syndromes
            "Estagnação de Qi do Fígado",
            "Deficiência de Yin do Rim",
            "Síndrome do frio",
            // Symptoms
            "Insônia",
            "Dor lombar",
            "Cefaleia",
            "Ansiedade",
            // Points
            "LI4",
            "ST36",
            "SP6",
            "LV3",
            // Meridians
            "Meridiano do Pulmão",
            "Meridiano do Fígado",
            // Formulas
            "Xiao Yao San",
            "Liu Wei Di Huang Wan",
            // Herbs
            "Chai Hu",
            "Huang Qi",
            // Techniques
            "Moxabustão",
            "Eletroacupuntura",
            // Mixed
            "tratamento de dor",
            "ponto de acqua",
            "qi e sangue",
        )
    }
}
