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
}
