package com.bioacupunt.data

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.bioacupunt.di.DatabaseModule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Guarda a migração 19 → 20 (Farmacologia). O risco real aqui é o mesmo do resto do
 * app: `formulario_medicamento` tem chave composta (medicamentoId, tenantId) — sem
 * ela, duas curadorias do mesmo medicamento no mesmo tenant poderiam duplicar em vez
 * de substituir, e o PharmaSafetyEngine passaria a ver dois formulários conflitantes
 * pro mesmo item.
 */
@RunWith(RobolectricTestRunner::class)
class AnvisaMigrationTest {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getDatabasePath(DB_NAME).takeIf { it.exists() }?.delete()
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(DB_NAME)
                .callback(object : SupportSQLiteOpenHelper.Callback(19) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) = Unit
                })
                .build()
        )
        db = helper.writableDatabase
    }

    @After
    fun tearDown() = helper.close()

    @Test
    fun `all four tables exist after the migration`() {
        runMigration19to20()

        val tables = mutableSetOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type IN ('table', 'view')").use { cursor ->
            while (cursor.moveToNext()) tables.add(cursor.getString(0))
        }
        assertTrue(tables.contains("medicamentos"))
        assertTrue(tables.contains("medicamentos_fts"))
        assertTrue(tables.contains("formulario_medicamento"))
        assertTrue(tables.contains("prescricoes"))
    }

    @Test
    fun `a medicamento can be inserted and found via FTS`() {
        runMigration19to20()

        db.execSQL(
            "INSERT INTO medicamentos (id, nomeComercial, principiosAtivosCsv, classeTerapeutica, categoriaRegulatoria, empresaDetentora, situacaoAtiva) " +
                "VALUES ('REG-1', 'Novalgina', 'dipirona sodica', 'Analgesico', 'REFERENCIA', 'Fabricante', 1)"
        )
        db.execSQL(
            "INSERT INTO medicamentos_fts (medicamentoId, nomeComercial, principiosAtivosCsv, classeTerapeutica) " +
                "VALUES ('REG-1', 'Novalgina', 'dipirona sodica', 'Analgesico')"
        )

        db.query("SELECT COUNT(*) FROM medicamentos_fts WHERE medicamentos_fts MATCH 'noval*'").use { cursor ->
            cursor.moveToNext()
            assertEquals(1, cursor.getInt(0))
        }
    }

    @Test
    fun `the composite primary key rejects a duplicate medicamentoId plus tenantId`() {
        runMigration19to20()
        insertFormulario(medicamentoId = "REG-1", tenantId = 1)

        val threw = runCatching { insertFormulario(medicamentoId = "REG-1", tenantId = 1) }.isFailure

        assertTrue("duas curadorias do mesmo medicamento no mesmo tenant não podem duplicar a linha", threw)
    }

    @Test
    fun `the same medicamentoId is allowed for a different tenant`() {
        runMigration19to20()
        insertFormulario(medicamentoId = "REG-1", tenantId = 1)

        val threw = runCatching { insertFormulario(medicamentoId = "REG-1", tenantId = 2) }.isFailure

        assertTrue("a chave é composta (medicamentoId, tenantId) — outro tenant não pode ficar bloqueado", !threw)
    }

    @Test
    fun `a prescricao can be inserted`() {
        runMigration19to20()

        db.execSQL(
            "INSERT INTO prescricoes (patientId, tenantId, medicamentoId, medicamentoNomeLivre, dose, frequencia, duracao, viaAdministracao, observacoes, prescritoPor, prescritoEm, active, overrideReason, overrideBy, overrideAt) " +
                "VALUES (1, 1, 'REG-1', '', '500mg', '6/6h', '5 dias', 'Oral', '', 'dra', '2026-07-27T00:00:00Z', 1, '', '', '')"
        )

        db.query("SELECT COUNT(*) FROM prescricoes WHERE active = 1").use { cursor ->
            cursor.moveToNext()
            assertEquals(1, cursor.getInt(0))
        }
    }

    private fun insertFormulario(medicamentoId: String, tenantId: Long) {
        db.execSQL(
            "INSERT INTO formulario_medicamento (medicamentoId, tenantId, posologiaAdulto, posologiaPediatrica, posologiaIdoso, posologiaRenal, posologiaHepatica, viaAdministracao, contraindicacoesAbsolutasCsv, contraindicacoesRelativasCsv, alergenosCsv, interacoesJson, efeitosAdversosJson, visaoIntegrativaMtc, status, autor, atualizadoEm) " +
                "VALUES (?, ?, '500mg', '', '', '', '', 'Oral', '', '', '', '[]', '[]', '', 'RASCUNHO', '', '')",
            arrayOf<Any>(medicamentoId, tenantId),
        )
    }

    private fun runMigration19to20() {
        val migration = DatabaseModule.migrations()
            .first { it.startVersion == 19 && it.endVersion == 20 }
        db.beginTransaction()
        try {
            migration.migrate(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private companion object {
        const val DB_NAME = "migration_v20_test.db"
    }
}
