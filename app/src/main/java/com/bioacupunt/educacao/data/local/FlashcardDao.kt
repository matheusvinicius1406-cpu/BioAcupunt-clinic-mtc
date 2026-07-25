package com.bioacupunt.educacao.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards WHERE tenantId = :tenantId ORDER BY createdAt DESC, id DESC")
    fun observeCards(tenantId: Long): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCard(entity: FlashcardEntity): Long

    @Query("DELETE FROM flashcards WHERE id = :id AND tenantId = :tenantId")
    suspend fun deleteCard(id: Long, tenantId: Long)

    @Query("SELECT * FROM flashcard_progress WHERE tenantId = :tenantId")
    fun observeProgress(tenantId: Long): Flow<List<FlashcardProgressEntity>>

    @Query("SELECT * FROM flashcard_progress WHERE tenantId = :tenantId AND cardKey = :cardKey LIMIT 1")
    suspend fun getProgress(tenantId: Long, cardKey: String): FlashcardProgressEntity?

    /**
     * REPLACE por conflito de PK ([FlashcardProgressEntity.id]), não pelo índice único
     * (tenantId, cardKey) — quem chama já leu a linha existente e preservou o [FlashcardProgressEntity.id]
     * (ver FlashcardRepositoryImpl.recordReview). Deixar o REPLACE resolver via índice
     * único trocaria o autoincrement a cada revisão.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(entity: FlashcardProgressEntity): Long

    @Query("DELETE FROM flashcard_progress WHERE tenantId = :tenantId AND cardKey = :cardKey")
    suspend fun deleteProgress(tenantId: Long, cardKey: String)
}
