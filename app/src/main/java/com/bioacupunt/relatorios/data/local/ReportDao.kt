package com.bioacupunt.relatorios.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bioacupunt.relatorios.domain.model.Report
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY generatedAt DESC")
    fun observeAll(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(report: ReportEntity): Long
}
