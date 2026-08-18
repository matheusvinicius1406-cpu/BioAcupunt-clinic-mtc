package com.bioacupunt.mtc.knowledge

import com.bioacupunt.biblioteca.domain.search.ArticleSearchBackend
import com.bioacupunt.biblioteca.domain.ingestion.Provenance
import com.bioacupunt.biblioteca.domain.search.RetrievedArticle
import com.bioacupunt.mtc.knowledge.data.SearchDualRun
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for SearchDualRun — verifies the dual-run comparison tool produces
 * correct overlap, missing, and core-only metrics.
 */
class SearchDualRunTest {

    /** Fake backend that returns pre-configured results. */
    class FakeBackend(private val resultsMap: Map<String, List<RetrievedArticle>>) : ArticleSearchBackend {
        override suspend fun search(query: String, limit: Int): List<RetrievedArticle> {
            return resultsMap[query.lowercase().trim()]?.take(limit) ?: emptyList()
        }
    }

    private lateinit var legacyBackend: FakeBackend
    private lateinit var coreBackend: FakeBackend
    private lateinit var dualRun: SearchDualRun

    @Before
    fun setup() {
        legacyBackend = FakeBackend(
            mapOf(
                "insonia" to listOf(
                    article("a1", "Artigo sobre insonia"),
                    article("a2", "Sono e MTC"),
                    article("a3", "Padroes de insonia"),
                ),
                "dor" to listOf(
                    article("b1", "Dor lombar"),
                    article("b2", "Dor cronica"),
                ),
            )
        )
        coreBackend = FakeBackend(
            mapOf(
                "insonia" to listOf(
                    article("a1", "Artigo sobre insonia"),
                    article("a3", "Padroes de insonia"),
                    article("a4", "Nove horas de sono"),
                ),
                "dor" to listOf(
                    article("b1", "Dor lombar"),
                    article("b3", "Dor aguda"),
                ),
            )
        )
        dualRun = SearchDualRun(legacyBackend, coreBackend)
    }

    @Test
    fun compare_findsCorrectOverlap() = runBlocking {
        val result = dualRun.compare("insonia", limit = 10)

        assertEquals(3, result.legacyCount)
        assertEquals(3, result.coreCount)
        assertEquals(2, result.overlapCount) // a1, a3 overlap
        assertEquals(setOf("a2"), result.legacyOnlyIds.toSet())
        assertEquals(setOf("a4"), result.coreOnlyIds.toSet())
    }

    @Test
    fun compare_noOverlap_returnsZero() = runBlocking {
        val noOverlapLegacy = FakeBackend(mapOf("x" to listOf(article("z1", "X"))))
        val noOverlapCore = FakeBackend(mapOf("x" to listOf(article("z2", "Y"))))
        val dr = SearchDualRun(noOverlapLegacy, noOverlapCore)

        val result = dr.compare("x")
        assertEquals(0, result.overlapCount)
        assertEquals(1, result.legacyOnlyIds.size)
        assertEquals(1, result.coreOnlyIds.size)
    }

    @Test
    fun compare_emptyQuery_returnsEmpty() = runBlocking {
        val result = dualRun.compare("nonexistent")
        assertEquals(0, result.legacyCount)
        assertEquals(0, result.coreCount)
        assertEquals(0, result.overlapCount)
    }

    @Test
    fun compare_identicalResults() = runBlocking {
        val identical = FakeBackend(
            mapOf("test" to listOf(article("c1", "Same")))
        )
        val dr = SearchDualRun(identical, identical)

        val result = dr.compare("test")
        assertEquals(1, result.overlapCount)
        assertTrue(result.legacyOnlyIds.isEmpty())
        assertTrue(result.coreOnlyIds.isEmpty())
        assertEquals(1.0, result.overlapRatio, 0.01)
    }

    @Test
    fun runAll_aggregatesCorrectly() = runBlocking {
        val queries = listOf("insonia", "dor")
        val report = dualRun.runAll(queries, limit = 10)

        assertEquals(2, report.totalQueries)
        assertEquals(2, report.results.size)

        // First query: overlap = 2/3 = 0.667
        // Second query: overlap = 1/2 = 0.5
        // Average: ~0.583
        assertTrue(report.averageOverlapRatio > 0.5)
        assertTrue(report.averageOverlapRatio < 0.7)

        // Legacy-only: a2 + b2 = 2
        assertEquals(2, report.totalLegacyOnly)
        // Core-only: a4 + b3 = 2
        assertEquals(2, report.totalCoreOnly)
    }

    @Test
    fun runAll_emptyQueries() = runBlocking {
        val report = dualRun.runAll(emptyList())
        assertEquals(0, report.totalQueries)
        assertEquals(0.0, report.averageOverlapRatio, 0.01)
    }

    @Test
    fun mtcQueries_hasMinimumQueries() {
        assertTrue(
            "MTC_QUERIES should have at least 32 queries",
            SearchDualRun.MTC_QUERIES.size >= 32
        )
    }

    @Test
    fun format_containsMetrics() = runBlocking {
        val report = dualRun.runAll(listOf("insonia"))
        val text = report.format()

        assertTrue(text.contains("DUAL-RUN SEARCH COMPARISON"))
        assertTrue(text.contains("Queries tested: 1"))
        assertTrue(text.contains("Average overlap:"))
    }

    private fun article(id: String, title: String) = RetrievedArticle(
        articleId = id,
        title = title,
        summary = "Summary of $title",
        content = "Content of $title",
        provenance = Provenance.VERIFICAVEL,
    )
}
