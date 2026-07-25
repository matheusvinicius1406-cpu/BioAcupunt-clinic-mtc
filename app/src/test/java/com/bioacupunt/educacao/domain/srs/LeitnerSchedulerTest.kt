package com.bioacupunt.educacao.domain.srs

import com.bioacupunt.educacao.domain.model.CardProgress
import org.junit.Assert.assertEquals
import org.junit.Test

/** Repetição espaçada é 100% função pura de (progresso anterior, resposta, agora) — sem clock real, sem IA. */
class LeitnerSchedulerTest {

    private val nowMs = 1_700_000_000_000L
    private val dayMs = 24L * 60 * 60 * 1000

    @Test
    fun `a never-reviewed card promoted to box 1 with a 1-day interval`() {
        val result = LeitnerScheduler.onRemembered(prev = null, key = "k1", nowMs = nowMs)

        assertEquals(1, result.box)
        assertEquals(nowMs + 1 * dayMs, result.dueAtEpochMs)
        assertEquals(1, result.totalReviews)
        assertEquals(0, result.totalLapses)
    }

    @Test
    fun `remembering climbs through the exact interval sequence 1, 3, 7, 14 days`() {
        var progress: CardProgress? = null
        val expectedIntervalsDays = listOf(1, 3, 7, 14)

        expectedIntervalsDays.forEachIndexed { i, days ->
            progress = LeitnerScheduler.onRemembered(progress, "k1", nowMs)
            assertEquals("box after remember #${i + 1}", i + 1, progress!!.box)
            assertEquals("interval after remember #${i + 1}", nowMs + days * dayMs, progress!!.dueAtEpochMs)
        }
    }

    @Test
    fun `box is capped at 4 — remembering again does not overflow the interval table`() {
        var progress: CardProgress? = null
        repeat(4) { progress = LeitnerScheduler.onRemembered(progress, "k1", nowMs) }
        assertEquals(4, progress!!.box)

        val onceMore = LeitnerScheduler.onRemembered(progress, "k1", nowMs)

        assertEquals(4, onceMore.box)
        assertEquals(nowMs + 14 * dayMs, onceMore.dueAtEpochMs)
    }

    @Test
    fun `forgetting resets to box 0, due immediately, and counts a lapse`() {
        val afterTwoRemembers = LeitnerScheduler.onRemembered(
            LeitnerScheduler.onRemembered(null, "k1", nowMs), "k1", nowMs
        )
        assertEquals(2, afterTwoRemembers.box)

        val forgot = LeitnerScheduler.onForgot(afterTwoRemembers, "k1", nowMs)

        assertEquals(0, forgot.box)
        assertEquals("esqueceu = due agora, sem período de graça", nowMs, forgot.dueAtEpochMs)
        assertEquals(3, forgot.totalReviews)
        assertEquals(1, forgot.totalLapses)
    }

    @Test
    fun `forgetting a card that was never reviewed still lands on box 0 due now`() {
        val forgot = LeitnerScheduler.onForgot(prev = null, key = "k1", nowMs = nowMs)

        assertEquals(0, forgot.box)
        assertEquals(nowMs, forgot.dueAtEpochMs)
        assertEquals(1, forgot.totalReviews)
        assertEquals(1, forgot.totalLapses)
    }

    @Test
    fun `same inputs always produce the same output — pure function, no hidden clock`() {
        val prev = CardProgress("k1", box = 2, dueAtEpochMs = 0, lastReviewedAtEpochMs = 0, totalReviews = 5, totalLapses = 1)

        val a = LeitnerScheduler.onRemembered(prev, "k1", nowMs)
        val b = LeitnerScheduler.onRemembered(prev, "k1", nowMs)

        assertEquals(a, b)
    }
}
