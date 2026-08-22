package com.bioacupunt.clinic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * DAO for pulse observations.
 * All queries are scoped by tenantId — no cross-tenant access possible.
 */
@Dao
interface PulseObservationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PulseObservationEntity): Long

    @Update
    suspend fun update(entity: PulseObservationEntity)

    @Query(
        """
        SELECT * FROM pulse_observations
        WHERE id = :id AND tenantId = :tenantId AND deleted = 0
        """
    )
    suspend fun getById(id: Long, tenantId: Long): PulseObservationEntity?

    @Query(
        """
        SELECT * FROM pulse_observations
        WHERE tenantId = :tenantId AND patientId = :patientId AND deleted = 0
        ORDER BY createdAt DESC
        """
    )
    suspend fun getByPatient(tenantId: Long, patientId: Long): List<PulseObservationEntity>

    @Query(
        """
        SELECT * FROM pulse_observations
        WHERE tenantId = :tenantId AND encounterId = :encounterId AND deleted = 0
        ORDER BY createdAt DESC
        """
    )
    suspend fun getByEncounter(tenantId: Long, encounterId: Long): List<PulseObservationEntity>

    @Query(
        """
        SELECT * FROM pulse_observations
        WHERE tenantId = :tenantId AND patientId = :patientId AND status = :status AND deleted = 0
        ORDER BY createdAt DESC
        """
    )
    suspend fun getByPatientAndStatus(
        tenantId: Long,
        patientId: Long,
        status: String,
    ): List<PulseObservationEntity>

    @Query(
        """
        UPDATE pulse_observations
        SET status = :status, updatedAt = :updatedAt
        WHERE id = :id AND tenantId = :tenantId
        """
    )
    suspend fun updateStatus(id: Long, tenantId: Long, status: String, updatedAt: String)

    @Query(
        """
        UPDATE pulse_observations
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
        UPDATE pulse_observations
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
        UPDATE pulse_observations
        SET deleted = :deletedAt, updatedAt = :updatedAt
        WHERE id = :id AND tenantId = :tenantId
        """
    )
    suspend fun softDelete(id: Long, tenantId: Long, deletedAt: String, updatedAt: String)

    @Query(
        """
        SELECT COUNT(*) FROM pulse_observations
        WHERE tenantId = :tenantId AND patientId = :patientId AND deleted = 0
        """
    )
    suspend fun countByPatient(tenantId: Long, patientId: Long): Int

    /**
     * Get the most recent confirmed pulse observation for a patient.
     * Used for longitudinal comparison.
     */
    @Query(
        """
        SELECT * FROM pulse_observations
        WHERE tenantId = :tenantId AND patientId = :patientId AND status = 'CONFIRMED' AND deleted = 0
        ORDER BY createdAt DESC
        LIMIT 1
        """
    )
    suspend fun getLatestConfirmed(tenantId: Long, patientId: Long): PulseObservationEntity?
}
