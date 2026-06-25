package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClinicaConfigDao {
    @Query("SELECT * FROM clinica_configs WHERE id = 'main_config'")
    fun getConfig(): Flow<ClinicaConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: ClinicaConfigEntity)
}
