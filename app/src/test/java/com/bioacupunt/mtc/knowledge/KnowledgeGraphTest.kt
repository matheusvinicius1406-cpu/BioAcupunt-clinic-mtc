package com.bioacupunt.mtc.knowledge

import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreDao
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreEntityEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreRelationEntity
import com.bioacupunt.mtc.knowledge.domain.*
import com.bioacupunt.mtc.knowledge.repository.GraphConfig
import com.bioacupunt.mtc.knowledge.repository.RoomKnowledgeGraphRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
/**
 * Tests for Knowledge Graph traversal.
 *
 * Uses a controlled graph with 3 scenarios:
 * Scenario A: Linear chain (A → B → C → D)
 * Scenario B: Fan-out (A → B, A → C, A → D)
 * Scenario C: Cycle (A → B → C → A)
 */
class KnowledgeGraphTest {

    private lateinit var fakeDao: FakeKnowledgeCoreDao
    private lateinit var repo: RoomKnowledgeGraphRepository

    @Before
    fun setup() {
        fakeDao = FakeKnowledgeCoreDao()
        repo = RoomKnowledgeGraphRepository(fakeDao)
    }

    // ── Scenario A: Linear chain ─────────────────────────────────────

    /**
     * Graph: PATTERN_A → SYMPTOM_B → ACUPOINT_C → FORMULA_D
     */
    private fun buildLinearChain() {
        fakeDao.entities.addAll(listOf(
            entity("pattern.a", "PATTERN", "Estagnação de Qi"),
            entity("symptom.b", "SYMPTOM", "Dor no flanco"),
            entity("acupoint.c", "ACUPOINT", "LI4"),
            entity("formula.d", "FORMULA", "Xiao Yao San"),
        ))
        fakeDao.relations.addAll(listOf(
            relation("pattern.a", "HAS_SYMPTOM", "symptom.b"),
            relation("symptom.b", "TREATED_BY", "acupoint.c"),
            relation("acupoint.c", "CONTAINS", "formula.d"),
        ))
    }

    @Test
    fun neighbors_depth1_returnsDirectNeighbors() = runTest {
        buildLinearChain()
        val result = repo.neighbors("pattern.a")
        assertEquals(setOf("pattern.a", "symptom.b"), result.visitedEntities.toSet())
        assertEquals(1, result.relations.size)
        assertEquals("symptom.b", result.relations[0].targetId)
    }

    @Test
    fun reachable_depth2_findsTwoHops() = runTest {
        buildLinearChain()
        val result = repo.reachable("pattern.a", GraphConfig(maxDepth = 2))
        assertEquals(setOf("pattern.a", "symptom.b", "acupoint.c"), result.visitedEntities.toSet())
    }

    @Test
    fun reachable_depth3_findsThreeHops() = runTest {
        buildLinearChain()
        val result = repo.reachable("pattern.a", GraphConfig(maxDepth = 3))
        assertEquals(4, result.visitedEntities.size)
    }

    @Test
    fun reachable_maxNodes_limitsOutput() = runTest {
        buildLinearChain()
        val result = repo.reachable("pattern.a", GraphConfig(maxDepth = 10, maxNodes = 2))
        assertTrue(result.visitedEntities.size <= 2)
    }

    // ── Scenario B: Fan-out ──────────────────────────────────────────

    /**
     * Graph: PATTERN_A → {SYMPTOM_B, ACUPOINT_C, FORMULA_D}
     */
    private fun buildFanOut() {
        fakeDao.entities.addAll(listOf(
            entity("pattern.a", "PATTERN", "Deficiência de Qi"),
            entity("symptom.b", "SYMPTOM", "Fadiga"),
            entity("acupoint.c", "ACUPOINT", "ST36"),
            entity("formula.d", "FORMULA", "Si Jun Zi Tang"),
        ))
        fakeDao.relations.addAll(listOf(
            relation("pattern.a", "HAS_SYMPTOM", "symptom.b"),
            relation("pattern.a", "HAS_POINT", "acupoint.c"),
            relation("pattern.a", "HAS_FORMULA", "formula.d"),
        ))
    }

    @Test
    fun neighbors_fanOut_returnsAllDirectNeighbors() = runTest {
        buildFanOut()
        val result = repo.neighbors("pattern.a")
        assertEquals(4, result.visitedEntities.size) // root + 3 neighbors
        assertEquals(3, result.relations.size)
    }

    @Test
    fun reachable_fanOut_allReachable() = runTest {
        buildFanOut()
        val result = repo.reachable("pattern.a", GraphConfig(maxDepth = 2))
        assertEquals(4, result.visitedEntities.size)
    }

    // ── Scenario C: Cycle ────────────────────────────────────────────

