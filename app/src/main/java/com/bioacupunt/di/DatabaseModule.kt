package com.bioacupunt.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bioacupunt.data.local.database.AppDatabase
import java.io.File

object DatabaseModule {

    const val DB_NAME = "bioacupunt_db"

    /** Arquivo principal do banco. O backup empacota este + os sidecars -wal/-shm. */
    fun databaseFile(context: Context): File = context.getDatabasePath(DB_NAME)
    private const val BACKUP_DIR_NAME = "db_backups"

    // MUST match @Database(version = …) on AppDatabase. Kept as a plain
    // constant on purpose: reading the version by reflecting the @Database
    // annotation at runtime (AppDatabase::class.java.getAnnotation(...)) returns
    // null on device — Room's @Database is consumed by the compile-time
    // annotation processor and is not retained for runtime reflection — which
    // crashed every database access with "AppDatabase must be annotated with
    // @Database".
    private const val DB_VERSION = 18

    /** Byte offset of `user_version` in the SQLite file header. */
    private const val USER_VERSION_OFFSET = 60L

    // @Volatile is required here, not decorative: this is textbook double-checked
    // locking (check outside the lock, re-check inside). Without it, a reader on
    // another thread can observe `initialized == true` before the write to
    // `instance` is visible to it (JMM reordering), and return a database handle
    // that isn't safely published — the same failure family as the lateinit/init()
    // bug already caught in RetrofitInstance (see CLAUDE.md).
    @Volatile private var initialized = false
    private lateinit var instance: AppDatabase

