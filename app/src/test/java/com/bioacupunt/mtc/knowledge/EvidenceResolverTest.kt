package com.bioacupunt.mtc.knowledge

import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreCitationEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreEvidenceEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreProvenanceEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreSourceEntity
import com.bioacupunt.mtc.knowledge.domain.EvidenceResolver
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for EvidenceResolver — the complete evidence chain:
 *
 * Evidence ID → KnowledgeEvidence → Citation IDs → KnowledgeCitation → KnowledgeSource → Provenance
 */
@RunWith(RobolectricTestRunner::class)
class EvidenceResolverTest {

    private lateinit var fakeDao: FakeKnowledgeCoreDao
    private lateinit var resolver: EvidenceResolver

    @Before
    fun setup() {
        fakeDao = FakeKnowledgeCoreDao()
        resolver = EvidenceResolver(fakeDao)
    }

    // ── Full chain ───────────────────────────────────────────────────

    @Test
    fun resolveEvidence_fullChain_resolvesAll() = runTest {
        fakeDao.sources.add(
            KnowledgeCoreSourceEntity(
                id = "src.1",
                name = "Maciocia - Foundations of Chinese Medicine",
                locator = "https://example.com",
                license = "MIT",
            )
        )
        fakeDao.citations.add(
            KnowledgeCoreCitationEntity(
                id = "cit.1",
                source_id = "src.1",
                locator = "p. 245",
                excerpt = "Liver Qi Stagnation presents with flank pain",
            )
        )
        fakeDao.evidence.add(
            KnowledgeCoreEvidenceEntity(
                id = "ev.1",
                claim = "Liver Qi Stagnation causes flank pain",
                level = "TRADITION",
                confidence = 0.85,
                citation_ids_json = "[\"cit.1\"]",
            )
        )

        val result = resolver.resolveEvidence("ev.1")

        assertNotNull("Should resolve evidence", result)
        assertEquals("ev.1", result!!.evidenceId)
        assertEquals("Liver Qi Stagnation causes flank pain", result.claim)
        assertEquals("TRADITION", result.level)
        assertEquals(0.85, result.confidence!!, 0.001)
        assertTrue("Should have citations", result.hasCitations)
        assertTrue("Should have sources", result.hasSources)
        assertEquals(1, result.citations.size)

        val citation = result.citations[0]
        assertEquals("cit.1", citation.citationId)
        assertEquals("src.1", citation.sourceId)
        assertEquals("Maciocia - Foundations of Chinese Medicine", citation.sourceName)
        assertEquals("p. 245", citation.locator)
        assertEquals("Liver Qi Stagnation presents with flank pain", citation.excerpt)
        assertEquals("MIT", citation.sourceLicense)
    }

    // ── Missing evidence ─────────────────────────────────────────────

    @Test
    fun resolveEvidence_missingEvidence_returnsNull() = runTest {
        val result = resolver.resolveEvidence("nonexistent")
        assertNull("Should return null for missing evidence", result)
    }

    // ── Missing citation ─────────────────────────────────────────────

    @Test
    fun resolveEvidence_missingCitation_returnsEmptyChain() = runTest {
        fakeDao.evidence.add(
            KnowledgeCoreEvidenceEntity(
                id = "ev.no.citation",
                claim = "Evidence without citations",
                level = "TRADITION",
                confidence = 0.5,
                citation_ids_json = "[]",
            )
        )

        val result = resolver.resolveEvidence("ev.no.citation")

        assertNotNull("Should resolve evidence even without citations", result)
        assertEquals("ev.no.citation", result!!.evidenceId)
        assertTrue("Citations should be empty", result.citations.isEmpty())
        assertFalse("Should NOT have citations", result.hasCitations)
        assertFalse("Should NOT have sources", result.hasSources)
    }

    // ── Missing source ───────────────────────────────────────────────

    @Test
    fun resolveEvidence_missingSource_citationStillResolved() = runTest {
        fakeDao.citations.add(
            KnowledgeCoreCitationEntity(
                id = "cit.no.source",
                source_id = "nonexistent.source",
                locator = "p. 100",
                excerpt = "Some excerpt",
            )
        )
        fakeDao.evidence.add(
            KnowledgeCoreEvidenceEntity(
                id = "ev.citation.no.source",
                claim = "Evidence with citation but no source",
                level = "TRADITION",
                confidence = 0.6,
                citation_ids_json = "[\"cit.no.source\"]",
            )
        )

        val result = resolver.resolveEvidence("ev.citation.no.source")

        assertNotNull("Should resolve evidence", result)
        assertEquals(1, result!!.citations.size)
        assertNull("Source name should be null", result.citations[0].sourceName)
        assertNull("Source license should be null", result.citations[0].sourceLicense)
        assertEquals("p. 100", result.citations[0].locator)
        assertEquals("Some excerpt", result.citations[0].excerpt)
    }

