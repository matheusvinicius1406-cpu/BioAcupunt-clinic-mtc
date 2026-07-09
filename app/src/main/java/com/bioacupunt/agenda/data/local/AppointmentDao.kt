package com.bioacupunt.agenda.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bioacupunt.agenda.domain.model.AppointmentStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {

    @Query("SELECT * FROM appointments WHERE tenantId = :tenantId AND deleted = 0 AND date = :date ORDER BY time ASC")
    fun observeByDate(date: String, tenantId: Long): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE tenantId = :tenantId AND deleted = 0 AND patientId = :patientId ORDER BY date DESC, time DESC")
    fun observeByPatient(patientId: Long, tenantId: Long): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE tenantId = :tenantId AND deleted = 0 AND status = :status ORDER BY date ASC, time ASC")
    fun observeByStatus(status: String, tenantId: Long): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE id = :id AND tenantId = :tenantId LIMIT 1")
    suspend fun getById(id: Long, tenantId: Long): AppointmentEntity?

    @Query("SELECT * FROM appointments WHERE tenantId = :tenantId AND deleted = 0 AND date = :date ORDER BY time ASC")
    suspend fun getByDateSync(date: String, tenantId: Long): List<AppointmentEntity>

    @Query(
        "SELECT * FROM appointments WHERE tenantId = :tenantId AND deleted = 0 AND date BETWEEN :start AND :end ORDER BY date ASC, time ASC"
    )
    fun observeBetween(start: String, end: String, tenantId: Long): Flow<List<AppointmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: AppointmentEntity): Long

    @Query("UPDATE appointments SET pendingSync = 1, lastModified = :ts WHERE id = :id AND tenantId = :tenantId")
    suspend fun markPending(id: Long, ts: String, tenantId: Long)

    @Query("SELECT COUNT(*) FROM appointments WHERE tenantId = :tenantId AND deleted = 0 AND date = :date")
    suspend fun countByDate(date: String, tenantId: Long): Int

    @Query("SELECT COUNT(*) FROM appointments WHERE tenantId = :tenantId AND deleted = 0 AND status = :status")
    suspend fun countByStatus(status: String, tenantId: Long): Int

    @Query("SELECT * FROM appointments WHERE tenantId = :tenantId AND pendingSync = 1 AND deleted = 0 AND lastModified >= :since ORDER BY lastModified ASC")
    suspend fun getChangedSince(tenantId: Long, since: String): List<AppointmentEntity>
}
