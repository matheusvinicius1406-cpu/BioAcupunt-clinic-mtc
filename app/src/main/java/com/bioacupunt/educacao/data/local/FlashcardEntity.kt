package com.bioacupunt.educacao.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Cards autorais da médica. Os 12 fixos ficam em código ([com.bioacupunt.educacao.data.BuiltinFlashcards]), nunca aqui. */
@Entity(
    tableName = "flashcards",
    indices = [Index("tenantId")],
)
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tenantId: Long,
    val front: String,
    val back: String,
    val category: String,
    /** Artigo de origem quando criado via "Criar de artigo". Vazio para cards do zero. */
    val sourceArticleId: String = "",
    val sourceSection: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
)

/**
 * Progresso de repetição espaçada por [cardKey] (`builtin_*` ou `user_<id>`) — cobre as
 * duas populações de flashcard com uma tabela só. [dueAtEpochMs] é INTEGER epoch-ms (não
 * ISO TEXT): ordenação lexicográfica de `Instant.toString()` é traiçoeira para
 * comparação de "vencido vs não".
 */
@Entity(
    tableName = "flashcard_progress",
    indices = [
        Index(value = ["tenantId", "cardKey"], unique = true),
        Index(value = ["tenantId", "dueAtEpochMs"]),
    ],
)
data class FlashcardProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tenantId: Long,
    val cardKey: String,
    val box: Int,
    val dueAtEpochMs: Long,
    val lastReviewedAtEpochMs: Long,
    val totalReviews: Int,
    val totalLapses: Int,
)
