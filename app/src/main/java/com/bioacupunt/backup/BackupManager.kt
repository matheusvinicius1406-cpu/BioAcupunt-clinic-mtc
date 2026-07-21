package com.bioacupunt.backup

import android.content.Context
import com.bioacupunt.core.util.AppJson
import com.bioacupunt.data.local.database.AppDatabase
import com.bioacupunt.di.DatabaseModule
import com.bioacupunt.observability.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Backup e restauração do banco clínico inteiro.
 *
 * Um consultório perde o celular; o prontuário não pode ir junto. Este manager empacota
 * o banco Room num `.zip` que a médica manda para onde quiser (Google Drive via seletor
 * do sistema, ou a nuvem que ela escolher) e restaura de volta.
 *
 * **Checkpoint antes de copiar** é o detalhe que faz o backup ser íntegro: com WAL
 * ligado, os dados recentes vivem no `-wal`, não no `.db`. Copiar só o `.db` salvaria
 * um banco desatualizado. `wal_checkpoint(TRUNCATE)` dobra o WAL de volta no `.db`
 * primeiro. Ainda assim empacotamos os sidecars, por garantia.
 */
class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
) {

    @Serializable
    data class Manifest(
        val schemaVersion: Int = SCHEMA,
        val createdAtEpochMs: Long,
        val appVersionName: String,
        val files: List<String>,
    ) {
        companion object { const val SCHEMA = 1 }
    }

    private fun dbFiles(): List<File> {
        val main = DatabaseModule.databaseFile(context)
        return listOf(main, File(main.path + "-wal"), File(main.path + "-shm"))
    }

    private fun checkpoint() {
        runCatching {
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        }.onFailure { AppLogger.e("BackupManager", "checkpoint falhou (segue com o que há)", it) }
    }

    private fun appVersionName(): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?" }.getOrDefault("?")

    /** Escreve o backup no destino (ex.: OutputStream de um documento do SAF/Drive). */
    suspend fun createBackup(out: OutputStream): Result<Manifest> = withContext(Dispatchers.IO) {
        runCatching {
            checkpoint()
            val present = dbFiles().filter { it.exists() && it.length() > 0 }
            check(present.isNotEmpty()) { "Banco de dados não encontrado para backup." }
            val manifest = Manifest(
                createdAtEpochMs = System.currentTimeMillis(),
                appVersionName = appVersionName(),
                files = present.map { it.name },
            )
            val entries = present.map { BackupArchive.Entry(it.name, it.readBytes()) } +
                BackupArchive.Entry(BackupArchive.MANIFEST_NAME, AppJson.encodeToString(manifest).toByteArray())
            BackupArchive.write(out, entries)
            manifest
        }.onFailure { AppLogger.e("BackupManager", "createBackup falhou", it) }
    }

    /** Igual ao [createBackup], mas devolve os bytes (para enviar via API do Drive). */
    suspend fun createBackupBytes(): Result<ByteArray> = withContext(Dispatchers.IO) {
        val buffer = java.io.ByteArrayOutputStream()
        createBackup(buffer).map { buffer.toByteArray() }
    }

    /** Restaura a partir de bytes de um backup baixado (ex.: do Drive). */
    suspend fun restoreBackupBytes(bytes: ByteArray): Result<Manifest> =
        restoreBackup(java.io.ByteArrayInputStream(bytes))

    /**
     * Restaura de um backup. Sobrescreve os arquivos do banco e sinaliza que o app
     * precisa reiniciar — a conexão Room aberta aponta para os bytes antigos; só um
     * reinício abre o banco restaurado com segurança. Falha fechada: se o zip não tem
     * o nosso manifesto/banco, não toca em nada.
     */
    suspend fun restoreBackup(input: InputStream): Result<Manifest> = withContext(Dispatchers.IO) {
        runCatching {
            val map = BackupArchive.read(input)
            val manifestBytes = map[BackupArchive.MANIFEST_NAME]
                ?: error("Arquivo não é um backup do BioAcupunt (sem manifesto).")
            val manifest = AppJson.decodeFromString<Manifest>(String(manifestBytes))
            val mainName = DatabaseModule.DB_NAME
            check(map.containsKey(mainName)) { "Backup não contém o banco principal." }

            // Fecha a conexão antes de sobrescrever os arquivos por baixo dela.
            runCatching { database.close() }

            val dbDir = DatabaseModule.databaseFile(context).parentFile
            // Remove sidecars antigos para não misturar um -wal velho com um .db novo.
            dbFiles().forEach { runCatching { it.delete() } }
            map.forEach { (name, bytes) ->
                if (name == BackupArchive.MANIFEST_NAME) return@forEach
                val target = File(dbDir, name)
                target.outputStream().use { it.write(bytes) }
            }
            manifest
        }.onFailure { AppLogger.e("BackupManager", "restoreBackup falhou", it) }
    }

    companion object {
        /** Nome sugerido do arquivo no seletor. */
        fun suggestedFileName(): String {
            val ts = java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.US).format(java.util.Date())
            return "bioacupunt-backup-$ts.zip"
        }
        const val MIME_TYPE = "application/zip"
    }
}
