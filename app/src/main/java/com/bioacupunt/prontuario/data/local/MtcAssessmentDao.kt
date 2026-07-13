package com.bioacupunt.prontuario.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MtcAssessmentDao {

    /** Full history for a patient — drives the evolution timeline. */
    @Query("SELECT * FROM mtc_assessments WHERE patientId = :pid ORDER BY date DESC, id DESC")
    fun observeForPatient(pid: Long): Flow<List<MtcAssessmentEntity>>

    /** The chart currently being worked on. */
    @Query("SELECT * FROM mtc_assessments WHERE patientId = :pid ORDER BY date DESC, id DESC LIMIT 1")
    fun observeLatest(pid: Long): Flow<MtcAssessmentEntity?>

    @Query("SELECT * FROM mtc_assessments WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MtcAssessmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: MtcAssessmentEntity): Long

    @Query("DELETE FROM mtc_assessments WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * Every clinical flag ever recorded for this patient, across all sessions.
     *
     * This is why `flagsCsv` is a queryable column rather than a JSON blob. The
     * Safety Engine screens against the patient's *standing* risk profile, not just
     * whatever was ticked in today's form: a pacemaker recorded in March must still
     * veto electroacupuncture in July, even if the practitioner forgot to re-tick it.
     * Forgetting is exactly the failure mode this software exists to prevent.
     */
    @Query("SELECT flagsCsv FROM mtc_assessments WHERE patientId = :pid AND flagsCsv != ''")
    suspend fun flagsHistory(pid: Long): List<String>

    /** Most recent gestational age on record, for trimester-sensitive rules. */
    @Query(
        """
        SELECT gestationalWeeks FROM mtc_assessments
        WHERE patientId = :pid AND gestationalWeeks IS NOT NULL
        ORDER BY date DESC, id DESC LIMIT 1
        """,
    )
    suspend fun latestGestationalWeeks(pid: Long): Int?

    @Query("SELECT COUNT(*) FROM mtc_assessments WHERE patientId = :pid")
    suspend fun count(pid: Long): Int

    /** Analytics: which patterns show up most in this clinic. */
    @Query(
        """
        SELECT baGangTemperature AS bucket, COUNT(*) AS total FROM mtc_assessments
        WHERE baGangTemperature != 'UNSET' GROUP BY baGangTemperature ORDER BY total DESC
        """,
    )
    suspend fun temperatureDistribution(): List<BucketCount>

    @Query("SELECT * FROM mtc_assessments WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun pendingSync(): List<MtcAssessmentEntity>
}

data class BucketCount(val bucket: String, val total: Int)