    /**
     * Graph: PATTERN_A → SYMPTOM_B → ACUPOINT_C → PATTERN_A (cycle)
     */
    private fun buildCycle() {
        fakeDao.entities.addAll(listOf(
            entity("pattern.a", "PATTERN", "Padrão A"),
            entity("symptom.b", "SYMPTOM", "Sintoma B"),
            entity("acupoint.c", "ACUPOINT", "Ponto C"),
        ))
        fakeDao.relations.addAll(listOf(
            relation("pattern.a", "HAS_SYMPTOM", "symptom.b"),
            relation("symptom.b", "TREATED_BY", "acupoint.c"),
            relation("acupoint.c", "ASSOCIATED_WITH", "pattern.a"), // cycle!
        ))
    }

    @Test
    fun reachable_cycle_doesNotInfiniteLoop() = runTest {
        buildCycle()
        val result = repo.reachable("pattern.a", GraphConfig(maxDepth = 10))
        // Should terminate with 3 visited entities, not infinite loop
        assertEquals(3, result.visitedEntities.size)
    }

    @Test
    fun reachable_cycle_visitedSetPreventsRevisiting() = runTest {
        buildCycle()
        val result = repo.reachable("pattern.a", GraphConfig(maxDepth = 10, maxNodes = 100))
        // Each entity should appear exactly once
        assertEquals(result.visitedEntities.size, result.visitedEntities.toSet().size)
    }

    // ── findPath ─────────────────────────────────────────────────────

    @Test
    fun findPath_linearChain_findsPath() = runTest {
        buildLinearChain()
        val paths = repo.findPath("pattern.a", "formula.d")
        assertEquals(1, paths.size)
        assertEquals(listOf("pattern.a", "symptom.b", "acupoint.c", "formula.d"), paths[0].entityIds)
    }

    @Test
    fun findPath_noPath_returnsEmpty() = runTest {
        fakeDao.entities.addAll(listOf(
            entity("a", "PATTERN", "A"),
            entity("b", "PATTERN", "B"),
        ))
        val paths = repo.findPath("a", "b")
        assertTrue(paths.isEmpty())
    }

    @Test
    fun findPath_sameNode_returnsSingleNodePath() = runTest {
        fakeDao.entities.add(entity("a", "PATTERN", "A"))
        val paths = repo.findPath("a", "a")
        assertEquals(1, paths.size)
        assertEquals(listOf("a"), paths[0].entityIds)
    }

    // ── Missing entity ───────────────────────────────────────────────

    @Test
    fun neighbors_missingEntity_returnsEmpty() = runTest {
        val result = repo.neighbors("nonexistent")
        assertTrue(result.visitedEntities.isEmpty())
        assertTrue(result.relations.isEmpty())
    }

    // ── Relation type filter ─────────────────────────────────────────

    @Test
    fun reachable_relationTypeFilter_onlyMatchingRelations() = runTest {
        buildLinearChain()
        val result = repo.reachable("pattern.a", GraphConfig(
            maxDepth = 3,
            relationTypes = setOf(KnowledgeRelationType.HAS_SYMPTOM),
        ))
        // Only follows HAS_SYMPTOM edges
        assertTrue(result.relations.all { it.relationType == KnowledgeRelationType.HAS_SYMPTOM })
    }

    // ── Confidence filter ────────────────────────────────────────────

    @Test
    fun reachable_confidenceFilter_onlyHighConfidence() = runTest {
        fakeDao.entities.addAll(listOf(
            entity("a", "PATTERN", "A"),
            entity("b", "SYMPTOM", "B"),
            entity("c", "SYMPTOM", "C"),
        ))
        fakeDao.relations.addAll(listOf(
            relation("a", "HAS_SYMPTOM", "b").copy(confidence = 0.9),
            relation("a", "HAS_SYMPTOM", "c").copy(confidence = 0.3),
        ))
        val result = repo.reachable("a", GraphConfig(
            maxDepth = 2,
            minConfidence = 0.5,
        ))
        // Only edge with confidence >= 0.5 should be traversed
        assertTrue(result.relations.all { (it.confidence ?: 0.0) >= 0.5 })
    }

    // ── Deterministic ordering ───────────────────────────────────────

    @Test
    fun reachable_deterministicOrdering() = runTest {
        // Add entities in random order
        fakeDao.entities.addAll(listOf(
            entity("z", "SYMPTOM", "Z Symptom"),
            entity("a", "SYMPTOM", "A Symptom"),
            entity("m", "SYMPTOM", "M Symptom"),
        ))
        fakeDao.relations.addAll(listOf(
            relation("root", "HAS_SYMPTOM", "z"),
            relation("root", "HAS_SYMPTOM", "a"),
            relation("root", "HAS_SYMPTOM", "m"),
        ))
        fakeDao.entities.add(entity("root", "PATTERN", "Root Pattern"))

        val result1 = repo.reachable("root", GraphConfig(maxDepth = 2))
        val result2 = repo.reachable("root", GraphConfig(maxDepth = 2))
        assertEquals(result1.visitedEntities, result2.visitedEntities)
    }

    // ── edgesFrom / edgesTo ──────────────────────────────────────────

