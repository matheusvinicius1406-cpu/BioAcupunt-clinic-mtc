package com.bioacupunt.clinic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * DAO for tongue observations.
 * All queries are scoped by tenantId — no cross-tenant access possible.
 */
@Dao
interface TongueObservationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TongueObservationEntity): Long

    @Update
    suspend fun update(entity: TongueObservationEntity)

    @Query(
        """
        SELECT * FROM tongue_observations
        WHERE id = :id AND tenantId = :tenantId AND deleted = 0
        """
    )
    suspend fun getById(id: Long, tenantId: Long): TongueObservationEntity?

    @Query(
        """
        SELECT * FROM tongue_observations
        WHERE tenantId = :tenantId AND patientId = :patientId AND deleted = 0
        ORDER BY createdAt DESC
        """
    )
    suspend fun getByPatient(tenantId: Long, patientId: Long): List<TongueObservationEntity>

    @Query(
        """
        SELECT * FROM tongue_observations
        WHERE tenantId = :tenantId AND encounterId = :encounterId AND deleted = 0
        ORDER BY createdAt DESC
        """
    )
    suspend fun getByEncounter(tenantId: Long, encounterId: Long): List<TongueObservationEntity>

    @Query(
        """
        SELECT * FROM tongue_observations
        WHERE tenantId = :tenantId AND patientId = :patientId AND status = :status AND deleted = 0
        ORDER BY createdAt DESC
        """
    )
    suspend fun getByPatientAndStatus(
        tenantId: Long,
        patientId: Long,
        status: String,
    ): List<TongueObservationEntity>

    @Query(
        """
        SELECT * FROM tongue_observations
        WHERE tenantId = :tenantId AND mediaId = :mediaId AND deleted = 0
        """
    )
    suspend fun getByMediaId(tenantId: Long, mediaId: Long): TongueObservationEntity?

    @Query(
        """
        UPDATE tongue_observations
        SET status = :status, updatedAt = :updatedAt
        WHERE id = :id AND tenantId = :tenantId
        """
    )
    suspend fun updateStatus(id: Long, tenantId: Long, status: String, updatedAt: String)

    @Query(
        """
        UPDATE tongue_observations
        SET reviewedBy = :reviewedBy, reviewedAt = :reviewedAt, updatedAt = :updatedAt
        WHERE id = :id AND tenantId = :tenantId
        """
    )
    suspend fun markReviewed(
        id: Long,
        tenantId: Long,
        reviewedBy: String,
        reviewedAt: String,
        updatedAt: String,
    )

    @Query(
        """
        UPDATE tongue_observations
        SET status = 'CONFIRMED', confirmedBy = :confirmedBy, confirmedAt = :confirmedAt, updatedAt = :updatedAt
        WHERE id = :id AND tenantId = :tenantId
        """
    )
    suspend fun confirm(
        id: Long,
        tenantId: Long,
        confirmedBy: String,
        confirmedAt: String,
        updatedAt: String,
    )

    @Query(
        """
        UPDATE tongue_observations
        SET deleted = :deletedAt, updatedAt = :updatedAt
        WHERE id = :id AND tenantId = :tenantId
        """
    )
    suspend fun softDelete(id: Long, tenantId: Long, deletedAt: String, updatedAt: String)

    @Query(
        """
        SELECT COUNT(*) FROM tongue_observations
        WHERE tenantId = :tenantId AND patientId = :patientId AND deleted = 0
        """
    )
    suspend fun countByPatient(tenantId: Long, patientId: Long): Int

    /**
     * Get the most recent confirmed tongue observation for a patient.
     * Used for longitudinal comparison.
     */
    @Query(
        """
        SELECT * FROM tongue_observations
        WHERE tenantId = :tenantId AND patientId = :patientId AND status = 'CONFIRMED' AND deleted = 0
        ORDER BY createdAt DESC
        LIMIT 1
        """
    )
    suspend fun getLatestConfirmed(tenantId: Long, patientId: Long): TongueObservationEntity?
}
