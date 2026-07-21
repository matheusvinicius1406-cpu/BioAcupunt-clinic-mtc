package com.bioacupunt.sync.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * How far this device has pulled.
 *
 * A single row. `lastPulledRev` is the highest server revision this device has
 * durably applied; the next pull asks for everything above it.
 *
 * It is advanced **only after** a batch has been written locally. Advancing it
 * on receipt instead would mean a crash mid-apply skips those records forever:
 * the cursor says they arrived, the database says otherwise, and nothing ever
 * asks for them again.
 */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val lastPulledRev: Long = 0,
    val lastSyncAt: String? = null,
    val lastError: String? = null,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

/**
 * A change the server refused because the record moved on underneath it.
 *
 * Both versions are kept — the local one and the server's — and neither is
 * applied until the doctor chooses. This table is the reason the app can promise
 * that no clinical or scheduling note is ever destroyed by automatic merging.
 *
 * A conflict is *not* an error state to be cleared. It stays until resolved.
 */
@Entity(
    tableName = "sync_conflicts",
    indices = [Index("entityType"), Index("resolvedAt"), Index(value = ["entityType", "clientId"], unique = true)],
)
data class SyncConflictEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val clientId: String,
    val serverId: Long?,
    /** The revision the server holds — what the local edit was measured against. */
    val serverRev: Long,
    /** JSON of what this device wanted to save. */
    val localPayloadJson: String,
    /** JSON of what the server currently holds. */
    val serverPayloadJson: String,
    val detectedAt: String,
    /** Null while unresolved. Set to the resolution timestamp once chosen. */
    val resolvedAt: String? = null,
    /** "LOCAL" or "SERVER" — recorded so the choice is auditable after the fact. */
    val resolution: String? = null,
)

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM sync_state WHERE id = :id LIMIT 1")
    suspend fun get(id: Int = SyncStateEntity.SINGLETON_ID): SyncStateEntity?

    @Query("SELECT * FROM sync_state WHERE id = :id LIMIT 1")
    fun observe(id: Int = SyncStateEntity.SINGLETON_ID): Flow<SyncStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(state: SyncStateEntity)

    /**
     * Moves the cursor forward, never backward.
     *
     * `MAX` rather than plain assignment: two overlapping sync runs can finish
     * out of order, and the later-finishing one may carry the older revision.
     * Assigning it would rewind the cursor and re-pull records already applied —
     * which, for a record the doctor has since edited locally, means resurrecting
     * the server's older copy over her newer one.
     */
    @Query("UPDATE sync_state SET lastPulledRev = MAX(lastPulledRev, :rev), lastSyncAt = :at, lastError = NULL WHERE id = :id")
    suspend fun advanceCursor(rev: Long, at: String, id: Int = SyncStateEntity.SINGLETON_ID)

    @Query("UPDATE sync_state SET lastError = :error WHERE id = :id")
    suspend fun recordError(error: String?, id: Int = SyncStateEntity.SINGLETON_ID)
}

@Dao
interface SyncConflictDao {
    @Query("SELECT * FROM sync_conflicts WHERE resolvedAt IS NULL ORDER BY detectedAt DESC")
    fun observeUnresolved(): Flow<List<SyncConflictEntity>>

    @Query("SELECT COUNT(*) FROM sync_conflicts WHERE resolvedAt IS NULL")
    fun observeUnresolvedCount(): Flow<Int>

    @Query("SELECT * FROM sync_conflicts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SyncConflictEntity?

    /**
     * REPLACE on the (entityType, clientId) unique index: re-pushing the same
     * record while a conflict is still open refreshes that conflict rather than
     * stacking a second one. The doctor should see one decision per record, not
     * a growing pile of the same question.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(conflict: SyncConflictEntity): Long

    @Query("UPDATE sync_conflicts SET resolvedAt = :at, resolution = :resolution WHERE id = :id")
    suspend fun markResolved(id: Long, resolution: String, at: String)
}
