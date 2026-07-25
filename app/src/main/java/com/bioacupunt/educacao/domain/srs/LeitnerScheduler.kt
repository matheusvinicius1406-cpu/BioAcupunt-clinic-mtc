package com.bioacupunt.educacao.domain.srs

import com.bioacupunt.educacao.domain.model.CardProgress

/**
 * Repetição espaçada determinística (Leitner-lite). Sem IA: a data da próxima
 * revisão é função pura de (caixa atual, acertou/errou, agora).
 *
 * Caixas 0..4, com intervalo em dias por caixa em [INTERVAL_DAYS]. Um card sem
 * progresso (nunca revisado) é tratado como caixa 0 implícita — o primeiro
 * "Lembrei" já promove para a caixa 1.
 */
object LeitnerScheduler {
    /** Intervalo em dias por caixa: caixa 0 = agora, ..., caixa 4 = 14 dias. */
    val INTERVAL_DAYS = intArrayOf(0, 1, 3, 7, 14)
    private const val MAX_BOX = 4
    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun onRemembered(prev: CardProgress?, key: String, nowMs: Long): CardProgress {
        val nextBox = ((prev?.box ?: 0) + 1).coerceAtMost(MAX_BOX)
        return CardProgress(
            cardKey = key,
            box = nextBox,
            dueAtEpochMs = nowMs + INTERVAL_DAYS[nextBox] * DAY_MS,
            lastReviewedAtEpochMs = nowMs,
            totalReviews = (prev?.totalReviews ?: 0) + 1,
            totalLapses = prev?.totalLapses ?: 0,
        )
    }

    fun onForgot(prev: CardProgress?, key: String, nowMs: Long): CardProgress {
        return CardProgress(
            cardKey = key,
            box = 0,
            dueAtEpochMs = nowMs + INTERVAL_DAYS[0] * DAY_MS,
            lastReviewedAtEpochMs = nowMs,
            totalReviews = (prev?.totalReviews ?: 0) + 1,
            totalLapses = (prev?.totalLapses ?: 0) + 1,
        )
    }
}
