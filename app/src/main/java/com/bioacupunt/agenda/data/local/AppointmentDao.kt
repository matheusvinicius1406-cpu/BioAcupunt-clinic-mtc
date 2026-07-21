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

    @Query(
        """
        SELECT * FROM appointments
        WHERE tenantId = :tenantId AND deleted = 0
        AND status NOT IN ('CANCELLED', 'NO_SHOW', 'COMPLETED')
        AND (date > :fromDate OR (date = :fromDate AND time >= :fromTime))
        ORDER BY date ASC, time ASC
        LIMIT 1
        """
    )
    fun observeNextUpcoming(fromDate: String, fromTime: String, tenantId: Long): Flow<AppointmentEntity?>

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

    // ── Sync ─────────────────────────────────────────────────────────────
    // Everything below is used only by SyncEngine.

    /** Every locally-changed row awaiting upload, oldest change first. */
    @Query("SELECT * FROM appointments WHERE pendingSync = 1 ORDER BY lastModified ASC")
    suspend fun getPendingSync(): List<AppointmentEntity>

    @Query("SELECT * FROM appointments WHERE clientId = :clientId LIMIT 1")
    suspend fun getByClientId(clientId: String): AppointmentEntity?

    @Query("SELECT * FROM appointments WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Long): AppointmentEntity?

    /**
     * Records that the server accepted this row at revision [rev].
     *
     * Clears pendingSync in the *same statement* that stores the revision.
     * Splitting them would leave a window where a crash marks the row synced
     * without recording what it synced to — and the next push would then send a
     * stale base revision and be refused as a conflict the doctor never caused.
     */
    @Query("UPDATE appointments SET pendingSync = 0, serverId = :serverId, baseRev = :rev WHERE clientId = :clientId")
    suspend fun markSynced(clientId: String, serverId: Long?, rev: Long)

    /**
     * Adopts the server's revision while keeping the local field values, so a
     * conflict resolved in favour of this device can be pushed again without
     * being refused as stale. pendingSync stays 1 on purpose: the row still has
     * to go up.
     */
    @Query("UPDATE appointments SET serverId = :serverId, baseRev = :rev, pendingSync = 1 WHERE clientId = :clientId")
    suspend fun rebaseOnServer(clientId: String, serverId: Long?, rev: Long)
}
