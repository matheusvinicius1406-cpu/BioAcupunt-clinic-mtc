package com.bioacupunt.backup

import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Empacotamento do backup — zip puro, sem Android, testável.
 *
 * Um backup é um `.zip` com os arquivos do banco (`bioacupunt_db` + `-wal`/`-shm`) e um
 * manifesto. Separado do [BackupManager] de propósito: a lógica de arquivar/desarquivar
 * é onde erros silenciosos doem (um zip corrompido que "restaura" pela metade), então
 * ela tem teste de ida e volta sem precisar de device.
 */
object BackupArchive {

    const val MANIFEST_NAME = "backup_manifest.json"

    data class Entry(val name: String, val bytes: ByteArray)

    fun write(out: OutputStream, entries: List<Entry>) {
        ZipOutputStream(out).use { zip ->
            for (entry in entries) {
                zip.putNextEntry(ZipEntry(entry.name))
                zip.write(entry.bytes)
                zip.closeEntry()
            }
        }
    }

    fun read(input: InputStream): Map<String, ByteArray> {
        val result = LinkedHashMap<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                // Path traversal defense: um zip malicioso não pode escrever "../fora".
                val safeName = entry.name.substringAfterLast('/')
                if (safeName.isNotBlank()) result[safeName] = zip.readBytes()
                zip.closeEntry()
            }
        }
        return result
    }
}
