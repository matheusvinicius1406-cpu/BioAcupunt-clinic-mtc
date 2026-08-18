package com.bioacupunt.mtc.knowledge

import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreCitationEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreDao
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreEntityEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreEvidenceEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreProvenanceEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreRelationEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreSourceEntity
import com.bioacupunt.mtc.knowledge.domain.*
import com.bioacupunt.mtc.knowledge.repository.KnowledgeCoreImporter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KnowledgeImporterTest {

    private lateinit var fakeDao: FakeKnowledgeCoreDao
    private lateinit var importer: KnowledgeCoreImporter

    @Before
    fun setup() {
        fakeDao = FakeKnowledgeCoreDao()
        importer = KnowledgeCoreImporter(fakeDao)
    }

    @Test
    fun import_singleEntity_persistsAll() = runBlocking {
        val now = 1L
        val imports = listOf(
            KnowledgeImport(
                entity = KnowledgeEntity(
                    id = "pattern.test",
                    type = KnowledgeEntityType.PATTERN,
                    canonicalName = "Test Pattern",
                    aliases = listOf("Alias1"),
                    summary = "Summary",
                    content = "Content",
                    metadata = mapOf("key" to "value"),
                    sourceIds = listOf("s1"),
                    citationIds = listOf("c1"),
                    evidenceIds = listOf("e1"),
                    version = KnowledgeVersion("1.0", now, now, status = KnowledgeStatus.PUBLISHED),
                    provenance = listOf(KnowledgeProvenance("library", "art1", "article", null, "v1", now)),
                    createdAt = now,
                    updatedAt = now,
                ),
                sources = listOf(KnowledgeSource("s1", "Source 1")),
                citations = listOf(KnowledgeCitation("c1", "s1")),
                evidence = listOf(KnowledgeEvidence("e1", "Claim 1", listOf("c1"), "MODERN", 0.9)),
            )
        )

        val result = importer.import(imports)

        assertEquals(1, result.entities.size)
        assertEquals(0, result.conflicts.size)
        assertEquals(0, result.duplicateCount) // 1 import → 1 entity, 0 duplicates

        // Verify all tables populated
        assertEquals(1, fakeDao.entities.size)
        assertEquals(1, fakeDao.sources.size)
        assertEquals(1, fakeDao.citations.size)
        assertEquals(1, fakeDao.evidence.size)
        assertEquals(1, fakeDao.provenance.size)

        // Verify entity fields
        val entity = fakeDao.entities[0]
        assertEquals("pattern.test", entity.id)
        assertEquals("PATTERN", entity.type)
        assertEquals("Test Pattern", entity.canonical_name)
        assertEquals("Summary", entity.summary)
        assertEquals("Content", entity.content)
        assertEquals("PUBLISHED", entity.status)

        // Verify metadata preserved
        assertTrue(entity.metadata_json.contains("key"))
        assertTrue(entity.metadata_json.contains("value"))

        // Verify evidence level preserved
        assertEquals("MODERN", fakeDao.evidence[0].level)
        assertEquals(0.9, fakeDao.evidence[0].confidence!!, 0.001)
    }

    @Test
    fun import_duplicateEntities_areMerged() = runBlocking {
        val now = 1L
        val imports = listOf(
            KnowledgeImport(
                entity = KnowledgeEntity(
                    id = "pattern.x",
                    type = KnowledgeEntityType.PATTERN,
                    canonicalName = "Padrão X",
                    content = "Content A",
                    version = KnowledgeVersion("1", now, now),
                    provenance = listOf(KnowledgeProvenance("library", "a", "pattern", null, "v1", now)),
                    createdAt = now, updatedAt = now,
                ),
            ),
            KnowledgeImport(
                entity = KnowledgeEntity(
                    id = "pattern.x",
                    type = KnowledgeEntityType.PATTERN,
                    canonicalName = "Padrão X",
                    content = "Content A",
                    version = KnowledgeVersion("1", now, now),
                    provenance = listOf(KnowledgeProvenance("mkis", "b", "pattern", null, "v1", now)),
                    createdAt = now, updatedAt = now,
                ),
            ),
        )

        val result = importer.import(imports)

        // Should merge into 1 entity with 2 provenance records
        assertEquals(1, result.entities.size)
        assertEquals(1, result.duplicateCount)
        assertEquals(2, result.entities[0].provenance.size)
    }

    @Test
    fun import_conflictingContent_createsConflict() = runBlocking {
        val now = 1L
        val imports = listOf(
            KnowledgeImport(
                entity = KnowledgeEntity(
                    id = "pattern.y",
                    type = KnowledgeEntityType.PATTERN,
                    canonicalName = "Padrão Y",
                    content = "Content from library",
                    version = KnowledgeVersion("1", now, now),
                    createdAt = now, updatedAt = now,
                ),
            ),
            KnowledgeImport(
                entity = KnowledgeEntity(
                    id = "pattern.y",
                    type = KnowledgeEntityType.PATTERN,
                    canonicalName = "Padrão Y",
                    content = "Content from MKIS (different!)",
                    version = KnowledgeVersion("1", now, now),
                    createdAt = now, updatedAt = now,
                ),
            ),
        )

        val result = importer.import(imports)

        assertEquals(1, result.entities.size)
        assertEquals(1, result.conflicts.size)
        assertEquals("conteúdo divergente entre fontes", result.conflicts[0].reason)
    }

    @Test
    fun import_idempotent_noDuplicatesOnSecondRun() = runBlocking {
        val now = 1L
        val imports = listOf(
            KnowledgeImport(
                entity = KnowledgeEntity(
                    id = "pattern.z",
                    type = KnowledgeEntityType.PATTERN,
                    canonicalName = "Padrão Z",
                    version = KnowledgeVersion("1", now, now),
                    createdAt = now, updatedAt = now,
                ),
            )
        )

        // First import
        val result1 = importer.import(imports)
        assertEquals(1, fakeDao.entities.size)

        // Second import (idempotent — REPLACE strategy)
        val result2 = importer.import(imports)
        assertEquals(1, fakeDao.entities.size) // Still 1 entity, not 2
    }

    @Test
    fun import_multipleTypes_allPersisted() = runBlocking {
        val now = 1L
        val imports = listOf(
            KnowledgeImport(entity = KnowledgeEntity("p1", KnowledgeEntityType.PATTERN, "P1", version = KnowledgeVersion("1", now, now), createdAt = now, updatedAt = now)),
            KnowledgeImport(entity = KnowledgeEntity("s1", KnowledgeEntityType.SYMPTOM, "S1", version = KnowledgeVersion("1", now, now), createdAt = now, updatedAt = now)),
            KnowledgeImport(entity = KnowledgeEntity("a1", KnowledgeEntityType.ACUPOINT, "A1", version = KnowledgeVersion("1", now, now), createdAt = now, updatedAt = now)),
            KnowledgeImport(entity = KnowledgeEntity("f1", KnowledgeEntityType.FORMULA, "F1", version = KnowledgeVersion("1", now, now), createdAt = now, updatedAt = now)),
        )

        val result = importer.import(imports)

        assertEquals(4, result.entities.size)
        assertEquals(4, fakeDao.entities.size)
    }

    // ── Fake DAO ──────────────────────────────────────────────────────

    class FakeKnowledgeCoreDao : KnowledgeCoreDao {
        val entities = mutableListOf<KnowledgeCoreEntityEntity>()
        val relations = mutableListOf<KnowledgeCoreRelationEntity>()
        val sources = mutableListOf<KnowledgeCoreSourceEntity>()
        val citations = mutableListOf<KnowledgeCoreCitationEntity>()
        val evidence = mutableListOf<KnowledgeCoreEvidenceEntity>()
        val provenance = mutableListOf<KnowledgeCoreProvenanceEntity>()

        override suspend fun getById(id: String) = entities.find { it.id == id }
        override suspend fun search(query: String, limit: Int) = entities
            .filter { it.canonical_name.contains(query, ignoreCase = true) }
            .take(limit)
        override fun observeAll() = flowOf(entities.toList())
        override suspend fun getRelations(entityId: String) = relations
            .filter { it.source_entity_id == entityId || it.target_entity_id == entityId }
        override suspend fun getEdgesFrom(entityId: String) = relations.filter { it.source_entity_id == entityId }
        override suspend fun getEdgesTo(entityId: String) = relations.filter { it.target_entity_id == entityId }
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
        override suspend fun insertSources(items: List<KnowledgeCoreSourceEntity>) { sources.addAll(items) }
        override suspend fun insertCitations(items: List<KnowledgeCoreCitationEntity>) { citations.addAll(items) }
        override suspend fun insertEvidence(items: List<KnowledgeCoreEvidenceEntity>) { evidence.addAll(items) }
        override suspend fun insertProvenance(items: List<KnowledgeCoreProvenanceEntity>) { provenance.addAll(items) }
    }
}
