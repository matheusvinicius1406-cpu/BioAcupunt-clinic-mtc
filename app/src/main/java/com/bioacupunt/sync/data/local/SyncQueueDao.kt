package com.bioacupunt.sync.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun pending(): Flow<List<SyncQueueEntity>>

    @Query("UPDATE sync_queue SET status = 'SYNCED', syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: Long, syncedAt: Long)

    @Query("UPDATE sync_queue SET status = 'ERROR', lastError = :error WHERE id = :id")
    suspend fun markError(id: Long, error: String?)
}
