package com.bioacupunt.educacao.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SimulatedCaseDao {
    @Query("SELECT * FROM simulated_cases WHERE tenantId = :tenantId ORDER BY createdAt DESC, id DESC")
    fun observeCases(tenantId: Long): Flow<List<SimulatedCaseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCase(entity: SimulatedCaseEntity): Long
}
