package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MtcProntuaryDao {
    @Query("SELECT * FROM mtc_prontuaries WHERE patientId = :patientId")
    fun getProntuaryByPatient(patientId: String): Flow<MtcProntuaryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProntuary(prontuary: MtcProntuaryEntity)
}
