package com.bioacupunt.educacao.domain.repository

import com.bioacupunt.core.util.Result
import com.bioacupunt.educacao.domain.model.CardProgress
import com.bioacupunt.educacao.domain.model.Flashcard
import com.bioacupunt.educacao.domain.model.StudyCard
import kotlinx.coroutines.flow.Flow

interface FlashcardRepository {
    /** União dos 12 cards fixos + cards da médica, cada um pareado com seu progresso. */
    fun observeDeck(): Flow<List<StudyCard>>

    /** Recusa [Flashcard.builtin] = true — cards fixos são read-only. */
    suspend fun saveCard(card: Flashcard): Result<Flashcard>

    /** Apaga o card da médica e o progresso associado (sem FK: builtin não tem linha-mãe). */
    suspend fun deleteCard(userRowId: Long): Result<Unit>

    /** Aplica o Leitner-lite ao [cardKey] e persiste o novo box/vencimento. */
    suspend fun recordReview(cardKey: String, remembered: Boolean, nowMs: Long): Result<CardProgress>
}
