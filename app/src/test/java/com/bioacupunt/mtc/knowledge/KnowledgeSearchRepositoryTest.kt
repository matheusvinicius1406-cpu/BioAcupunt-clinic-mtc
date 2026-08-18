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
import com.bioacupunt.mtc.knowledge.repository.RoomKnowledgeSearchRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class KnowledgeSearchRepositoryTest {

    private lateinit var roomDb: AppDatabase
    private lateinit var db: SupportSQLiteDatabase
    private lateinit var searchRepo: RoomKnowledgeSearchRepository
    private lateinit var ftsSyncer: KnowledgeCoreFtsSyncer
    private lateinit var ftsDao: KnowledgeCoreFtsDao

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = File(context.cacheDir, "test_search_fts.db")
        if (dbFile.exists()) dbFile.delete()

        roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration(false)
            .build()
        db = roomDb.openHelper.writableDatabase

        // Use FTS4 (proven on Android SQLite) instead of FTS5
        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `knowledge_core_fts` USING fts4(`canonical_name`, `aliases`, `summary`, `content`)")

        val dao = roomDb.knowledgeCoreDao()
        ftsSyncer = KnowledgeCoreFtsSyncer { db }
        ftsDao = KnowledgeCoreFtsDao { db }
        searchRepo = RoomKnowledgeSearchRepository(dao) { db }

        // Seed entities
        dao.insertEntities(listOf(
            KnowledgeCoreEntityEntity("p1", "PATTERN", "Estagnacao de Qi", "[]", "Padrao hepatico", "Conteudo sobre estagnacao", "{}", "[]", "[]", "[]", "1.0", "PUBLISHED", 1000, 1000, null),
            KnowledgeCoreEntityEntity("s1", "SYMPTOM", "Insonia", "[]", "Disturbio do sono", "Dificuldade para dormir", "{}", "[]", "[]", "[]", "1.0", "PUBLISHED", 1000, 1000, null),
            KnowledgeCoreEntityEntity("a1", "ACUPOINT", "LI4 Hegu", "[]", "Ponto HAND-4", "Ponto do Intestino Grosso", "{}", "[]", "[]", "[]", "1.0", "PUBLISHED", 1000, 1000, null),
            KnowledgeCoreEntityEntity("f1", "FORMULA", "Xiao Yao San", "[]", "Formula hepatica", "Dispersa estagnacao de Qi", "{}", "[]", "[]", "[]", "1.0", "DRAFT", 1000, 1000, null),
        ))
        ftsSyncer.rebuildFull()
    }

    @Test
    fun search_findsByCanonicalName() = runBlocking {
        val results = searchRepo.search("Insonia")
        assertTrue("Expected results for 'Insonia', got ${results.size}", results.isNotEmpty())
        assertEquals("s1", results.first().entity.id)
    }

    @Test
    fun search_findsByContent() = runBlocking {
        val results = searchRepo.search("estagnacao")
        assertTrue("Expected results for 'estagnacao', got ${results.size}", results.isNotEmpty())
        assertTrue(results.any { it.entity.id == "p1" })
    }

    @Test
    fun search_emptyQuery_returnsEmpty() = runBlocking {
        assertTrue(searchRepo.search("").isEmpty())
    }

    @Test
    fun search_noMatch_returnsEmpty() = runBlocking {
        assertTrue(searchRepo.search("xyznonexistent").isEmpty())
    }

    @Test
    fun searchByType_filtersCorrectly() = runBlocking {
        val results = searchRepo.searchByType("Qi", KnowledgeEntityType.PATTERN, 10)
        assertTrue(results.all { it.entity.type == KnowledgeEntityType.PATTERN })
    }

    @Test
    fun searchByStatus_filtersCorrectly() = runBlocking {
        val results = searchRepo.searchByStatus("San", KnowledgeStatus.DRAFT, 10)
        assertTrue(results.all { it.entity.version.status == KnowledgeStatus.DRAFT })
    }

    @Test
    fun getById_returnsEntity() = runBlocking {
        assertNotNull(searchRepo.getById("p1"))
        assertEquals("Estagnacao de Qi", searchRepo.getById("p1")!!.canonicalName)
    }

    @Test
    fun getById_returnsNullForMissing() = runBlocking {
        assertNull(searchRepo.getById("nonexistent"))
    }

    @Test
    fun count_returnsTotal() = runBlocking {
        assertEquals(4, searchRepo.count())
    }

    @Test
    fun countByType_returnsCorrectCount() = runBlocking {
        assertEquals(1, searchRepo.countByType(KnowledgeEntityType.PATTERN))
        assertEquals(0, searchRepo.countByType(KnowledgeEntityType.HERB))
    }

    @Test
    fun ftsSyncer_populatesIndex() {
        val count = ftsDao.count()
        assertTrue("FTS should have entries, got $count", count > 0)
    }
}