    // ── Multiple citations ───────────────────────────────────────────

    @Test
    fun resolveEvidence_multipleCitations_resolvesAll() = runTest {
        fakeDao.sources.add(KnowledgeCoreSourceEntity(id = "src.1", name = "Source 1", license = "MIT"))
        fakeDao.sources.add(KnowledgeCoreSourceEntity(id = "src.2", name = "Source 2", license = "Apache-2.0"))
        fakeDao.citations.addAll(listOf(
            KnowledgeCoreCitationEntity(id = "cit.1", source_id = "src.1", locator = "p. 10", excerpt = "First citation"),
            KnowledgeCoreCitationEntity(id = "cit.2", source_id = "src.2", locator = "doi:10.1234/test", excerpt = "Second citation"),
        ))
        fakeDao.evidence.add(
            KnowledgeCoreEvidenceEntity(
                id = "ev.multi",
                claim = "Multi-citation evidence",
                level = "CLINICAL_EVIDENCE",
                confidence = 0.9,
                citation_ids_json = "[\"cit.1\", \"cit.2\"]",
            )
        )

        val result = resolver.resolveEvidence("ev.multi")

        assertNotNull(result)
        assertEquals(2, result!!.citations.size)
        assertEquals(2, result.sourceCount)
        assertEquals("cit.1", result.citations[0].citationId)
        assertEquals("cit.2", result.citations[1].citationId)
    }

    // ── Multiple sources ─────────────────────────────────────────────

    @Test
    fun resolveEvidence_sameSourceMultipleCitations_sourceCountDistinct() = runTest {
        fakeDao.sources.add(KnowledgeCoreSourceEntity(id = "src.only", name = "Single Source", license = "MIT"))
        fakeDao.citations.addAll(listOf(
            KnowledgeCoreCitationEntity(id = "cit.a", source_id = "src.only", locator = "p. 100", excerpt = "First"),
            KnowledgeCoreCitationEntity(id = "cit.b", source_id = "src.only", locator = "p. 200", excerpt = "Second"),
        ))
        fakeDao.evidence.add(
            KnowledgeCoreEvidenceEntity(
                id = "ev.same.src",
                claim = "Multiple citations from same source",
                level = "TRADITION",
                confidence = 0.7,
                citation_ids_json = "[\"cit.a\", \"cit.b\"]",
            )
        )

        val result = resolver.resolveEvidence("ev.same.src")

        assertNotNull(result)
        assertEquals(2, result!!.citations.size)
        assertEquals(1, result.sourceCount)
    }

    // ── Batch resolution ─────────────────────────────────────────────

    @Test
    fun resolveEvidenceBatch_resolvesAndSorts() = runTest {
        fakeDao.evidence.addAll(listOf(
            KnowledgeCoreEvidenceEntity(id = "ev.low", claim = "Low", level = "TRADITION", confidence = 0.3, citation_ids_json = "[]"),
            KnowledgeCoreEvidenceEntity(id = "ev.high", claim = "High", level = "CLINICAL_EVIDENCE", confidence = 0.9, citation_ids_json = "[]"),
            KnowledgeCoreEvidenceEntity(id = "ev.mid", claim = "Mid", level = "MODERN_LITERATURE", confidence = 0.6, citation_ids_json = "[]"),
        ))

        val results = resolver.resolveEvidenceBatch(listOf("ev.low", "ev.high", "ev.mid"))

        assertEquals(3, results.size)
        assertEquals("ev.high", results[0].evidenceId)
        assertEquals("ev.mid", results[1].evidenceId)
        assertEquals("ev.low", results[2].evidenceId)
    }

    @Test
    fun resolveEvidenceBatch_missingEvidence_filteredOut() = runTest {
        fakeDao.evidence.add(
            KnowledgeCoreEvidenceEntity(id = "ev.exists", claim = "Exists", level = "TRADITION", confidence = 0.5, citation_ids_json = "[]")
        )

        val results = resolver.resolveEvidenceBatch(listOf("ev.exists", "ev.missing"))

        assertEquals(1, results.size)
        assertEquals("ev.exists", results[0].evidenceId)
    }

    // ── Provenance ───────────────────────────────────────────────────

