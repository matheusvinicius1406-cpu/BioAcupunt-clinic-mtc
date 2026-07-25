package com.bioacupunt.educacao.data.repository

import com.bioacupunt.core.util.AppError
import com.bioacupunt.core.util.Result
import com.bioacupunt.educacao.data.BuiltinFlashcards
import com.bioacupunt.educacao.data.local.FlashcardDao
import com.bioacupunt.educacao.data.local.toDomain
import com.bioacupunt.educacao.data.local.toEntity
import com.bioacupunt.educacao.domain.model.CardProgress
import com.bioacupunt.educacao.domain.model.Flashcard
import com.bioacupunt.educacao.domain.model.StudyCard
import com.bioacupunt.educacao.domain.repository.FlashcardRepository
import com.bioacupunt.educacao.domain.srs.LeitnerScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * [tenantId] é uma lambda, não um [com.bioacupunt.core.multitenancy.TenantManager] injetado
 * direto: `TenantManager` é uma classe concreta sobre `SecurePreferences`, impossível de
 * instanciar em teste JVM puro (o padrão de teste do projeto é FakeDao sem Robolectric).
 * Resolvida por chamada — não cacheada — para que troca de tenant não vaze o deck.
 */
class FlashcardRepositoryImpl(
    private val dao: FlashcardDao,
    private val tenantId: () -> Long,
) : FlashcardRepository {

    override fun observeDeck(): Flow<List<StudyCard>> {
        val cardsFlow = dao.observeCards(tenantId()).map { list -> list.map { it.toDomain() } }
        val progressFlow = dao.observeProgress(tenantId()).map { list -> list.map { it.toDomain() } }
        return combine(cardsFlow, progressFlow) { userCards, progressList ->
            val progressByKey = progressList.associateBy { it.cardKey }
            (BuiltinFlashcards.cards + userCards).map { card -> StudyCard(card, progressByKey[card.key]) }
        }.catch { emit(emptyList()) }
    }

    override suspend fun saveCard(card: Flashcard): Result<Flashcard> {
        if (card.builtin) {
            return Result.Error(AppError.ValidationError("Cards fixos não podem ser editados."))
        }
        return try {
            val tid = tenantId()
            val now = java.time.Instant.now().toString()
            val savedId = dao.saveCard(card.toEntity(tid, now))
            val finalId = card.userRowId ?: savedId
            Result.Success(card.copy(userRowId = finalId, key = "user_$finalId"))
        } catch (e: Exception) {
            Result.Error(AppError.from(e))
        }
    }

    override suspend fun deleteCard(userRowId: Long): Result<Unit> {
        return try {
            val tid = tenantId()
            dao.deleteCard(userRowId, tid)
            dao.deleteProgress(tid, "user_$userRowId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.from(e))
        }
    }

    override suspend fun recordReview(cardKey: String, remembered: Boolean, nowMs: Long): Result<CardProgress> {
        return try {
            val tid = tenantId()
            val existing = dao.getProgress(tid, cardKey)
            val prev = existing?.toDomain()
            val updated = if (remembered) {
                LeitnerScheduler.onRemembered(prev, cardKey, nowMs)
            } else {
                LeitnerScheduler.onForgot(prev, cardKey, nowMs)
            }
            dao.saveProgress(updated.toEntity(tid, id = existing?.id ?: 0L))
            Result.Success(updated)
        } catch (e: Exception) {
            Result.Error(AppError.from(e))
        }
    }
}
