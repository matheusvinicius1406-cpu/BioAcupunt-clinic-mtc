package com.bioacupunt.clinic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ClinicalMediaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ClinicalMediaEntity): Long

    @Update
    suspend fun update(entity: ClinicalMediaEntity)

    @Query(
        """
        SELECT * FROM clinical_media
        WHERE id = :id AND tenantId = :tenantId AND deleted = 0
        """
    )
    suspend fun getById(id: Long, tenantId: Long): ClinicalMediaEntity?

    @Query(
        """
        SELECT * FROM clinical_media
        WHERE tenantId = :tenantId AND patientId = :patientId AND deleted = 0
        ORDER BY createdAt DESC
        """
    )
    suspend fun getByPatient(tenantId: Long, patientId: Long): List<ClinicalMediaEntity>

    @Query(
        """
        SELECT * FROM clinical_media
        WHERE tenantId = :tenantId AND encounterId = :encounterId AND deleted = 0
        ORDER BY createdAt DESC
        """
    )
    suspend fun getByEncounter(tenantId: Long, encounterId: Long): List<ClinicalMediaEntity>

    @Query(
        """
        SELECT * FROM clinical_media
        WHERE tenantId = :tenantId AND patientId = :patientId AND category = :category AND deleted = 0
        ORDER BY createdAt DESC
        """
    )
    suspend fun getByPatientAndCategory(
        tenantId: Long,
        patientId: Long,
        category: String
    ): List<ClinicalMediaEntity>

    @Query(
        """
        SELECT * FROM clinical_media
        WHERE tenantId = :tenantId AND status = :status AND deleted = 0
        ORDER BY createdAt DESC
        """
    )
    suspend fun getByStatus(tenantId: Long, status: String): List<ClinicalMediaEntity>

    @Query(
        """
        UPDATE clinical_media
        SET status = :status, updatedAt = :updatedAt
        WHERE id = :id AND tenantId = :tenantId
        """
    )
    suspend fun updateStatus(id: Long, tenantId: Long, status: String, updatedAt: String)

    @Query(
        """
        UPDATE clinical_media
        SET deleted = :deletedAt, updatedAt = :updatedAt
        WHERE id = :id AND tenantId = :tenantId
        """
    )
    suspend fun softDelete(id: Long, tenantId: Long, deletedAt: String, updatedAt: String)

    @Query(
        """
        SELECT COUNT(*) FROM clinical_media
        WHERE tenantId = :tenantId AND patientId = :patientId AND deleted = 0
        """
    )
    suspend fun countByPatient(tenantId: Long, patientId: Long): Int
}
