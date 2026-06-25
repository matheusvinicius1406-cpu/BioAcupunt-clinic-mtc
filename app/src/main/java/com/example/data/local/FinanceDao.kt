package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM finances ORDER BY date DESC")
    fun getAllFinances(): Flow<List<FinanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinance(finance: FinanceEntity)

    @Query("DELETE FROM finances WHERE id = :id")
    suspend fun deleteFinance(id: String)
}
