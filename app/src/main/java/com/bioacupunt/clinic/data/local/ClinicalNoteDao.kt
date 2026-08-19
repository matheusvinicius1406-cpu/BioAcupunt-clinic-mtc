package com.bioacupunt.clinic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ClinicalNoteDao {
    @Query("SELECT * FROM clinical_notes WHERE id = :id AND deleted = 0")
    suspend fun getById(id: Long): ClinicalNoteEntity?

    @Query("SELECT * FROM clinical_notes WHERE encounterId = :encounterId AND deleted = 0")
    suspend fun getByEncounterId(encounterId: Long): ClinicalNoteEntity?

    @Query("SELECT * FROM clinical_notes WHERE patientId = :patientId AND deleted = 0 ORDER BY createdAt DESC")
    suspend fun getByPatientId(patientId: Long): List<ClinicalNoteEntity>

    @Query("SELECT * FROM clinical_notes WHERE tenantId = :tenantId AND deleted = 0 ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getByTenantId(tenantId: Long, limit: Int = 50): List<ClinicalNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ClinicalNoteEntity): Long

    @Update
    suspend fun update(entity: ClinicalNoteEntity)

    @Query("UPDATE clinical_notes SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: String)
}
