package com.example.data.intelligence

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface IntelligenceDao {
    @Query("SELECT * FROM topics")
    fun getAllTopics(): Flow<List<TopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity)

    @Query("SELECT * FROM flashcards")
    fun getAllFlashcards(): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity)

    @Query("SELECT * FROM clinical_cases")
    fun getAllCases(): Flow<List<ClinicalCaseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCase(clinicalCase: ClinicalCaseEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaseResult(result: CaseResultEntity)
}

@Database(
    entities = [TopicEntity::class, FlashcardEntity::class, ClinicalCaseEntity::class, CaseResultEntity::class],
    version = 1,
    exportSchema = false
)
abstract class IntelligenceDatabase : RoomDatabase() {
    abstract fun intelligenceDao(): IntelligenceDao
}
