package com.bioacupunt.pharma.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicamentoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MedicamentoEntity>)

    @Query("SELECT COUNT(*) FROM medicamentos")
    suspend fun count(): Int

    @Query("SELECT * FROM medicamentos WHERE id = :id")
    suspend fun getById(id: String): MedicamentoEntity?

    @Query("SELECT * FROM medicamentos WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<MedicamentoEntity>

    @Query(
        """
        SELECT classeTerapeutica, COUNT(*) as count FROM medicamentos
        WHERE classeTerapeutica != '' AND situacaoAtiva = 1
        GROUP BY classeTerapeutica
        ORDER BY classeTerapeutica ASC
        """
    )
    suspend fun listClasses(): List<ClasseTerapeuticaCountRow>

    @Query(
        """
        SELECT * FROM medicamentos
        WHERE classeTerapeutica = :classe AND situacaoAtiva = 1
        ORDER BY nomeComercial ASC
        LIMIT :limit
        """
    )
    suspend fun getByClasse(classe: String, limit: Int): List<MedicamentoEntity>
}

/** Projeção de `GROUP BY` — Room mapeia por nome de campo, não precisa de `@ColumnInfo` aqui. */
data class ClasseTerapeuticaCountRow(
    val classeTerapeutica: String,
    val count: Int,
)

@Dao
interface MedicamentoFtsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MedicamentoFtsEntity>)

    @Query("SELECT count(*) FROM medicamentos_fts")
    suspend fun count(): Int

    /**
     * MATCH pode lançar `SQLiteException` pra sintaxe inválida (ex.: query terminando
     * em operador) — o repositório envolve isto em `runCatching` e degrada pra lista
     * vazia, nunca crasha a tela de busca.
     */
    @Query(
        """
        SELECT * FROM medicamentos_fts
        WHERE medicamentos_fts MATCH :query
        LIMIT :limit
        """
    )
    suspend fun search(query: String, limit: Int = 30): List<MedicamentoFtsEntity>
}

@Dao
interface FormularioMedicamentoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: FormularioMedicamentoEntity)

    @Query("SELECT * FROM formulario_medicamento WHERE medicamentoId = :medicamentoId AND tenantId = :tenantId")
    suspend fun getById(medicamentoId: String, tenantId: Long): FormularioMedicamentoEntity?

    @Query("SELECT * FROM formulario_medicamento WHERE tenantId = :tenantId AND status = 'APROVADO'")
    fun observeApproved(tenantId: Long): Flow<List<FormularioMedicamentoEntity>>

    @Query(
        "SELECT * FROM formulario_medicamento WHERE medicamentoId IN (:ids) AND tenantId = :tenantId AND status = 'APROVADO'"
    )
    suspend fun getApprovedByIds(ids: List<String>, tenantId: Long): List<FormularioMedicamentoEntity>
}

@Dao
interface PrescricaoDao {
    @Query("SELECT * FROM prescricoes WHERE patientId = :patientId AND tenantId = :tenantId AND active = 1 ORDER BY id DESC")
    fun observeActiveByPatient(patientId: Long, tenantId: Long): Flow<List<PrescricaoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: PrescricaoEntity): Long

    @Query("UPDATE prescricoes SET active = 0 WHERE id = :id AND tenantId = :tenantId")
    suspend fun deactivate(id: Long, tenantId: Long)
}
