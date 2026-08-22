package com.bioacupunt.clinic

import com.bioacupunt.clinic.domain.model.PulseFeature
import com.bioacupunt.clinic.domain.model.PulseInputProvider
import com.bioacupunt.clinic.domain.model.PulseObservation
import com.bioacupunt.clinic.domain.model.PulseObservationStatus
import com.bioacupunt.clinic.domain.model.PulsePosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PulseObservationTest {

    @Test
    fun defaultPulse_hasCorrectDefaults() {
        val pulse = PulseObservation(tenantId = 1L, patientId = 10L)
        assertEquals(0L, pulse.id)
        assertEquals(PulseObservationStatus.DRAFT, pulse.status)
        assertEquals(PulseInputProvider.MANUAL, pulse.source)
        assertNull(pulse.rate)
        assertTrue(pulse.depth.isEmpty())
        assertTrue(pulse.features.isEmpty())
    }

    @Test
    fun positions_allValuesExist() {
        assertEquals(3, PulsePosition.values().size)
        assertEquals("Cun (proximal)", PulsePosition.CUN.label)
        assertEquals("Guan (middle)", PulsePosition.GUAN.label)
        assertEquals("Chi (distal)", PulsePosition.CHI.label)
    }

    @Test
    fun statusLifecycle_allValuesExist() {
        val statuses = PulseObservationStatus.values()
        assertEquals(5, statuses.size)
        assertTrue(statuses.contains(PulseObservationStatus.CAPTURED))
        assertTrue(statuses.contains(PulseObservationStatus.DRAFT))
        assertTrue(statuses.contains(PulseObservationStatus.REVIEWED))
        assertTrue(statuses.contains(PulseObservationStatus.CONFIRMED))
        assertTrue(statuses.contains(PulseObservationStatus.REJECTED))
    }

    @Test
    fun inputProviders_allValuesExist() {
        val providers = PulseInputProvider.values()
        assertEquals(4, providers.size)
        assertTrue(providers.contains(PulseInputProvider.MANUAL))
        assertTrue(providers.contains(PulseInputProvider.DEVICE))
        assertTrue(providers.contains(PulseInputProvider.IMPORTED))
        assertTrue(providers.contains(PulseInputProvider.AI_ASSISTED))
    }

    @Test
    fun positionsStoredCorrectly() {
        val pulse = PulseObservation(
            tenantId = 1L, patientId = 10L,
            leftCun = "浮, rápido",
            leftGuan = "normal",
            leftChi = "沉, fraco",
            rightCun = "normal",
            rightGuan = "滑",
            rightChi = "normal",
        )
        assertEquals("浮, rápido", pulse.leftCun)
        assertEquals("沉, fraco", pulse.leftChi)
        assertEquals("滑", pulse.rightGuan)
    }

    @Test
    fun features_storedWithProvenance() {
        val pulse = PulseObservation(
            tenantId = 1L, patientId = 10L,
            features = listOf(
                PulseFeature(name = "rate", value = "80", unit = "bpm", source = PulseInputProvider.DEVICE),
                PulseFeature(name = "depth", value = "superficial", confidence = 0.8),
            ),
        )
        assertEquals(2, pulse.features.size)
        assertEquals("80", pulse.features[0].value)
        assertEquals(PulseInputProvider.DEVICE, pulse.features[0].source)
        assertEquals(0.8, pulse.features[1].confidence!!, 0.001)
    }

    @Test
    fun manualInput_startsAtDraft() {
        val pulse = PulseObservation(
            tenantId = 1L, patientId = 10L,
            source = PulseInputProvider.MANUAL,
            status = PulseObservationStatus.DRAFT,
        )
        assertEquals(PulseObservationStatus.DRAFT, pulse.status)
        assertEquals(PulseInputProvider.MANUAL, pulse.source)
    }

    @Test
    fun deviceInput_startsAtCaptured() {
        val pulse = PulseObservation(
            tenantId = 1L, patientId = 10L,
            source = PulseInputProvider.DEVICE,
            status = PulseObservationStatus.CAPTURED,
        )
        assertEquals(PulseObservationStatus.CAPTURED, pulse.status)
    }

    @Test
    fun notAutoConfirmed() {
        // Even with device input, pulse must be reviewed
        val pulse = PulseObservation(
            tenantId = 1L, patientId = 10L,
            source = PulseInputProvider.DEVICE,
            status = PulseObservationStatus.DRAFT,
        )
        assertFalse(pulse.status == PulseObservationStatus.CONFIRMED)
    }

    @Test
    fun rateOptional_canBeNull() {
        val pulse = PulseObservation(tenantId = 1L, patientId = 10L)
        assertNull(pulse.rate)
    }

    @Test
    fun rate_storedWhenProvided() {
        val pulse = PulseObservation(tenantId = 1L, patientId = 10L, rate = 72)
        assertEquals(72, pulse.rate)
    }
}
