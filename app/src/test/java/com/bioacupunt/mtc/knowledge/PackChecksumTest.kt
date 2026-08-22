package com.bioacupunt.mtc.knowledge

import com.bioacupunt.mtc.knowledge.domain.KnowledgeEntity
import com.bioacupunt.mtc.knowledge.domain.KnowledgePack
import com.bioacupunt.mtc.knowledge.domain.KnowledgePackManifest
import com.bioacupunt.mtc.knowledge.domain.KnowledgeRelation
import com.bioacupunt.mtc.knowledge.domain.KnowledgeRelationType
import com.bioacupunt.mtc.knowledge.domain.KnowledgeVersion
import com.bioacupunt.mtc.knowledge.domain.KnowledgeEntityType
import com.bioacupunt.mtc.knowledge.domain.EditorialStatus
import com.bioacupunt.mtc.knowledge.domain.PackChecksum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PackChecksumTest {

    @Test
    fun sameContentSameChecksum() {
        val pack = makePack("e1", "e2")
        val checksum1 = PackChecksum.compute(pack)
        val checksum2 = PackChecksum.compute(pack)
        assertEquals(checksum1, checksum2)
    }

    @Test
    fun differentContentDifferentChecksum() {
        val pack1 = makePack("e1", "e2")
        val pack2 = makePack("e1", "e3")
        val checksum1 = PackChecksum.compute(pack1)
        val checksum2 = PackChecksum.compute(pack2)
        assertNotEquals(checksum1, checksum2)
    }

    @Test
    fun entityOrderDoesNotMatter() {
        val pack1 = KnowledgePack(
            manifest = makeManifest(),
            entities = listOf(makeEntity("e1"), makeEntity("e2")),
        )
        val pack2 = KnowledgePack(
            manifest = makeManifest(),
            entities = listOf(makeEntity("e2"), makeEntity("e1")),
        )
        assertEquals(PackChecksum.compute(pack1), PackChecksum.compute(pack2))
    }

    @Test
    fun checksumIsValidSha256() {
        val pack = makePack("e1")
        val checksum = PackChecksum.compute(pack)
        assertEquals(64, checksum.length) // SHA-256 hex = 64 chars
        assertTrue(checksum.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun verifyMatchesSelf() {
        val pack = makePack("e1")
        val checksum = PackChecksum.compute(pack)
        assertTrue(PackChecksum.verify(pack, checksum))
    }

    @Test
    fun verifyRejectsMismatch() {
        val pack = makePack("e1")
        val wrongChecksum = "0".repeat(64)
        assertTrue(!PackChecksum.verify(pack, wrongChecksum))
    }

    @Test
    fun verifyRejectsBlank() {
        val pack = makePack("e1")
        assertTrue(!PackChecksum.verify(pack, ""))
    }

    @Test
    fun modifiedEntityChangesChecksum() {
        val pack1 = makePack("e1")
        val checksum1 = PackChecksum.compute(pack1)

        val modified = KnowledgePack(
            manifest = makeManifest(),
            entities = listOf(KnowledgeEntity(
                id = "e1",
                type = KnowledgeEntityType.SYMPTOM,
                canonicalName = "MODIFIED",
                version = KnowledgeVersion(version = "1.0", createdAt = 0, updatedAt = 0),
            )),
        )
        val checksum2 = PackChecksum.compute(modified)
        assertNotEquals(checksum1, checksum2)
    }

    @Test
    fun emptyPackHasValidChecksum() {
        val pack = KnowledgePack(manifest = makeManifest())
        val checksum = PackChecksum.compute(pack)
        assertEquals(64, checksum.length)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun makePack(vararg entityIds: String) = KnowledgePack(
        manifest = makeManifest(),
        entities = entityIds.map { makeEntity(it) },
    )

    private fun makeManifest() = KnowledgePackManifest(
        packId = "test-pack",
        version = "1.0.0",
        schemaVersion = "1.0.0",
    )

    private fun makeEntity(id: String) = KnowledgeEntity(
        id = id,
        type = KnowledgeEntityType.SYMPTOM,
        canonicalName = "Entity $id",
        version = KnowledgeVersion(version = "1.0", createdAt = 0, updatedAt = 0),
    )
}
