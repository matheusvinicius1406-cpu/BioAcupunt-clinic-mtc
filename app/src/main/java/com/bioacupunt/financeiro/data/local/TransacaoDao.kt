package com.bioacupunt.financeiro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bioacupunt.financeiro.domain.model.TransactionStatus
import com.bioacupunt.financeiro.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransacaoDao {

    @Query("SELECT * FROM transacoes WHERE deleted = 0 ORDER BY date DESC")
    fun observeAll(): Flow<List<TransacaoEntity>>

    @Query("SELECT * FROM transacoes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransacaoEntity?

    @Query("SELECT * FROM transacoes WHERE patientId = :patientId AND date BETWEEN :start AND :end ORDER BY date DESC")
    fun observeByPatientAndRange(patientId: Long, start: String, end: String): Flow<List<TransacaoEntity>>

    @Query("""SELECT COALESCE(SUM(amountBrl),0.0) FROM transacoes 
              WHERE status = :status AND date BETWEEN :start AND :end AND type = :type""")
    suspend fun sumByStatusAndRange(status: String, start: String, end: String, type: String): Double

    @Query("""SELECT COALESCE(SUM(amountBrl),0.0) FROM transacoes 
              WHERE date BETWEEN :start AND :end""")
    suspend fun sumRevenue(start: String, end: String): Double

    @Query("""SELECT COALESCE(SUM(amountBrl),0.0) FROM transacoes 
              WHERE status = 'PAGO' AND date BETWEEN :start AND :end""")
    suspend fun sumPaid(start: String, end: String): Double

    @Query("""SELECT COALESCE(SUM(amountBrl),0.0) FROM transacoes 
              WHERE status = 'PENDENTE' AND date BETWEEN :start AND :end""")
    suspend fun sumPending(start: String, end: String): Double

    @Query("""SELECT * FROM transacoes WHERE pendingSync = 1 AND deleted = 0 
              AND lastModified >= :since ORDER BY lastModified ASC""")
    suspend fun getChangedSince(since: String): List<TransacaoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: TransacaoEntity): Long

    // ── Sync ─────────────────────────────────────────────────────────────
    // Everything below is used only by SyncEngine.

    /** Every locally-changed row awaiting upload, oldest change first. */
    @Query("SELECT * FROM transacoes WHERE pendingSync = 1 ORDER BY lastModified ASC")
    suspend fun getPendingSync(): List<TransacaoEntity>

    @Query("SELECT * FROM transacoes WHERE clientId = :clientId LIMIT 1")
    suspend fun getByClientId(clientId: String): TransacaoEntity?

    @Query("SELECT * FROM transacoes WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Long): TransacaoEntity?

    /**
     * Records that the server accepted this row at revision [rev].
     *
     * Clears pendingSync in the *same statement* that stores the revision.
     * Splitting them would leave a window where a crash marks the row synced
     * without recording what it synced to — and the next push would then send a
     * stale base revision and be refused as a conflict the doctor never caused.
     */
    @Query("UPDATE transacoes SET pendingSync = 0, serverId = :serverId, baseRev = :rev WHERE clientId = :clientId")
    suspend fun markSynced(clientId: String, serverId: Long?, rev: Long)

    /**
     * Adopts the server's revision while keeping the local field values, so a
     * conflict resolved in favour of this device can be pushed again without
     * being refused as stale. pendingSync stays 1 on purpose: the row still has
     * to go up.
     */
    @Query("UPDATE transacoes SET serverId = :serverId, baseRev = :rev, pendingSync = 1 WHERE clientId = :clientId")
    suspend fun rebaseOnServer(clientId: String, serverId: Long?, rev: Long)
}
