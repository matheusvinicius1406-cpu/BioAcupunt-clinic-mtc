package com.bioacupunt.educacao.data.local

import com.bioacupunt.educacao.domain.model.CardProgress
import com.bioacupunt.educacao.domain.model.Flashcard

fun FlashcardEntity.toDomain(): Flashcard = Flashcard(
    key = "user_$id",
    front = front,
    back = back,
    category = category,
    builtin = false,
    sourceArticleId = sourceArticleId,
    sourceSection = sourceSection,
    userRowId = id,
)

/** Só para cards da médica ([Flashcard.builtin] = false) — o repositório recusa builtin antes de chamar isto. */
fun Flashcard.toEntity(tenantId: Long, now: String): FlashcardEntity = FlashcardEntity(
    id = userRowId ?: 0L,
    tenantId = tenantId,
    front = front,
    back = back,
    category = category,
    sourceArticleId = sourceArticleId,
    sourceSection = sourceSection,
    createdAt = now,
    updatedAt = now,
)

fun FlashcardProgressEntity.toDomain(): CardProgress = CardProgress(
    cardKey = cardKey,
    box = box,
    dueAtEpochMs = dueAtEpochMs,
    lastReviewedAtEpochMs = lastReviewedAtEpochMs,
    totalReviews = totalReviews,
    totalLapses = totalLapses,
)

/** [id] é a PK da linha existente (0L para uma criação nova) — ver FlashcardDao.saveProgress. */
fun CardProgress.toEntity(tenantId: Long, id: Long = 0L): FlashcardProgressEntity = FlashcardProgressEntity(
    id = id,
    tenantId = tenantId,
    cardKey = cardKey,
    box = box,
    dueAtEpochMs = dueAtEpochMs,
    lastReviewedAtEpochMs = lastReviewedAtEpochMs,
    totalReviews = totalReviews,
    totalLapses = totalLapses,
)
