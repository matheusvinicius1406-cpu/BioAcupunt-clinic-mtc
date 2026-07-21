package com.bioacupunt.prontuario.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.bioacupunt.prontuario.domain.model.Prontuario
import com.bioacupunt.prontuario.domain.model.ProntuarioEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ProntuarioDao {
    @Query("SELECT * FROM prontuarios WHERE patientId = :pid LIMIT 1")
    fun observe(pid: Long): Flow<ProntuarioEntity?>

    /** Id da linha de prontuário já existente para esta paciente (para upsert real). */
    @Query("SELECT id FROM prontuarios WHERE patientId = :pid LIMIT 1")
    suspend fun findId(pid: Long): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: ProntuarioEntity): Long

    @Query("SELECT * FROM prontuario_entries WHERE patientId = :pid ORDER BY date DESC, id DESC")
    fun observeEntries(pid: Long): Flow<List<ProntuarioEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEntry(entity: ProntuarioEntryEntity): Long

    @Query("DELETE FROM prontuario_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query("SELECT COUNT(*) FROM prontuario_entries WHERE patientId = :pid")
    suspend fun countEntries(pid: Long): Int
}
