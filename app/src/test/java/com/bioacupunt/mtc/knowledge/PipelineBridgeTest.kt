package com.bioacupunt.mtc.knowledge

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.bioacupunt.data.local.database.AppDatabase
import com.bioacupunt.data.local.model.KnowledgeNodeEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreFtsSyncer
import com.bioacupunt.mtc.knowledge.data.MkisAdapter
import com.bioacupunt.mtc.knowledge.data.PipelineBridge
import com.bioacupunt.mtc.knowledge.repository.KnowledgeCoreImporter
import com.bioacupunt.mtc.knowledge.repository.KnowledgeSearchRepository
import com.bioacupunt.mtc.knowledge.repository.RoomKnowledgeSearchRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * INTEGRATION TEST — PipelineBridge writes a KnowledgeNodeEntity to Knowledge Core.
 *
 * Verifies:
 * 1. Entity appears in Knowledge Core after bridging
 * 2. Bridging same entity twice is idempotent (no duplicates)
 * 3. Content is preserved
 * 4. Bridged entity is findable via FTS search
 */
@RunWith(RobolectricTestRunner::class)
class PipelineBridgeTest {

    private lateinit var roomDb: AppDatabase
    private lateinit var db: SupportSQLiteDatabase
    private lateinit var searchRepo: KnowledgeSearchRepository
    private lateinit var importer: KnowledgeCoreImporter
    private lateinit var bridge: PipelineBridge
    private lateinit var ftsSyncer: KnowledgeCoreFtsSyncer

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = File(context.cacheDir, "test_pipeline_bridge.db")
        if (dbFile.exists()) dbFile.delete()

        roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration(false)
            .build()
        db = roomDb.openHelper.writableDatabase

        // Create FTS table (same as migration v26)
        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `knowledge_core_fts` USING fts4(`canonical_name`, `aliases`, `summary`, `content`)")

        val dao = roomDb.knowledgeCoreDao()
        val mkisAdapter = MkisAdapter()
        importer = KnowledgeCoreImporter(dao)
        bridge = PipelineBridge(mkisAdapter, importer)

