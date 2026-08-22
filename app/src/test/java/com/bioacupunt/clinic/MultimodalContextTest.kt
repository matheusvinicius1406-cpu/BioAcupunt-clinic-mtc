package com.bioacupunt.clinic

import com.bioacupunt.clinic.domain.model.MultimodalContext
import com.bioacupunt.clinic.domain.model.MultimodalObservation
import com.bioacupunt.clinic.domain.model.ObservationFactStatus
import com.bioacupunt.clinic.domain.model.PulseObservation
import com.bioacupunt.clinic.domain.model.TongueBodyColor
import com.bioacupunt.clinic.domain.model.TongueObservation
import com.bioacupunt.clinic.domain.model.TongueShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultimodalContextTest {

    @Test
    fun emptyContext_hasNoFindings() {
        val ctx = MultimodalContext(patientId = 1L)
        assertFalse(ctx.hasConfirmedFindings)
        assertFalse(ctx.hasAiDrafts)
        assertFalse(ctx.hasComparisonData)
        assertTrue(ctx.buildCopilotContext().isEmpty())
    }

    @Test
    fun contextWithConfirmedTongue_hasFindings() {
        val tongue = TongueObservation(
            tenantId = 1L, patientId = 10L,
            bodyColor = TongueBodyColor.RED,
            shape = TongueShape.THICK,
        )
        val ctx = MultimodalContext(
            patientId = 10L,
            confirmedTongueFindings = listOf(tongue),
        )
        assertTrue(ctx.hasConfirmedFindings)
        assertFalse(ctx.hasAiDrafts)
    }

    @Test
    fun contextWithConfirmedPulse_hasFindings() {
        val pulse = PulseObservation(
            tenantId = 1L, patientId = 10L,
            rate = 80,
            depth = "profundo",
        )
        val ctx = MultimodalContext(
            patientId = 10L,
            confirmedPulseFindings = listOf(pulse),
        )
        assertTrue(ctx.hasConfirmedFindings)
    }

    @Test
    fun aiDrafts_notConfirmedFacts() {
        val draft = MultimodalObservation(
            sourceType = "tongue_vision",
            status = ObservationFactStatus.AI_DETECTED,
            summary = "Possível coating branco detectado",
            confidence = 0.75,
        )
        val ctx = MultimodalContext(
            patientId = 10L,
            aiDraftFindings = listOf(draft),
        )
        assertFalse(ctx.hasConfirmedFindings)
        assertTrue(ctx.hasAiDrafts)
    }

    @Test
    fun comparisonData_previousEncounter() {
        val prevTongue = TongueObservation(
            tenantId = 1L, patientId = 10L,
            bodyColor = TongueBodyColor.PALE,
        )
        val ctx = MultimodalContext(
            patientId = 10L,
            previousEncounterTongue = prevTongue,
        )
        assertTrue(ctx.hasComparisonData)
    }

    @Test
    fun copilotContext_includesOnlyConfirmed() {
        val tongue = TongueObservation(
            tenantId = 1L, patientId = 10L,
            bodyColor = TongueBodyColor.RED,
        )
        val draft = MultimodalObservation(
            sourceType = "pulse_vision",
            status = ObservationFactStatus.AI_DETECTED,
            summary = "Possível pulso rápido",
        )
        val ctx = MultimodalContext(
            patientId = 10L,
            confirmedTongueFindings = listOf(tongue),
            aiDraftFindings = listOf(draft),
        )
        val context = ctx.buildCopilotContext()
        assertTrue("Confirmed findings should be in context", context.contains("Achados confirmados"))
        assertTrue("AI drafts should be labeled as not confirmed", context.contains("não confirmadas"))
        assertTrue("Should include tongue color", context.contains("Vermelha"))
    }

    @Test
    fun copilotContext_multipleSources() {
        val tongue = TongueObservation(
            tenantId = 1L, patientId = 10L,
            bodyColor = TongueBodyColor.PURPLE,
            coating = com.bioacupunt.clinic.domain.model.TongueCoating.THIN_WHITE,
        )
        val pulse = PulseObservation(
            tenantId = 1L, patientId = 10L,
            rate = 60,
            depth = "superficial",
            leftCun = "flutuante",
        )
        val ctx = MultimodalContext(
            patientId = 10L,
            confirmedTongueFindings = listOf(tongue),
            confirmedPulseFindings = listOf(pulse),
        )
        val context = ctx.buildCopilotContext()
        assertTrue(context.contains("Achados confirmados de língua"))
        assertTrue(context.contains("Achados confirmados de pulso"))
        assertTrue(context.contains("60 bpm"))
        assertTrue(context.contains("flutuante"))
    }

    @Test
    fun observationFactStatus_allValues() {
        val values = ObservationFactStatus.values()
        assertEquals(4, values.size)
        assertTrue(values.contains(ObservationFactStatus.OBSERVED))
        assertTrue(values.contains(ObservationFactStatus.AI_DETECTED))
        assertTrue(values.contains(ObservationFactStatus.REVIEWED))
        assertTrue(values.contains(ObservationFactStatus.CONFIRMED))
    }

    @Test
    fun context_preservesProvenance() {
        val draft = MultimodalObservation(
            sourceType = "tongue_vision",
            status = ObservationFactStatus.AI_DETECTED,
            summary = "test",
            confidence = 0.9,
            modelVersion = "1.0",
        )
        assertEquals(0.9, draft.confidence!!, 0.001)
        assertEquals("1.0", draft.modelVersion)
    }
}