    fun provideAppDatabase(context: Context): AppDatabase {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    val appContext = context.applicationContext
                    val databaseFile = appContext.getDatabasePath(DB_NAME)
                    val existingVersion = if (databaseFile.exists()) {
                        runCatching { getCurrentDbVersion(databaseFile) }.getOrNull()
                    } else null

                    if (existingVersion != null && existingVersion < DB_VERSION) {
                        backupDatabase(appContext)
                    }

                    val migrations = buildMigrations()
                    instance = Room.databaseBuilder(appContext, AppDatabase::class.java, DB_NAME)
                        .addMigrations(*migrations.toTypedArray())
                        .build()
                    initialized = true
                }
            }
        }
        return instance
    }

    /**
     * Reads `user_version` — which SQLite stores at byte offset 60 of the file
     * header, and which Room uses as the schema version.
     *
     * This used to read offset 24, the file *change counter*: a number that goes
     * up on every write and is therefore always far larger than any schema
     * version. `existingVersion < DB_VERSION` was consequently never true, so the
     * pre-migration backup below never ran a single time. Silent missing
     * insurance on a database holding patient charts.
     */
    private fun getCurrentDbVersion(databaseFile: File): Int {
        var version = 0
        databaseFile.inputStream().use { input ->
            val buffer = ByteArray(32)
            var skipped = 0L
            while (skipped < USER_VERSION_OFFSET) {
                val n = input.skip(USER_VERSION_OFFSET - skipped)
                if (n <= 0) return 0
                skipped += n
            }
            val read = input.read(buffer, 0, 4)
            if (read == 4) {
                version = ((buffer[0].toInt() and 0xFF) shl 24) or
                    ((buffer[1].toInt() and 0xFF) shl 16) or
                    ((buffer[2].toInt() and 0xFF) shl 8) or
                    (buffer[3].toInt() and 0xFF)
            }
        }
        return version
    }

    private fun backupDatabase(context: Context) {
        runCatching {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) return@runCatching
            val backupDir = File(context.filesDir, BACKUP_DIR_NAME)
            if (!backupDir.exists()) backupDir.mkdirs()
            val timestamp = System.currentTimeMillis()
            val backupFile = File(backupDir, "$DB_NAME-$timestamp.bak")
            dbFile.copyTo(backupFile, overwrite = true)
            arrayOf("-wal", "-shm").forEach { suffix ->
                val src = File(dbFile.parentFile, dbFile.name + suffix)
                val dst = File(backupDir, "$DB_NAME-$timestamp.bak$suffix")
                if (src.exists()) {
                    src.copyTo(dst, overwrite = true)
                }
            }
            deleteOldBackups(backupDir)
            com.bioacupunt.observability.AppLogger.i("DatabaseModule", "Database backup created: ${backupFile.absolutePath}")
        }.onFailure { e ->
            com.bioacupunt.observability.AppLogger.e("DatabaseModule", "Database backup failed", e)
        }
    }

    private fun deleteOldBackups(backupDir: File) {
        runCatching {
            val backups = backupDir.listFiles()?.sortedBy { it.name } ?: emptyList()
            val toDelete = backups.dropLast(5)
            toDelete.forEach { it.delete() }
        }
    }

    /** Exposed so migrations can be exercised against a real SQLite file in tests. */
    @androidx.annotation.VisibleForTesting
    internal fun migrations(): List<androidx.room.migration.Migration> = buildMigrations()

    private fun buildMigrations(): List<androidx.room.migration.Migration> {
        val current = DB_VERSION
        val migrations = mutableListOf<androidx.room.migration.Migration>()

        if (current >= 2) migrations.add(MIGRATION_1_2)
        if (current >= 3) migrations.add(MIGRATION_2_3)
        if (current >= 4) migrations.add(MIGRATION_3_4)
        if (current >= 5) migrations.add(MIGRATION_4_5)
        if (current >= 6) migrations.add(MIGRATION_5_6)
        if (current >= 7) migrations.add(MIGRATION_6_7)
        if (current >= 8) migrations.add(MIGRATION_7_8)
        if (current >= 9) migrations.add(MIGRATION_8_9)
        if (current >= 10) migrations.add(MIGRATION_9_10)
        if (current >= 11) migrations.add(MIGRATION_10_11)
        if (current >= 12) migrations.add(MIGRATION_11_12)
        if (current >= 13) migrations.add(MIGRATION_12_13)
        if (current >= 14) migrations.add(MIGRATION_13_14)
        if (current >= 15) migrations.add(MIGRATION_14_15)
        if (current >= 16) migrations.add(MIGRATION_15_16)
        if (current >= 17) migrations.add(MIGRATION_16_17)
        if (current >= 18) migrations.add(MIGRATION_17_18)
        return migrations
    }

    private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE patients ADD COLUMN pendingSync INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE patients ADD COLUMN tenantId INTEGER NOT NULL DEFAULT 1")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_patients_tenantId ON patients(tenantId)")
        }
    }

    private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE sync_queue ADD COLUMN retryCount INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE sync_queue ADD COLUMN lastError TEXT")
        }
    }

    private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE sync_queue ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
        }
    }

    private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE appointments ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
        }
    }

    private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE appointments ADD COLUMN sessionNumber INTEGER NOT NULL DEFAULT 1")
        }
    }

    private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE appointments ADD COLUMN valueBrl REAL NOT NULL DEFAULT 0.0")
            database.execSQL("ALTER TABLE appointments ADD COLUMN paid INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * Adds the structured TCM assessment ("Prontuário Supremo").
     *
     * Additive only: no existing table is touched, so no chart written by v8 can be
     * lost by upgrading. The column types and the index names must match exactly what
     * Room derives from [com.bioacupunt.prontuario.data.local.MtcAssessmentEntity],
     * or Room throws an IllegalStateException on open — which, given this app already
     * died once at startup, is a failure mode worth being paranoid about.
     */
    private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `mtc_assessments` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `patientId` INTEGER NOT NULL,
                    `date` TEXT NOT NULL DEFAULT '',
                    `chiefComplaint` TEXT NOT NULL DEFAULT '',
                    `baGangPolarity` TEXT NOT NULL DEFAULT 'UNSET',
                    `baGangDepth` TEXT NOT NULL DEFAULT 'UNSET',
                    `baGangTemperature` TEXT NOT NULL DEFAULT 'UNSET',
                    `baGangStrength` TEXT NOT NULL DEFAULT 'UNSET',
                    `patternsJson` TEXT NOT NULL DEFAULT '[]',
                    `tongueJson` TEXT NOT NULL DEFAULT '{}',
                    `pulseJson` TEXT NOT NULL DEFAULT '{}',
                    `bodyMarksJson` TEXT NOT NULL DEFAULT '[]',
                    `flagsCsv` TEXT NOT NULL DEFAULT '',
                    `gestationalWeeks` INTEGER,
                    `clinicalImpression` TEXT NOT NULL DEFAULT '',
                    `syncedAt` TEXT,
                    `createdAt` TEXT NOT NULL DEFAULT '',
                    `updatedAt` TEXT NOT NULL DEFAULT '',
                    FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_mtc_assessments_patientId` ON `mtc_assessments` (`patientId`)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_mtc_assessments_date` ON `mtc_assessments` (`date`)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_mtc_assessments_updatedAt` ON `mtc_assessments` (`updatedAt`)",
            )
        }
    }

    /**
     * Adds the "Exames" (vitals/labs/medications/allergies) and "Documentos" tables.
     * Additive only, same paranoia as MIGRATION_8_9: column types and index names must
     * match exactly what Room derives from the corresponding entities.
     */
    private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `vital_signs` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `patientId` INTEGER NOT NULL,
                    `label` TEXT NOT NULL,
                    `value` TEXT NOT NULL,
                    `recordedAt` TEXT NOT NULL DEFAULT '',
                    `createdAt` TEXT NOT NULL DEFAULT '',
                    `updatedAt` TEXT NOT NULL DEFAULT '',
                    FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_vital_signs_patientId` ON `vital_signs` (`patientId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_vital_signs_recordedAt` ON `vital_signs` (`recordedAt`)")

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `lab_exams` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `patientId` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `date` TEXT NOT NULL DEFAULT '',
                    `resultTag` TEXT NOT NULL DEFAULT 'PENDING',
                    `notes` TEXT NOT NULL DEFAULT '',
                    `createdAt` TEXT NOT NULL DEFAULT '',
                    `updatedAt` TEXT NOT NULL DEFAULT '',
                    FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_lab_exams_patientId` ON `lab_exams` (`patientId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_lab_exams_date` ON `lab_exams` (`date`)")

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `medications` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `patientId` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `info` TEXT NOT NULL DEFAULT '',
                    `active` INTEGER NOT NULL DEFAULT 1,
                    `createdAt` TEXT NOT NULL DEFAULT '',
                    `updatedAt` TEXT NOT NULL DEFAULT '',
                    FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_medications_patientId` ON `medications` (`patientId`)")

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `allergies` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `patientId` INTEGER NOT NULL,
                    `description` TEXT NOT NULL,
                    `createdAt` TEXT NOT NULL DEFAULT '',
                    `updatedAt` TEXT NOT NULL DEFAULT '',
                    FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_allergies_patientId` ON `allergies` (`patientId`)")

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `prontuario_documents` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `patientId` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `uri` TEXT NOT NULL,
                    `mimeType` TEXT NOT NULL DEFAULT '',
                    `sizeBytes` INTEGER NOT NULL DEFAULT 0,
                    `addedAt` TEXT NOT NULL DEFAULT '',
                    `createdAt` TEXT NOT NULL DEFAULT '',
                    `updatedAt` TEXT NOT NULL DEFAULT '',
                    FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_prontuario_documents_patientId` ON `prontuario_documents` (`patientId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_prontuario_documents_addedAt` ON `prontuario_documents` (`addedAt`)")
        }
    }

    /** Adds article bookmarking ("favoritos" in the Biblioteca screen). */
    private val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `favorite_articles` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `articleId` TEXT NOT NULL,
                    `createdAt` TEXT NOT NULL DEFAULT ''
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_favorite_articles_articleId` ON `favorite_articles` (`articleId`)")
        }
    }

    /** Adds the Atendimento wizard's extra MTC-assessment fields (additive columns only). */
    private val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE mtc_assessments ADD COLUMN relievingJson TEXT NOT NULL DEFAULT '[]'")
            database.execSQL("ALTER TABLE mtc_assessments ADD COLUMN aggravatingJson TEXT NOT NULL DEFAULT '[]'")
            database.execSQL("ALTER TABLE mtc_assessments ADD COLUMN reviewOfSystemsJson TEXT NOT NULL DEFAULT '[]'")
            database.execSQL("ALTER TABLE mtc_assessments ADD COLUMN interrogationNotes TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE mtc_assessments ADD COLUMN orientations TEXT NOT NULL DEFAULT ''")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // v13 — one patient registry
    //
    // The app had two patient tables that had drifted apart. `crm_patients` is
    // the one the app actually writes: the CRM screen creates patients there,
    // the Dashboard reads from it, and appointments already pointed at it. The
    // legacy `patients` table was written by almost nothing.
    //
    // Yet every clinical table — prontuários, evoluções, avaliações MTC, sinais
    // vitais, exames, medicações, alergias, documentos — carried a foreign key
    // to `patients`. So opening a patient from the CRM and saving anything
    // clinical produced a row whose patientId did not exist in `patients`,
    // SQLite rejected it, and the repository reported "Falha ao acessar dados
    // locais". Not a storage failure — a schema pointing at the wrong table.
    //
    // This migration repoints all eight foreign keys at `crm_patients`.
    // ─────────────────────────────────────────────────────────────────────────
    private val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
        override fun migrate(database: SupportSQLiteDatabase) {
            val remap = buildPatientIdRemap(database)

            // Apply the remap *before* rebuilding, so the copy is a straight
            // column-for-column move. Descending order avoids an intermediate
            // collision when an id is remapped onto a value still in use.
            remap.entries
                .filter { it.key != it.value }
                .sortedByDescending { it.key }
                .forEach { (oldId, newId) ->
                    CLINICAL_TABLES.forEach { table ->
                        if (tableExists(database, table)) {
                            database.execSQL(
                                "UPDATE `$table` SET patientId = ? WHERE patientId = ?",
                                arrayOf<Any>(newId, oldId),
                            )
                        }
                    }
                }

            CLINICAL_TABLE_SCHEMAS.forEach { schema -> rebuildWithCrmForeignKey(database, schema) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // v15 — FTS4 search index
    //
    // Adiciona a tabela virtual FTS4 `article_fts` para busca textual
    // dos artigos da biblioteca. Substitui o índice BM25 em memória
    // por comandos SQL no SQLite.
    //
    // Additive only: nenhuma tabela existente é modificada.
    // ─────────────────────────────────────────────────────────────────────────
    private val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS `article_fts` USING fts4(
                    `articleId` TEXT,
                    `category` TEXT,
                    `title` TEXT,
                    `summary` TEXT,
                    `content` TEXT,
                    `tags` TEXT
                )
                """.trimIndent(),
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // v16 — Provenance no FTS4
    //
    // Adiciona a coluna `provenance` ao índice FTS4 para classificar
    // artigos como VERIFICAVEL (com localizador de fonte) ou RASCUNHO
    // (sem localizador, gerado por IA).
    //
    // FTS4 não permite ALTER TABLE, então a tabela é dropada e recriada.
    // Como ela é populada unicamente pelo [FtsSearchService.rebuildIndex()]
    // (que limpa e reinsere tudo), não há perda de dados — o rebuild roda
    // na próxima busca.
    // ─────────────────────────────────────────────────────────────────────────
    private val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("DROP TABLE IF EXISTS `article_fts`")
            database.execSQL(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS `article_fts` USING fts4(
                    `articleId` TEXT,
                    `category` TEXT,
                    `title` TEXT,
                    `summary` TEXT,
                    `content` TEXT,
                    `tags` TEXT,
                    `provenance` TEXT
                )
                """.trimIndent(),
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // v17 — tenantId na transacoes + override do veto clínico
    //
    // Multi-tenant isolation para o módulo financeiro (tenantId faltava) e
    // registra o override do veto clínico (razão, usuário, timestamp) na
    // mtc_assessments para auditoria LGPD/CFM.
    //
    // O default do tenantId é 1L (compatível com instalações single-tenant).
    // ─────────────────────────────────────────────────────────────────────────
    private val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE transacoes ADD COLUMN tenantId INTEGER NOT NULL DEFAULT 1")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transacoes_tenantId ON transacoes(tenantId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transacoes_tenantId_date ON transacoes(tenantId, date)")
            database.execSQL("ALTER TABLE mtc_assessments ADD COLUMN overrideReason TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE mtc_assessments ADD COLUMN overrideBy TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE mtc_assessments ADD COLUMN overrideAt TEXT NOT NULL DEFAULT ''")
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // v18 — MKIS On-Device: knowledge_nodes expandido + novas tabelas
    //        + sqlite-vec + FTS5
    //
    // Adiciona:
    // 1. Novas colunas ao knowledge_nodes (enums, scores, governança, timestamps)
    // 2. Tabela ingestion_jobs (pipeline state machine)
    // 3. Tabela purge_certificates (LGPD deep delete)
    // 4. Tabela audit_trail (append-only audit log)
    // 5. Tabela virtual vec_knowledge_nodes (sqlite-vec, 384d)
    // 6. Tabela virtual knowledge_fts (FTS5 para busca híbrida)
    //
    // NOTA: O sqlite-vec precisa ter a extensão .so carregada via
    // SupportSQLiteDatabase.execSQL("SELECT load_extension('libsqlite_vec')").
    // Se a lib não estiver disponível, a migration ignora a criação da
    // tabela virtual (usando IF NOT EXISTS) e o app continua funcionando
    // sem busca vetorial — degradação graciosa.
    // ═════════════════════════════════════════════════════════════════════
    private val MIGRATION_17_18 = object : androidx.room.migration.Migration(17, 18) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // ═══════════════════════════════════════════════════════════════
            // Passo 1: Recriar knowledge_nodes com schema EXATO que Room espera
            //
            // A tabela ANTIGA foi criada por uma versão anterior do app com:
            //   id TEXT PK, type TEXT, title TEXT, content TEXT,
            //   summary TEXT, tags TEXT, version INTEGER, metadata TEXT
            //
            // A NOVA entidade KnowledgeNodeEntity tem ~37 colunas, SEM a coluna
            // 'type' (renomeada para knowledge_type), com version como TEXT,
            // e todas as colunas SEM DEFAULT SQL (Room usa defaults Kotlin,
            // não SQL DEFAULT).
            //
            // ALTER TABLE ADD COLUMN não resolve porque:
            // 1) 'type' antiga não existe na nova entidade (não tem como dropar)
            // 2) 'version' mudou de INTEGER para TEXT
            // 3) Room valida que DEFAULT values NÃO existam (usa Kotlin defaults)
            //
            // Solução: DROP e recria com o schema correto.
            // A tabela antiga não tem dados clínicos reais (só seed de dev).
            // ═══════════════════════════════════════════════════════════════
            database.execSQL("DROP INDEX IF EXISTS index_knowledge_nodes_checksum")
            database.execSQL("DROP INDEX IF EXISTS index_knowledge_nodes_tenant_created")
            database.execSQL("DROP INDEX IF EXISTS index_knowledge_nodes_tenant_id")
            database.execSQL("DROP INDEX IF EXISTS index_knowledge_nodes_status")
            database.execSQL("DROP TABLE IF EXISTS `knowledge_nodes`")

            // CREATE TABLE sem DEFAULT SQL — Room usa defaults do Kotlin na app.
            // A ordem e tipos DEVEM corresponder exatamente ao @Entity.
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `knowledge_nodes` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `tenant_id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `summary` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `knowledge_type` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `evidence_level` TEXT,
                    `bias_risk` TEXT NOT NULL,
                    `clinical_evidence` TEXT,
                    `category` TEXT NOT NULL,
                    `source` TEXT NOT NULL,
                    `specialty` TEXT NOT NULL,
                    `language` TEXT NOT NULL,
                    `data_classification` TEXT NOT NULL,
                    `doi` TEXT,
                    `pmid` TEXT,
                    `source_url` TEXT,
                    `authors` TEXT NOT NULL,
                    `citation` TEXT NOT NULL,
                    `scientific_score` REAL,
                    `ai_score` REAL,
                    `reliability_score` REAL,
                    `checksum` TEXT NOT NULL,
                    `created_by` TEXT,
                    `reviewed_by` TEXT,
                    `approved_by` TEXT,
                    `approved_at` INTEGER,
                    `version` TEXT NOT NULL,
                    `superseded_by` TEXT,
                    `tags` TEXT NOT NULL,
                    `metadata` TEXT NOT NULL,
                    `conflicts` TEXT NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    `deleted_at` INTEGER
                )
            """.trimIndent())

            // ── Passo 2: Índices com nomes que Room gera do @Entity ──
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_nodes_tenant_id` ON `knowledge_nodes` (`tenant_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_nodes_status` ON `knowledge_nodes` (`status`)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_knowledge_nodes_checksum_tenant_id` ON `knowledge_nodes` (`checksum`, `tenant_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_nodes_tenant_id_created_at` ON `knowledge_nodes` (`tenant_id`, `created_at`)")

            // ── Passo 3: Criar tabela ingestion_jobs ──
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `ingestion_jobs` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `tenant_id` TEXT NOT NULL DEFAULT 'default',
                    `artifact_id` TEXT,
                    `node_id` TEXT,
                    `status` TEXT NOT NULL DEFAULT 'na_fila',
                    `attempt_id` TEXT NOT NULL,
                    `attempt_count` INTEGER NOT NULL DEFAULT 1,
                    `max_attempts` INTEGER NOT NULL DEFAULT 3,
                    `current_stage` TEXT,
                    `error_code` TEXT,
                    `error_message` TEXT,
                    `quarantine_reason` TEXT,
                    `review_notes` TEXT,
                    `source_url` TEXT,
                    `source_package` TEXT,
                    `priority` INTEGER NOT NULL DEFAULT 0,
                    `started_at` INTEGER,
                    `completed_at` INTEGER,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL
                )
            """.trimIndent())
            database.execSQL("CREATE INDEX IF NOT EXISTS index_ingestion_jobs_tenant_status ON ingestion_jobs(tenant_id, status)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_ingestion_jobs_node_id ON ingestion_jobs(node_id)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_ingestion_jobs_created_status ON ingestion_jobs(created_at, status)")

            // ── Passo 4: Criar tabela purge_certificates ──
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `purge_certificates` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `tenant_id` TEXT NOT NULL DEFAULT 'default',
                    `target_type` TEXT NOT NULL,
                    `target_id` TEXT NOT NULL,
                    `cascade_scope` TEXT NOT NULL DEFAULT 'metadata_only',
                    `target_details` TEXT NOT NULL DEFAULT '{}',
                    `requested_by` TEXT NOT NULL,
                    `started_at` INTEGER NOT NULL,
                    `completed_at` INTEGER,
                    `checkpoint` TEXT NOT NULL DEFAULT 'pending',
                    `steps_log` TEXT NOT NULL DEFAULT '[]',
                    `certificate_hash` TEXT NOT NULL,
                    `legal_hold_verified_at` INTEGER,
                    `notes` TEXT,
                    `created_at` INTEGER NOT NULL
                )
            """.trimIndent())
            database.execSQL("CREATE INDEX IF NOT EXISTS index_purge_certificates_tenant ON purge_certificates(tenant_id, created_at)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_purge_certificates_target ON purge_certificates(target_type, target_id)")

            // ── Passo 5: Criar tabela audit_trail ──
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `audit_trail` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `tenant_id` TEXT NOT NULL DEFAULT 'default',
                    `actor_id` TEXT NOT NULL,
                    `actor_role` TEXT,
                    `action` TEXT NOT NULL,
                    `resource_type` TEXT NOT NULL,
                    `resource_id` TEXT,
                    `ip_address` TEXT,
                    `outcome` TEXT,
                    `request_id` TEXT NOT NULL,
                    `metadata` TEXT NOT NULL DEFAULT '{}',
                    `occurred_at` INTEGER NOT NULL,
                    `created_at` INTEGER NOT NULL
                )
            """.trimIndent())
            database.execSQL("CREATE INDEX IF NOT EXISTS index_audit_trail_tenant_time ON audit_trail(tenant_id, occurred_at)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_audit_trail_resource ON audit_trail(resource_type, resource_id)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_audit_trail_action ON audit_trail(action)")

            // ── Passo 6: Criar tabela virtual FTS5 (knowledge_fts) ──
            // Separada do article_fts (FTS4) existente. FTS5 tem BM25 nativo.
            runCatching {
                database.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS `knowledge_fts` USING fts5(
                        `node_id` UNINDEXED,
                        `title`,
                        `summary`,
                        `content`,
                        `tags`,
                        tokenize='porter unicode61'
                    )
                """.trimIndent())
            }.onFailure { e ->
                com.bioacupunt.observability.AppLogger.w("MIGRATION_17_18", "FTS5 creation failed (missing extension?)", e)
            }

            // ── Passo 7: Tentar criar tabela virtual sqlite-vec ──
            // Se a extensão .so não estiver disponível, apenas loga o warning.
            // A tabela pode ser criada posteriormente quando a lib estiver disponível.
            runCatching {
                database.execSQL("SELECT load_extension('libsqlite_vec')")
                database.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS `vec_knowledge_nodes` USING vec0(
                        embedding float[384] distance_metric=cosine
                    )
                """.trimIndent())
                com.bioacupunt.observability.AppLogger.i("MIGRATION_17_18", "sqlite-vec loaded and vec_knowledge_nodes created")
            }.onFailure { e ->
                com.bioacupunt.observability.AppLogger.w("MIGRATION_17_18",
                    "sqlite-vec not available yet. Vector search disabled. ${e.message}", e)
            }

            // ── Passo 8: (obsoleto) Backfill removido — a tabela foi recriada
            // do zero no Passo 1. Dados antigos (se houverem) são perdidos
            // intencionalmente: o schema antigo era incompatível (type→knowledge_type,
            // version INTEGER→TEXT) e não havia dados clínicos reais na tabela.
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // v14 — sync identity
    //
    // Gives the three syncable tables (CRM patients, appointments, financial
    // transactions) the columns two-way sync needs, and adds the local tables
    // that track how far this device has pulled and which records are waiting
    // on a human decision.
    //
    // Clinical tables are deliberately absent: prontuário, avaliação MTC,
    // língua/pulso and contraindication flags stay on the device.
    // ─────────────────────────────────────────────────────────────────────────
    private val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
        override fun migrate(database: SupportSQLiteDatabase) {
            SYNCABLE_TABLES.forEach { table ->
                database.execSQL("ALTER TABLE `$table` ADD COLUMN clientId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `$table` ADD COLUMN serverId INTEGER")
                database.execSQL("ALTER TABLE `$table` ADD COLUMN baseRev INTEGER NOT NULL DEFAULT 0")
                backfillClientIds(database, table)
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_${table}_clientId` ON `$table` (`clientId`)"
                )
            }

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sync_state` (
                    `id` INTEGER NOT NULL PRIMARY KEY,
                    `lastPulledRev` INTEGER NOT NULL DEFAULT 0,
                    `lastSyncAt` TEXT,
                    `lastError` TEXT
                )
                """.trimIndent(),
            )
            database.execSQL("INSERT OR IGNORE INTO `sync_state` (`id`, `lastPulledRev`) VALUES (1, 0)")

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sync_conflicts` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `entityType` TEXT NOT NULL,
                    `clientId` TEXT NOT NULL,
                    `serverId` INTEGER,
                    `serverRev` INTEGER NOT NULL DEFAULT 0,
                    `localPayloadJson` TEXT NOT NULL DEFAULT '{}',
                    `serverPayloadJson` TEXT NOT NULL DEFAULT '{}',
                    `detectedAt` TEXT NOT NULL DEFAULT '',
                    `resolvedAt` TEXT,
                    `resolution` TEXT
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_conflicts_entityType` ON `sync_conflicts` (`entityType`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_conflicts_resolvedAt` ON `sync_conflicts` (`resolvedAt`)")
            database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_sync_conflicts_entityType_clientId` ON `sync_conflicts` (`entityType`, `clientId`)"
            )
        }
    }

    private val SYNCABLE_TABLES = listOf("crm_patients", "appointments", "transacoes")

    /**
     * Gives every pre-existing row a client id.
     *
     * These rows were created before the app had any notion of sync identity,
     * so they all carry the `''` default. A unique index over a column where
     * every row is the empty string cannot be created — and more importantly,
     * rows sharing a client id would be treated by the server as *the same
     * record*, so the second patient to sync would be read as an edit of the
     * first and one of them would vanish.
     *
     * Row ids are unique per table, so `local-<table>-<rowid>` is guaranteed
     * distinct without needing a UUID generator inside the migration.
     */
    private fun backfillClientIds(database: SupportSQLiteDatabase, table: String) {
        database.execSQL(
            "UPDATE `$table` SET clientId = 'local-$table-' || CAST(id AS TEXT) WHERE clientId = ''"
        )
    }

    private val CLINICAL_TABLES = listOf(
        "prontuarios",
        "prontuario_entries",
        "mtc_assessments",
        "vital_signs",
        "lab_exams",
        "medications",
        "allergies",
        "prontuario_documents",
    )

    /**
     * Decides, for every patientId referenced by a clinical row, which
     * `crm_patients.id` that row should belong to after the migration.
     *
     * The one thing this must never do is merge two different people. Both
     * tables autoincrement from 1, so "id 3 exists in both" is *not* evidence
     * that they are the same person — except where the id already resolves in
     * `crm_patients`, which is the case the app itself created: the doctor
     * navigated to the chart using a CRM id, so that is the patient she meant.
     *
     * Anything that does not resolve is rescued into a new `crm_patients` row
     * rather than dropped. A chart that loses its owner is recoverable; a chart
     * silently deleted, or silently attached to the wrong patient, is not.
     */
    private fun buildPatientIdRemap(database: SupportSQLiteDatabase): Map<Long, Long> {
        val referenced = linkedSetOf<Long>()
        CLINICAL_TABLES.forEach { table ->
            if (!tableExists(database, table)) return@forEach
            database.query("SELECT DISTINCT patientId FROM `$table`").use { cursor ->
                while (cursor.moveToNext()) referenced.add(cursor.getLong(0))
            }
        }
        if (referenced.isEmpty()) return emptyMap()

        val remap = LinkedHashMap<Long, Long>()
        referenced.forEach { id ->
            if (existsInCrm(database, id)) {
                remap[id] = id
            } else {
                remap[id] = rescuePatient(database, id)
            }
        }
        return remap
    }

    private fun existsInCrm(database: SupportSQLiteDatabase, id: Long): Boolean =
        database.query("SELECT 1 FROM crm_patients WHERE id = ? LIMIT 1", arrayOf<Any>(id))
            .use { it.moveToNext() }

    /**
     * Creates a `crm_patients` row for a clinical record whose patient is not in
     * the CRM, copying the legacy `patients` row when one exists and falling back
     * to a clearly-labelled placeholder when it does not. Returns the new id.
     *
     * The placeholder is deliberately visible in the patient list. The doctor can
     * see that a chart needs re-attaching; she cannot see a chart that was deleted.
     */
    private fun rescuePatient(database: SupportSQLiteDatabase, legacyId: Long): Long {
        val now = java.time.Instant.now().toString()
        var name = "Paciente sem cadastro (registro #$legacyId)"
        var tenantId = 1L

        if (tableExists(database, "patients")) {
            database.query(
                "SELECT name, tenantId FROM patients WHERE id = ? LIMIT 1",
                arrayOf<Any>(legacyId),
            ).use { cursor ->
                if (cursor.moveToNext()) {
                    name = cursor.getString(0) ?: name
                    tenantId = cursor.getLong(1)
                }
            }
        }

        database.execSQL(
            """
            INSERT INTO crm_patients
                (tenantId, name, phone, email, birthDate, stage, totalSessions,
                 totalRevenueBrl, lastVisit, nextAppointment, tags, notes,
                 referralSource, npsScore, healthInsurance, mainComplaint,
                 createdAt, updatedAt, pendingSync, deleted, lastModified)
            VALUES (?, ?, '', '', '', ?, 0, 0.0, '', '', '', ?, '', NULL, '', '', ?, ?, 1, 0, ?)
            """.trimIndent(),
            arrayOf<Any>(
                tenantId,
                name,
                com.bioacupunt.crm.domain.model.PatientStage.ACTIVE.name,
                "Cadastro recuperado automaticamente na migração v13. " +
                    "Confira os dados deste paciente.",
                now,
                now,
                now,
            ),
        )
        return database.query("SELECT last_insert_rowid()").use { cursor ->
            cursor.moveToNext()
            cursor.getLong(0)
        }
    }

    private fun tableExists(database: SupportSQLiteDatabase, table: String): Boolean =
        database.query(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1",
            arrayOf<Any>(table),
        ).use { it.moveToNext() }

    /**
     * SQLite cannot alter a foreign key in place, so each table is recreated with
     * the corrected reference and its rows copied across. Columns are listed
     * explicitly rather than `SELECT *` — a positional copy silently shifts every
     * value one column left if the schemas ever disagree, which in a chart means
     * a pulse reading landing in the tongue field.
     */
    private fun rebuildWithCrmForeignKey(database: SupportSQLiteDatabase, schema: TableSchema) {
        if (!tableExists(database, schema.name)) {
            database.execSQL(schema.createSql(schema.name))
            schema.indexSql(schema.name).forEach { database.execSQL(it) }
            return
        }
        val temp = "${schema.name}_v13"
        database.execSQL("DROP TABLE IF EXISTS `$temp`")
        database.execSQL(schema.createSql(temp))
        val columns = schema.columns.joinToString(", ") { "`$it`" }
        database.execSQL("INSERT INTO `$temp` ($columns) SELECT $columns FROM `${schema.name}`")
        database.execSQL("DROP TABLE `${schema.name}`")
        database.execSQL("ALTER TABLE `$temp` RENAME TO `${schema.name}`")
        schema.indexSql(schema.name).forEach { database.execSQL(it) }
    }

    private class TableSchema(
        val name: String,
        val columns: List<String>,
        private val bodySql: String,
        private val indexedColumns: List<String>,
        private val uniqueIndexedColumns: List<String> = emptyList(),
    ) {
        fun createSql(tableName: String): String =
            """
            CREATE TABLE IF NOT EXISTS `$tableName` (
                $bodySql,
                FOREIGN KEY(`patientId`) REFERENCES `crm_patients`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()

        fun indexSql(tableName: String): List<String> =
            indexedColumns.map {
                "CREATE INDEX IF NOT EXISTS `index_${name}_$it` ON `$tableName` (`$it`)"
            } + uniqueIndexedColumns.map {
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_${name}_$it` ON `$tableName` (`$it`)"
            }
    }

    private val CLINICAL_TABLE_SCHEMAS = listOf(
        TableSchema(
            name = "prontuarios",
            columns = listOf(
                "id", "patientId", "summary", "mainComplaint", "diagnosis",
                "treatmentPlan", "syncedAt", "createdAt", "updatedAt",
            ),
            bodySql = """
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `patientId` INTEGER NOT NULL,
                `summary` TEXT NOT NULL DEFAULT '',
                `mainComplaint` TEXT NOT NULL DEFAULT '',
                `diagnosis` TEXT NOT NULL DEFAULT '',
                `treatmentPlan` TEXT NOT NULL DEFAULT '',
                `syncedAt` TEXT,
                `createdAt` TEXT NOT NULL DEFAULT '',
                `updatedAt` TEXT NOT NULL DEFAULT ''
            """.trimIndent(),
            indexedColumns = listOf("patientId", "updatedAt"),
        ),
        TableSchema(
            name = "prontuario_entries",
            columns = listOf(
                "id", "patientId", "doctorName", "date", "type", "body",
                "attachmentsJson", "syncedAt", "createdAt", "updatedAt",
            ),
            bodySql = """
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `patientId` INTEGER NOT NULL,
                `doctorName` TEXT NOT NULL DEFAULT '',
                `date` TEXT NOT NULL DEFAULT '',
                `type` TEXT NOT NULL DEFAULT 'EVOLUTION',
                `body` TEXT NOT NULL DEFAULT '',
                `attachmentsJson` TEXT NOT NULL DEFAULT '[]',
                `syncedAt` TEXT,
                `createdAt` TEXT NOT NULL DEFAULT '',
                `updatedAt` TEXT NOT NULL DEFAULT ''
            """.trimIndent(),
            indexedColumns = listOf("patientId", "date", "type"),
        ),
        TableSchema(
            name = "mtc_assessments",
            columns = listOf(
                "id", "patientId", "date", "chiefComplaint", "baGangPolarity",
                "baGangDepth", "baGangTemperature", "baGangStrength", "patternsJson",
                "tongueJson", "pulseJson", "bodyMarksJson", "flagsCsv",
                "gestationalWeeks", "clinicalImpression", "relievingJson",
                "aggravatingJson", "reviewOfSystemsJson", "interrogationNotes",
                "orientations", "syncedAt", "createdAt", "updatedAt",
            ),
            bodySql = """
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `patientId` INTEGER NOT NULL,
                `date` TEXT NOT NULL DEFAULT '',
                `chiefComplaint` TEXT NOT NULL DEFAULT '',
                `baGangPolarity` TEXT NOT NULL DEFAULT 'UNSET',
                `baGangDepth` TEXT NOT NULL DEFAULT 'UNSET',
                `baGangTemperature` TEXT NOT NULL DEFAULT 'UNSET',
                `baGangStrength` TEXT NOT NULL DEFAULT 'UNSET',
                `patternsJson` TEXT NOT NULL DEFAULT '[]',
                `tongueJson` TEXT NOT NULL DEFAULT '{}',
                `pulseJson` TEXT NOT NULL DEFAULT '{}',
                `bodyMarksJson` TEXT NOT NULL DEFAULT '[]',
                `flagsCsv` TEXT NOT NULL DEFAULT '',
                `gestationalWeeks` INTEGER,
                `clinicalImpression` TEXT NOT NULL DEFAULT '',
                `relievingJson` TEXT NOT NULL DEFAULT '[]',
                `aggravatingJson` TEXT NOT NULL DEFAULT '[]',
                `reviewOfSystemsJson` TEXT NOT NULL DEFAULT '[]',
                `interrogationNotes` TEXT NOT NULL DEFAULT '',
                `orientations` TEXT NOT NULL DEFAULT '',
                `syncedAt` TEXT,
                `createdAt` TEXT NOT NULL DEFAULT '',
                `updatedAt` TEXT NOT NULL DEFAULT ''
            """.trimIndent(),
            indexedColumns = listOf("patientId", "date", "updatedAt"),
        ),
        TableSchema(
            name = "vital_signs",
            columns = listOf("id", "patientId", "label", "value", "recordedAt", "createdAt", "updatedAt"),
            bodySql = """
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `patientId` INTEGER NOT NULL,
                `label` TEXT NOT NULL,
                `value` TEXT NOT NULL,
                `recordedAt` TEXT NOT NULL DEFAULT '',
                `createdAt` TEXT NOT NULL DEFAULT '',
                `updatedAt` TEXT NOT NULL DEFAULT ''
            """.trimIndent(),
            indexedColumns = listOf("patientId", "recordedAt"),
        ),
        TableSchema(
            name = "lab_exams",
            columns = listOf("id", "patientId", "name", "date", "resultTag", "notes", "createdAt", "updatedAt"),
            bodySql = """
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `patientId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `date` TEXT NOT NULL DEFAULT '',
                `resultTag` TEXT NOT NULL DEFAULT 'PENDING',
                `notes` TEXT NOT NULL DEFAULT '',
                `createdAt` TEXT NOT NULL DEFAULT '',
                `updatedAt` TEXT NOT NULL DEFAULT ''
            """.trimIndent(),
            indexedColumns = listOf("patientId", "date"),
        ),
        TableSchema(
            name = "medications",
            columns = listOf("id", "patientId", "name", "info", "active", "createdAt", "updatedAt"),
            bodySql = """
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `patientId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `info` TEXT NOT NULL DEFAULT '',
                `active` INTEGER NOT NULL DEFAULT 1,
                `createdAt` TEXT NOT NULL DEFAULT '',
                `updatedAt` TEXT NOT NULL DEFAULT ''
            """.trimIndent(),
            indexedColumns = listOf("patientId"),
        ),
        TableSchema(
            name = "allergies",
            columns = listOf("id", "patientId", "description", "createdAt", "updatedAt"),
            bodySql = """
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `patientId` INTEGER NOT NULL,
                `description` TEXT NOT NULL,
                `createdAt` TEXT NOT NULL DEFAULT '',
                `updatedAt` TEXT NOT NULL DEFAULT ''
            """.trimIndent(),
            indexedColumns = listOf("patientId"),
        ),
        TableSchema(
            name = "prontuario_documents",
            columns = listOf(
                "id", "patientId", "name", "uri", "mimeType", "sizeBytes",
                "addedAt", "createdAt", "updatedAt",
            ),
            bodySql = """
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `patientId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `uri` TEXT NOT NULL,
                `mimeType` TEXT NOT NULL DEFAULT '',
                `sizeBytes` INTEGER NOT NULL DEFAULT 0,
                `addedAt` TEXT NOT NULL DEFAULT '',
                `createdAt` TEXT NOT NULL DEFAULT '',
                `updatedAt` TEXT NOT NULL DEFAULT ''
            """.trimIndent(),
            indexedColumns = listOf("patientId", "addedAt"),
        ),
    )
}