    @Test
    fun resolveProvenance_returnsProvenanceChain() = runTest {
        fakeDao.provenance.add(
            KnowledgeCoreProvenanceEntity(
                entity_id = "entity.1",
                original_source = "TCM_knowledge_graph",
                original_id = "ext.pattern.123",
                original_type = "Pattern",
                source_reference = "github.com/AI-HPC-Research-Team/TCM_knowledge_graph",
                migration_version = "1.0",
                imported_at = System.currentTimeMillis(),
            )
        )

        val provenance = resolver.resolveProvenance("entity.1")

        assertEquals(1, provenance.size)
        assertEquals("entity.1", provenance[0].entityId)
        assertEquals("TCM_knowledge_graph", provenance[0].originalSource)
        assertEquals("ext.pattern.123", provenance[0].originalId)
        assertEquals("Pattern", provenance[0].originalType)
        assertEquals("1.0", provenance[0].migrationVersion)
    }

    @Test
    fun resolveProvenance_noProvenance_returnsEmpty() = runTest {
        val provenance = resolver.resolveProvenance("entity.nonexistent")
        assertTrue("Should return empty for missing provenance", provenance.isEmpty())
    }

    // ── Source quality bonus ─────────────────────────────────────────

    @Test
    fun getSourceQualityBonus_mitLicense_returnsHigherBonus() {
        val bonus = resolver.getSourceQualityBonus("Source", "MIT")
        assertTrue("MIT bonus should be positive", bonus > 0.10)
    }

    @Test
    fun getSourceQualityBonus_apacheLicense_returnsHigherBonus() {
        val bonus = resolver.getSourceQualityBonus("Source", "Apache-2.0")
        assertTrue("Apache bonus should be positive", bonus > 0.10)
    }

    @Test
    fun getSourceQualityBonus_noLicense_returnsLowBonus() {
        val bonus = resolver.getSourceQualityBonus("Source", null)
        assertEquals(0.05, bonus, 0.001)
    }

    @Test
    fun getSourceQualityBonus_noSource_returnsZero() {
        val bonus = resolver.getSourceQualityBonus(null, null)
        assertEquals(0.0, bonus, 0.001)
    }

    // ── Partial chain edge cases ─────────────────────────────────────

    @Test
    fun resolveEvidence_emptyCitationIds_handlesCorrectly() = runTest {
        fakeDao.evidence.add(
            KnowledgeCoreEvidenceEntity(id = "ev.empty.json", claim = "Empty", level = "TRADITION", confidence = 0.4, citation_ids_json = "")
        )

        val result = resolver.resolveEvidence("ev.empty.json")

        assertNotNull("Should handle empty citation JSON", result)
        assertTrue(result!!.citations.isEmpty())
    }

    @Test
    fun resolveEvidence_malformedJson_handlesGracefully() = runTest {
        fakeDao.evidence.add(
            KnowledgeCoreEvidenceEntity(id = "ev.bad.json", claim = "Malformed", level = "TRADITION", confidence = 0.4, citation_ids_json = "not-valid-json")
        )

        val result = resolver.resolveEvidence("ev.bad.json")

        assertNotNull("Should handle malformed JSON gracefully", result)
        assertTrue(result!!.citations.isEmpty())
    }

    // ── Complete chain with provenance ───────────────────────────────

    @Test
    fun resolveEvidence_fullChainWithProvenance() = runTest {
        fakeDao.sources.add(KnowledgeCoreSourceEntity(id = "src.complete", name = "Complete Source", license = "CC-BY-4.0"))
        fakeDao.citations.add(KnowledgeCoreCitationEntity(id = "cit.complete", source_id = "src.complete", locator = "doi:10.1234/complete", excerpt = "Complete chain excerpt"))
        fakeDao.evidence.add(KnowledgeCoreEvidenceEntity(id = "ev.complete", claim = "Complete chain claim", level = "CLINICAL_EVIDENCE", confidence = 0.95, citation_ids_json = "[\"cit.complete\"]"))
        fakeDao.provenance.add(KnowledgeCoreProvenanceEntity(entity_id = "entity.from.migration", original_source = "LegacyImporter", original_id = "legacy.pattern.1", original_type = "Pattern", source_reference = "bioacupunt/assets/packs", migration_version = "2.0", imported_at = System.currentTimeMillis()))

        val evidence = resolver.resolveEvidence("ev.complete")
        assertNotNull(evidence)
        assertTrue(evidence!!.hasCitations)
        assertTrue(evidence.hasSources)
        assertEquals("CC-BY-4.0", evidence.citations[0].sourceLicense)

        val provenance = resolver.resolveProvenance("entity.from.migration")
        assertEquals(1, provenance.size)
        assertEquals("LegacyImporter", provenance[0].originalSource)
    }
}