        ftsSyncer = KnowledgeCoreFtsSyncer { db }
        searchRepo = RoomKnowledgeSearchRepository(dao) { db }
    }

    private fun createTestNode(
        title: String = "Estagnacao de Qi do Figado",
        content: String = "A estagnacao de Qi do Figado e um padrao frequente na clinica de MTC.",
        summary: String = "Padrao hepatico comum na MTC",
        knowledgeType: String = "artigo",  // maps to DOCUMENT in canonicalizer
    ) = KnowledgeNodeEntity(
        id = "node-${System.nanoTime()}",
        tenant_id = "default",
        title = title,
        summary = summary,
        content = content,
        knowledge_type = knowledgeType,
        status = "aprovado",  // maps to PUBLISHED via MkisAdapter
        evidence_level = "tradicional",
        bias_risk = "nenhum",
        source = "teste",
        source_url = "",
        checksum = "test-checksum-${System.nanoTime()}",
        tags = "figado qi estagnacao",
        metadata = """{"test": true, "pipeline_version": "1.0"}""",
        created_at = System.currentTimeMillis(),
        updated_at = System.currentTimeMillis(),
    )

    @Test
    fun bridge_singleEntity_appearsInCore() = runBlocking {
        val node = createTestNode()
        val success = bridge.bridgeEntity(node)
        assertTrue("Bridge should succeed", success)

        // Verify entity is in Knowledge Core
        val dao = roomDb.knowledgeCoreDao()
        val count = dao.countAll()
        assertEquals("Should have 1 entity in core", 1, count)

        // Verify via search
        ftsSyncer.rebuildFull()
        val results = searchRepo.search("estagnacao")
        assertTrue("Should find entity via FTS", results.isNotEmpty())
        assertTrue("Should match title", results.any { it.entity.canonicalName == "Estagnacao de Qi do Figado" })
    }

    @Test
    fun bridge_preservesTitle() = runBlocking {
        val node = createTestNode(title = "Formula Xiao Yao San")
        bridge.bridgeEntity(node)

        val dao = roomDb.knowledgeCoreDao()
        val count = dao.countAll()
        assertEquals(1, count)

        ftsSyncer.rebuildFull()
        val results = searchRepo.search("Xiao Yao San")
        assertTrue("Should find by title", results.isNotEmpty())
        assertEquals("Formula Xiao Yao San", results.first().entity.canonicalName)
    }

    @Test
    fun bridge_preservesContent() = runBlocking {
        val node = createTestNode(content = "Conteudo detalhado sobre acuponto LI4")
        bridge.bridgeEntity(node)

        ftsSyncer.rebuildFull()
        val results = searchRepo.search("acuponto LI4")
        assertTrue("Should find content", results.isNotEmpty())
        assertTrue(results.first().entity.content.contains("acuponto LI4"))
    }

    @Test
    fun bridge_preservesType() = runBlocking {
        val node = createTestNode(knowledgeType = "ponto")  // maps to ACUPOINT
        bridge.bridgeEntity(node)

        ftsSyncer.rebuildFull()
        val results = searchRepo.search("Estagnacao")
        // The title is still the same, but type should be ACUPOINT
        // Since we search by content, let's search by the title instead
        val entity = searchRepo.getById(results.first().entity.id)
        assertNotNull(entity)
        assertEquals("ACUPOINT", entity!!.type.name)
    }

    @Test
    fun bridge_preservesStatus() = runBlocking {
        val node = createTestNode()
        bridge.bridgeEntity(node)

        ftsSyncer.rebuildFull()
        val results = searchRepo.search("Estagnacao")
        assertTrue(results.isNotEmpty())
        // status "aprovado" maps to PUBLISHED via MkisAdapter
        assertEquals("PUBLISHED", results.first().entity.version.status.name)
    }

    @Test
    fun bridge_idempotent_noDuplicates() = runBlocking {
        val node = createTestNode()

        // Bridge twice
        bridge.bridgeEntity(node)
        bridge.bridgeEntity(node)

        // Should still have only 1 entity (idempotent)
        val dao = roomDb.knowledgeCoreDao()
        val count = dao.countAll()
        assertEquals("Idempotent: should have 1 entity, not 2", 1, count)
    }

    @Test
    fun bridge_multipleNodes() = runBlocking {
        val nodes = listOf(
            createTestNode(title = "Yin e Yang", knowledgeType = "teoria"),   // THEORY
            createTestNode(title = "Cinco Elementos", knowledgeType = "teoria"), // THEORY
            createTestNode(title = "Meridiano do Pulmao", knowledgeType = "meridiano"), // MERIDIAN
        )

        val result = bridge.bridgeAll(nodes)
        assertEquals(3, result.success)
        assertEquals(0, result.failed)

        val dao = roomDb.knowledgeCoreDao()
        assertEquals("Should have 3 entities", 3, dao.countAll())
    }

    @Test
    fun bridge_searchAfterImport() = runBlocking {
        val nodes = listOf(
            createTestNode(title = "Deficiencia de Yin do Rim", knowledgeType = "padrao"),
            createTestNode(title = "Insomia", knowledgeType = "sintoma"),
            createTestNode(title = "Ponto ST36 Zusanli", knowledgeType = "ponto"),
        )

        nodes.forEach { bridge.bridgeEntity(it) }
        ftsSyncer.rebuildFull()

        // Search by keyword
        val yinResults = searchRepo.search("Yin")
        assertTrue("Should find Yin content", yinResults.isNotEmpty())

        val st36 = searchRepo.search("ST36")
        assertTrue("Should find ST36", st36.isNotEmpty())
        assertTrue("Should find Zusanli in title", st36.any { it.entity.canonicalName.contains("ST36") })
    }

    @Test
    fun bridge_batchIdempotent() = runBlocking {
        val nodes = listOf(
            createTestNode(title = "Primeiro item"),
            createTestNode(title = "Segundo item"),
        )

        // Bridge twice
        bridge.bridgeAll(nodes)
        bridge.bridgeAll(nodes)

        val dao = roomDb.knowledgeCoreDao()
        assertEquals("Idempotent batch: should have 2 entities", 2, dao.countAll())
    }
}
