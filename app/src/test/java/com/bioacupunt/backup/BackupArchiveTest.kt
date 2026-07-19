package com.bioacupunt.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BackupArchiveTest {

    @Test
    fun roundTripPreservesEveryEntry() {
        val entries = listOf(
            BackupArchive.Entry("bioacupunt_db", byteArrayOf(1, 2, 3, 4, 5)),
            BackupArchive.Entry("bioacupunt_db-wal", byteArrayOf(9, 8, 7)),
            BackupArchive.Entry(BackupArchive.MANIFEST_NAME, "{\"v\":12}".toByteArray()),
        )
        val out = ByteArrayOutputStream()
        BackupArchive.write(out, entries)

        val read = BackupArchive.read(ByteArrayInputStream(out.toByteArray()))
        assertEquals(3, read.size)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), read["bioacupunt_db"])
        assertArrayEquals(byteArrayOf(9, 8, 7), read["bioacupunt_db-wal"])
        assertEquals("{\"v\":12}", String(read[BackupArchive.MANIFEST_NAME]!!))
    }

    @Test
    fun pathTraversalNamesAreFlattened() {
        // Um zip com "../../etc/x" não pode virar escrita fora do destino: o nome é
        // reduzido ao último segmento antes de qualquer uso.
        val entries = listOf(BackupArchive.Entry("../../evil", byteArrayOf(0)))
        val out = ByteArrayOutputStream()
        BackupArchive.write(out, entries)

        val read = BackupArchive.read(ByteArrayInputStream(out.toByteArray()))
        assertTrue(read.containsKey("evil"))
        assertNull(read["../../evil"])
    }
}
