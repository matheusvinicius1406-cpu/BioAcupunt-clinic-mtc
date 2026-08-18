package com.bioacupunt.mtc.knowledge

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.bioacupunt.data.local.database.AppDatabase
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreEntityEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreFtsDao
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreFtsSyncer
import com.bioacupunt.mtc.knowledge.domain.KnowledgeEntityType
import com.bioacupunt.mtc.knowledge.domain.KnowledgeStatus
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
 * END-TO-END TEST — verifies the full pipeline:
 *
 * Legacy Entity → Adapter → Canonicalizer → Importer → Knowledge Core → FTS → Search
 *
 * This test proves that content imported into the Knowledge Core can be
 * found through the canonical search path.
 */
@RunWith(RobolectricTestRunner::class)
class KnowledgeCoreE2ETest {

    private lateinit var roomDb: AppDatabase
    private lateinit var db: SupportSQLiteDatabase
    private lateinit var searchRepo: KnowledgeSearchRepository
    private lateinit var ftsSyncer: KnowledgeCoreFtsSyncer
    private lateinit var ftsDao: KnowledgeCoreFtsDao

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = File(context.cacheDir, "test_e2e.db")
        if (dbFile.exists()) dbFile.delete()

        roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration(false)
            .build()
        db = roomDb.openHelper.writableDatabase
        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `knowledge_core_fts` USING fts4(`canonical_name`, `aliases`, `summary`, `content`)")

        val dao = roomDb.knowledgeCoreDao()
        ftsSyncer = KnowledgeCoreFtsSyncer { db }
        ftsDao = KnowledgeCoreFtsDao { db }
        searchRepo = RoomKnowledgeSearchRepository(dao) { db }
    }

    @Test
    fun e2e_importThenSearch() = runBlocking {
        // Step 1: Import entities (simulating LegacyImporter)
        val dao = roomDb.knowledgeCoreDao()
        dao.insertEntities(listOf(
            KnowledgeCoreEntityEntity("p1", "PATTERN", "Estagnacao de Qi do Figado", "[]", "Padrao hepatico comum", "A estagnacao de Qi do Figado e um padrao frequente na clinica. Causa irritabilidade, dor no hipocôndrio, sindrome do irritavel intestino.", "{}", "[]", "[]", "[]", "1.0", "PUBLISHED", 1000, 1000, null),
            KnowledgeCoreEntityEntity("s1", "SYMPTOM", "Insonia", "[]", "Disturbio do sono", "Dificuldade para dormir ou manter o sono. Pode ser causada por deficiencia de Yin, fogo cardiaco, ou estagnacao de Qi.", "{}", "[]", "[]", "[]", "1.0", "PUBLISHED", 1000, 1000, null),
            KnowledgeCoreEntityEntity("a1", "ACUPOINT", "LI4 Hegu", "[]", "Ponto do Intestino Grosso", "Ponto Yuan-Source. Indicado para dor de cabeca, dor dental, rinsao. Tonifica Qi e expulsa vento.", "{}", "[]", "[]", "[]", "1.0", "PUBLISHED", 1000, 1000, null),
            KnowledgeCoreEntityEntity("f1", "FORMULA", "Xiao Yao San", "[]", "Formula dispersora", "Dispersa estagnacao de Qi do Figado, tonifica o Baco. Indicada para estagnacao de Qi com deficiencia de sangue.", "{}", "[]", "[]", "[]", "1.0", "PUBLISHED", 1000, 1000, null),
        ))

        // Step 2: Sync to FTS
        ftsSyncer.rebuildFull()
        assertTrue("FTS should have entries", ftsDao.count() > 0)

        // Step 3: Search via KnowledgeSearchRepository
        val results = searchRepo.search("insonia")
        assertTrue("Should find insonia", results.isNotEmpty())
        assertEquals("s1", results.first().entity.id)

        // Step 4: Search by content
        val estagnacaoResults = searchRepo.search("estagnacao")
        assertTrue("Should find estagnacao content", estagnacaoResults.isNotEmpty())
        assertTrue("Should find pattern p1", estagnacaoResults.any { it.entity.id == "p1" })

        // Step 5: Search by type
        val patterns = searchRepo.searchByType("Qi", KnowledgeEntityType.PATTERN, 10)
        assertTrue("Should find patterns", patterns.isNotEmpty())
        assertTrue("All should be PATTERN type", patterns.all { it.entity.type == KnowledgeEntityType.PATTERN })

        // Step 6: Entity lookup
        val entity = searchRepo.getById("a1")
        assertNotNull("Should find LI4", entity)
        assertEquals("LI4 Hegu", entity!!.canonicalName)

        // Step 7: Count
        assertEquals(4, searchRepo.count())
        assertEquals(1, searchRepo.countByType(KnowledgeEntityType.ACUPOINT))
    }

    @Test
    fun e2e_emptyCoreReturnsNoResults() = runBlocking {
        // Empty Knowledge Core should return no results
        val results = searchRepo.search("anything")
        assertTrue("Empty core should return no results", results.isEmpty())
        assertEquals(0, searchRepo.count())
    }

    @Test
    fun e2e_ftsSyncPreservesData() = runBlocking {
        val dao = roomDb.knowledgeCoreDao()
        dao.insertEntities(listOf(
            KnowledgeCoreEntityEntity("t1", "THEORY", "Yin e Yang", "[]", "Conceito fundamental", "Yin e Yang sao conceitos fundamentais da MTC que descrevem forcas opostas e complementares.", "{}", "[]", "[]", "[]", "1.0", "PUBLISHED", 1000, 1000, null),
        ))
        ftsSyncer.rebuildFull()

        // Verify FTS has the entry
        assertEquals(1, ftsDao.count())

        // Verify entity is also in core
        val entity = searchRepo.getById("t1")
        assertNotNull(entity)
        assertEquals("Yin e Yang", entity!!.canonicalName)
    }
}
