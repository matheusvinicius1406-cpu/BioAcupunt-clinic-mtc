package com.bioacupunt.mtc.knowledge

import com.bioacupunt.mtc.knowledge.domain.InstalledPack
import com.bioacupunt.mtc.knowledge.domain.KnowledgeEntity
import com.bioacupunt.mtc.knowledge.domain.KnowledgePack
import com.bioacupunt.mtc.knowledge.domain.KnowledgePackManifest
import com.bioacupunt.mtc.knowledge.domain.KnowledgeVersion
import com.bioacupunt.mtc.knowledge.domain.KnowledgeEntityType
import com.bioacupunt.mtc.knowledge.domain.PackStatus
import com.bioacupunt.mtc.knowledge.domain.usecase.ManageKnowledgePackUseCase
import com.bioacupunt.mtc.knowledge.repository.InstalledPackRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ManageKnowledgePackUseCaseTest {

    private lateinit var fakeRepo: FakeInstalledPackRepository
    private lateinit var useCase: ManageKnowledgePackUseCase

    @Before
    fun setUp() {
        fakeRepo = FakeInstalledPackRepository()
        useCase = ManageKnowledgePackUseCase(fakeRepo)
    }

    @Test
    fun validate_validPack_returnsValid() = kotlinx.coroutines.runBlocking {
        val pack = makePack("e1", "e2")
        val result = useCase.validate(pack, tenantId = 1L)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isValid)
    }

    @Test
    fun validate_invalidPack_returnsErrors() = kotlinx.coroutines.runBlocking {
        val pack = makePackWithEmptyId()
        val result = useCase.validate(pack, tenantId = 1L)
        assertTrue(result.isSuccess)
        assertTrue(!result.getOrThrow().isValid)
    }

    @Test
    fun install_stagesPack() = kotlinx.coroutines.runBlocking {
        val pack = makePack("e1")
        val result = useCase.install(pack, tenantId = 1L)
        assertTrue(result.isSuccess)
        val installed = result.getOrThrow()
        assertEquals(PackStatus.STAGED, installed.status)
        assertEquals("test-pack", installed.packId)
        assertEquals("1.0.0", installed.version)
    }

    @Test
    fun activate_switchesActivePack() = kotlinx.coroutines.runBlocking {
        // Install v1 and v2
        val pack1 = makePack("e1", version = "1.0.0")
        val pack2 = makePack("e1", "e2", version = "2.0.0")
        useCase.install(pack1, tenantId = 1L).getOrThrow()
        useCase.install(pack2, tenantId = 1L).getOrThrow()

        // Activate v1
        val activated1 = useCase.activate("test-pack", "1.0.0", tenantId = 1L).getOrThrow()
        assertEquals(PackStatus.ACTIVE, activated1.status)

        // Activate v2 — v1 should become INACTIVE
        val activated2 = useCase.activate("test-pack", "2.0.0", tenantId = 1L).getOrThrow()
        assertEquals(PackStatus.ACTIVE, activated2.status)

        // Verify v1 is inactive
        val allVersions = useCase.getAllVersions("test-pack", tenantId = 1L).getOrThrow()
        val v1 = allVersions.find { it.version == "1.0.0" }
        assertNotNull(v1)
        assertEquals(PackStatus.INACTIVE, v1!!.status)
    }

    @Test
    fun rollback_revertsToPreviousVersion() = kotlinx.coroutines.runBlocking {
        // Install v1 and v2, activate v2
        val pack1 = makePack("e1", version = "1.0.0")
        val pack2 = makePack("e1", "e2", version = "2.0.0")
        useCase.install(pack1, tenantId = 1L).getOrThrow()
        useCase.install(pack2, tenantId = 1L).getOrThrow()
        useCase.activate("test-pack", "2.0.0", tenantId = 1L).getOrThrow()

        // Rollback to v1
        val rolledBack = useCase.rollback("test-pack", "1.0.0", tenantId = 1L).getOrThrow()
        assertEquals(PackStatus.ACTIVE, rolledBack.status)
        assertEquals("1.0.0", rolledBack.version)

        // v2 should be ROLLBACK
        val allVersions = useCase.getAllVersions("test-pack", tenantId = 1L).getOrThrow()
        val v2 = allVersions.find { it.version == "2.0.0" }
        assertNotNull(v2)
        assertEquals(PackStatus.ROLLBACK, v2!!.status)
    }

    @Test
    fun rollback_invalidTarget_fails() = kotlinx.coroutines.runBlocking {
        val pack1 = makePack("e1", version = "1.0.0")
        useCase.install(pack1, tenantId = 1L).getOrThrow()
        useCase.activate("test-pack", "1.0.0", tenantId = 1L).getOrThrow()

        val result = useCase.rollback("test-pack", "9.9.9", tenantId = 1L)
        assertTrue(result.isFailure)
    }

    @Test
    fun diff_detectsAddedEntities() {
        val oldPack = makePack("e1")
        val newPack = makePack("e1", "e2")
        val diff = useCase.diff(oldPack, newPack)
        assertEquals(1, diff.added.size)
        assertEquals("e2", diff.added[0].entityId)
    }

    @Test
    fun diff_detectsRemovedEntities() {
        val oldPack = makePack("e1", "e2")
        val newPack = makePack("e1")
        val diff = useCase.diff(oldPack, newPack)
        assertEquals(1, diff.removed.size)
        assertEquals("e2", diff.removed[0].entityId)
    }

    @Test
    fun diff_detectsChangedEntities() {
        val oldPack = KnowledgePack(
            manifest = makeManifest(),
            entities = listOf(KnowledgeEntity(
                id = "e1", type = KnowledgeEntityType.SYMPTOM,
                canonicalName = "Old", content = "old content",
                version = KnowledgeVersion(version = "1.0", createdAt = 0, updatedAt = 0),
            )),
        )
        val newPack = KnowledgePack(
            manifest = makeManifest(),
            entities = listOf(KnowledgeEntity(
                id = "e1", type = KnowledgeEntityType.SYMPTOM,
                canonicalName = "New", content = "new content",
                version = KnowledgeVersion(version = "2.0", createdAt = 0, updatedAt = 0),
            )),
        )
        val diff = useCase.diff(oldPack, newPack)
        assertEquals(1, diff.changed.size)
        assertEquals("e1", diff.changed[0].entityId)
    }

    @Test
    fun getActive_returnsNullWhenNone() = kotlinx.coroutines.runBlocking {
        val result = useCase.getActive("nonexistent", tenantId = 1L)
        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }

    @Test
    fun getActive_returnsActivePack() = kotlinx.coroutines.runBlocking {
        val pack = makePack("e1")
        useCase.install(pack, tenantId = 1L).getOrThrow()
        useCase.activate("test-pack", "1.0.0", tenantId = 1L).getOrThrow()

        val active = useCase.getActive("test-pack", tenantId = 1L).getOrThrow()
        assertNotNull(active)
        assertEquals(PackStatus.ACTIVE, active!!.status)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun makePack(vararg entityIds: String, version: String = "1.0.0") = KnowledgePack(
        manifest = makeManifest(version = version),
        entities = entityIds.map { makeEntity(it) },
    )

    private fun makePackWithEmptyId() = KnowledgePack(
        manifest = makeManifest(packId = ""),
    )

    private fun makeManifest(packId: String = "test-pack", version: String = "1.0.0") = KnowledgePackManifest(
        packId = packId,
        version = version,
        schemaVersion = "1.0.0",
    )

    private fun makeEntity(id: String) = KnowledgeEntity(
        id = id,
        type = KnowledgeEntityType.SYMPTOM,
        canonicalName = "Entity $id",
        version = KnowledgeVersion(version = "1.0", createdAt = 0, updatedAt = 0),
    )

    // ── Fake Repository ──────────────────────────────────────────────────────

    private class FakeInstalledPackRepository : InstalledPackRepository {
        private val packs = mutableListOf<InstalledPack>()
        private var nextId = 1L

        override suspend fun getAll(tenantId: Long) = Result.success(packs.filter { it.tenantId == tenantId && !it.isDeleted() })
        override suspend fun getByPackId(tenantId: Long, packId: String) = Result.success(packs.filter { it.tenantId == tenantId && it.packId == packId })
        override suspend fun getActive(tenantId: Long, packId: String) = Result.success(packs.find { it.tenantId == tenantId && it.packId == packId && it.status == PackStatus.ACTIVE && !it.isDeleted() })
        override suspend fun getAllActive(tenantId: Long) = Result.success(packs.filter { it.tenantId == tenantId && it.status == PackStatus.ACTIVE && !it.isDeleted() })
        override suspend fun getById(id: Long) = Result.success(packs.find { it.id == id && !it.isDeleted() })
        override suspend fun getByPackIdAndVersion(tenantId: Long, packId: String, version: String) = Result.success(packs.find { it.tenantId == tenantId && it.packId == packId && it.version == version && !it.isDeleted() })
        override suspend fun insert(pack: InstalledPack): Result<Long> {
            val id = nextId++
            packs.add(pack.copy(id = id))
            return Result.success(id)
        }
        override suspend fun update(pack: InstalledPack): Result<Unit> {
            val idx = packs.indexOfFirst { it.id == pack.id }
            if (idx >= 0) packs[idx] = pack
            return Result.success(Unit)
        }
        override suspend fun updateStatus(id: Long, status: PackStatus): Result<Unit> {
            val idx = packs.indexOfFirst { it.id == id }
            if (idx >= 0) packs[idx] = packs[idx].copy(status = status)
            return Result.success(Unit)
        }
        override suspend fun softDelete(id: Long): Result<Unit> {
            val idx = packs.indexOfFirst { it.id == id }
            if (idx >= 0) packs[idx] = packs[idx].copy(status = PackStatus.INACTIVE)
            return Result.success(Unit)
        }
        override suspend fun countByPackId(tenantId: Long, packId: String) = Result.success(packs.count { it.tenantId == tenantId && it.packId == packId && !it.isDeleted() })

        private fun InstalledPack.isDeleted() = status == PackStatus.ROLLBACK
    }
}
