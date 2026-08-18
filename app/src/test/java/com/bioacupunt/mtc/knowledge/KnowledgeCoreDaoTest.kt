package com.bioacupunt.mtc.knowledge

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bioacupunt.data.local.database.AppDatabase
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreDao
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreEntityEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KnowledgeCoreDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: KnowledgeCoreDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.knowledgeCoreDao()
    }

    @After
    fun teardown() { db.close() }

    private fun entity(
        id: String = "test-1",
        type: String = "PATTERN",
        name: String = "Test Pattern",
        summary: String = "",
        content: String = "",
        status: String = "DRAFT",
    ) = KnowledgeCoreEntityEntity(
        id = id, type = type, canonical_name = name,
        aliases_json = "[]", summary = summary, content = content,
        metadata_json = "{}", source_ids_json = "[]",
        citation_ids_json = "[]", evidence_ids_json = "[]",
        version = "1.0", status = status,
        created_at = System.currentTimeMillis(),
        updated_at = System.currentTimeMillis(),
    )

    // ── CRUD ──────────────────────────────────────────────────────────

    @Test
    fun insertAndGetById() = runBlocking {
        val e = entity(id = "p1", name = "Pattern 1")
        dao.insertEntities(listOf(e))

        val found = dao.getById("p1")
        assertNotNull(found)
        assertEquals("Pattern 1", found!!.canonical_name)
    }

    @Test
    fun getById_returnsNullForMissing() = runBlocking {
        assertNull(dao.getById("nonexistent"))
    }

    @Test
    fun observeAll_returnsAll() = runBlocking {
        dao.insertEntities(listOf(entity("a", name = "A"), entity("b", name = "B")))

        val all = dao.observeAll().first()
        assertEquals(2, all.size)
    }

    // ── Search ────────────────────────────────────────────────────────

    @Test
    fun search_findsByName() = runBlocking {
        dao.insertEntities(listOf(
            entity("p1", name = "Estagnação de Qi"),
            entity("p2", name = "Deficiência de Sangue"),
        ))

        val results = dao.search("Qi", 10)
        assertEquals(1, results.size)
        assertEquals("Estagnação de Qi", results[0].canonical_name)
    }

    @Test
    fun search_findsByContent() = runBlocking {
        dao.insertEntities(listOf(
            entity("p1", name = "Pattern 1", content = "Conteúdo sobre fígado"),
        ))

        val results = dao.search("fígado", 10)
        assertEquals(1, results.size)
    }

    @Test
    fun search_respectsLimit() = runBlocking {
        dao.insertEntities(listOf(
            entity("a", name = "Alpha"),
            entity("b", name = "Alpha 2"),
            entity("c", name = "Alpha 3"),
        ))

        val results = dao.search("Alpha", 2)
        assertEquals(2, results.size)
    }

    // ── Filter by type/status ─────────────────────────────────────────

    @Test
    fun getByType_filtersCorrectly() = runBlocking {
        dao.insertEntities(listOf(
            entity("p1", type = "PATTERN", name = "P1"),
            entity("s1", type = "SYMPTOM", name = "S1"),
            entity("p2", type = "PATTERN", name = "P2"),
        ))

        val patterns = dao.getByType("PATTERN")
        assertEquals(2, patterns.size)
    }

    @Test
    fun getByStatus_filtersCorrectly() = runBlocking {
        dao.insertEntities(listOf(
            entity("a", status = "PUBLISHED"),
            entity("b", status = "DRAFT"),
            entity("c", status = "PUBLISHED"),
        ))

        val published = dao.getByStatus("PUBLISHED")
        assertEquals(2, published.size)
    }

    // ── Count ─────────────────────────────────────────────────────────

    @Test
    fun countAll_returnsCorrectCount() = runBlocking {
        dao.insertEntities(listOf(entity("a"), entity("b"), entity("c")))
        assertEquals(3, dao.countAll())
    }

    @Test
    fun countByType_returnsCorrectCount() = runBlocking {
        dao.insertEntities(listOf(
            entity("p1", type = "PATTERN"),
            entity("p2", type = "PATTERN"),
            entity("s1", type = "SYMPTOM"),
        ))
        assertEquals(2, dao.countByType("PATTERN"))
        assertEquals(1, dao.countByType("SYMPTOM"))
    }

    // ── Delete ────────────────────────────────────────────────────────

    @Test
    fun deleteById_removesEntity() = runBlocking {
        dao.insertEntities(listOf(entity("a"), entity("b")))
        dao.deleteById("a")

        assertNull(dao.getById("a"))
        assertNotNull(dao.getById("b"))
        assertEquals(1, dao.countAll())
    }

    // ── Relations ─────────────────────────────────────────────────────

    @Test
    fun insertAndGetRelations() = runBlocking {
        val rel = com.bioacupunt.mtc.knowledge.data.KnowledgeCoreRelationEntity(
            source_entity_id = "p1",
            relation_type = "TREATED_BY",
            target_entity_id = "a1",
            evidence_ids_json = "[]",
            confidence = 0.9,
            provenance_json = "[]",
            created_at = 1L,
            updated_at = 1L,
        )
        dao.insertRelations(listOf(rel))

        val found = dao.getRelations("p1")
        assertEquals(1, found.size)
        assertEquals("TREATED_BY", found[0].relation_type)
    }

    @Test
    fun getEdgesFrom_filtersCorrectly() = runBlocking {
        dao.insertRelations(listOf(
            com.bioacupunt.mtc.knowledge.data.KnowledgeCoreRelationEntity("p1", "TREATED_BY", "a1", "[]", null, "[]", 1L, 1L),
            com.bioacupunt.mtc.knowledge.data.KnowledgeCoreRelationEntity("p2", "ASSOCIATED_WITH", "a1", "[]", null, "[]", 1L, 1L),
        ))

        val fromP1 = dao.getEdgesFrom("p1")
        assertEquals(1, fromP1.size)
        assertEquals("a1", fromP1[0].target_entity_id)
    }

    @Test
    fun deleteRelationsFor_removesAll() = runBlocking {
        dao.insertRelations(listOf(
            com.bioacupunt.mtc.knowledge.data.KnowledgeCoreRelationEntity("p1", "TREATED_BY", "a1", "[]", null, "[]", 1L, 1L),
            com.bioacupunt.mtc.knowledge.data.KnowledgeCoreRelationEntity("p1", "ASSOCIATED_WITH", "a2", "[]", null, "[]", 1L, 1L),
            com.bioacupunt.mtc.knowledge.data.KnowledgeCoreRelationEntity("p2", "TREATED_BY", "a1", "[]", null, "[]", 1L, 1L),
        ))

        dao.deleteRelationsFor("p1")

        assertEquals(0, dao.getRelations("p1").size)
        assertEquals(1, dao.getRelations("p2").size) // p2 untouched
    }

    // ── Batch operations ──────────────────────────────────────────────

    @Test
    fun getByIds_returnsMultiple() = runBlocking {
        dao.insertEntities(listOf(entity("a"), entity("b"), entity("c")))

        val found = dao.getByIds(listOf("a", "c"))
        assertEquals(2, found.size)
    }
}