    @Test
    fun edgesFrom_returnsOutgoingOnly() = runTest {
        buildLinearChain()
        val edges = repo.edgesFrom("pattern.a")
        assertEquals(1, edges.size)
        assertEquals("symptom.b", edges[0].targetId)
    }

    @Test
    fun edgesTo_returnsIncomingOnly() = runTest {
        buildLinearChain()
        val edges = repo.edgesTo("symptom.b")
        assertEquals(1, edges.size)
        assertEquals("pattern.a", edges[0].sourceId)
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private fun entity(id: String, type: String, name: String) = KnowledgeCoreEntityEntity(
        id = id, type = type, canonical_name = name,
        created_at = System.currentTimeMillis(), updated_at = System.currentTimeMillis(),
    )

    private fun relation(source: String, type: String, target: String, confidence: Double? = null) =
        KnowledgeCoreRelationEntity(
            source_entity_id = source, relation_type = type, target_entity_id = target,
            confidence = confidence, created_at = System.currentTimeMillis(), updated_at = System.currentTimeMillis(),
        )
}

// ── Fake DAO ──────────────────────────────────────────────────────────

class FakeKnowledgeCoreDao : KnowledgeCoreDao {
    val entities = mutableListOf<KnowledgeCoreEntityEntity>()
    val relations = mutableListOf<KnowledgeCoreRelationEntity>()
    val sources = mutableListOf<com.bioacupunt.mtc.knowledge.data.KnowledgeCoreSourceEntity>()
    val citations = mutableListOf<com.bioacupunt.mtc.knowledge.data.KnowledgeCoreCitationEntity>()
    val evidence = mutableListOf<com.bioacupunt.mtc.knowledge.data.KnowledgeCoreEvidenceEntity>()
    val provenance = mutableListOf<com.bioacupunt.mtc.knowledge.data.KnowledgeCoreProvenanceEntity>()

    override suspend fun getById(id: String) = entities.find { it.id == id }
    override suspend fun search(query: String, limit: Int) = entities
        .filter { it.canonical_name.contains(query, ignoreCase = true) }
        .take(limit)
    override fun observeAll() = flowOf(entities.toList())
    override suspend fun getRelations(entityId: String) = relations
        .filter { it.source_entity_id == entityId || it.target_entity_id == entityId }
    override suspend fun getEdgesFrom(entityId: String) = relations.filter { it.source_entity_id == entityId }
    override suspend fun getEdgesTo(entityId: String) = relations.filter { it.target_entity_id == entityId }
    override suspend fun getEdgesBetween(sourceId: String, targetId: String) = relations.filter { it.source_entity_id == sourceId && it.target_entity_id == targetId }
    override suspend fun getByType(type: String) = entities.filter { it.type == type }
    override suspend fun getByStatus(status: String) = entities.filter { it.status == status }
    override suspend fun getByIds(ids: List<String>) = entities.filter { it.id in ids }
    override suspend fun countAll() = entities.size
    override suspend fun countByType(type: String) = entities.count { it.type == type }
    override suspend fun deleteById(id: String) { entities.removeAll { it.id == id } }
    override suspend fun deleteRelationsFor(entityId: String) {
        relations.removeAll { it.source_entity_id == entityId || it.target_entity_id == entityId }
    }
    override suspend fun insertEntities(items: List<KnowledgeCoreEntityEntity>) {
        items.forEach { item ->
            entities.removeAll { it.id == item.id }
            entities.add(item)
        }
    }
    override suspend fun insertRelations(items: List<KnowledgeCoreRelationEntity>) { relations.addAll(items) }
    override suspend fun insertSources(items: List<com.bioacupunt.mtc.knowledge.data.KnowledgeCoreSourceEntity>) { sources.addAll(items) }
    override suspend fun insertCitations(items: List<com.bioacupunt.mtc.knowledge.data.KnowledgeCoreCitationEntity>) { citations.addAll(items) }
    override suspend fun insertEvidence(items: List<com.bioacupunt.mtc.knowledge.data.KnowledgeCoreEvidenceEntity>) { evidence.addAll(items) }
    override suspend fun insertProvenance(items: List<com.bioacupunt.mtc.knowledge.data.KnowledgeCoreProvenanceEntity>) { provenance.addAll(items) }
    override suspend fun getEvidenceById(id: String) = evidence.find { it.id == id }
    override suspend fun getEvidenceByIds(ids: List<String>) = evidence.filter { it.id in ids }
    override suspend fun getCitationById(id: String) = citations.find { it.id == id }
    override suspend fun getCitationsByIds(ids: List<String>) = citations.filter { it.id in ids }
    override suspend fun getCitationsBySource(sourceId: String) = citations.filter { it.source_id == sourceId }
    override suspend fun getSourceById(id: String) = sources.find { it.id == id }
    override suspend fun getSourcesByIds(ids: List<String>) = sources.filter { it.id in ids }
    override suspend fun getProvenanceByEntity(entityId: String) = provenance.filter { it.entity_id == entityId }
}
