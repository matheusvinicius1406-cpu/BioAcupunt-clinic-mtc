package com.bioacupunt.educacao.domain.model

/**
 * Um flashcard, seja um dos 12 fixos no código ([builtin] = true, read-only) ou um
 * criado pela médica (tabela `flashcards`, [userRowId] aponta a linha).
 *
 * [key] é a identidade estável usada por [CardProgress.cardKey]: `builtin_*` para os
 * fixos, `user_<id>` para os da médica.
 */
data class Flashcard(
    val key: String,
    val front: String,
    val back: String,
    val category: String,
    val builtin: Boolean,
    /** Artigo de origem quando criado via "Criar de artigo" (R4: extração verbatim). Vazio se não veio de artigo. */
    val sourceArticleId: String = "",
    val sourceSection: String = "",
    /** Id da linha em `flashcards` quando [builtin] = false. Null para cards fixos. */
    val userRowId: Long? = null,
)

/** Progresso de repetição espaçada (Leitner-lite) de um [Flashcard], por [cardKey]. */
data class CardProgress(
    val cardKey: String,
    val box: Int,
    val dueAtEpochMs: Long,
    val lastReviewedAtEpochMs: Long,
    val totalReviews: Int,
    val totalLapses: Int,
)

/** Um card pareado com seu progresso — [progress] é null quando o card nunca foi revisado. */
data class StudyCard(
    val card: Flashcard,
    val progress: CardProgress?,
) {
    /** Nunca revisado conta como vencido: a médica precisa ver todo card ao menos uma vez. */
    fun isDue(nowMs: Long): Boolean = progress == null || progress.dueAtEpochMs <= nowMs
}
