package com.bioacupunt.clinic

import com.bioacupunt.clinic.domain.model.TongueBodyColor
import com.bioacupunt.clinic.domain.model.TongueCoating
import com.bioacupunt.clinic.domain.model.TongueMoisture
import com.bioacupunt.clinic.domain.model.TongueObservation
import com.bioacupunt.clinic.domain.model.TongueObservationSource
import com.bioacupunt.clinic.domain.model.TongueObservationStatus
import com.bioacupunt.clinic.domain.model.TongueShape
import com.bioacupunt.clinic.domain.model.UnavailableVisionEngine
import com.bioacupunt.clinic.domain.model.VisionFeature
import com.bioacupunt.clinic.domain.model.VisionProviderType
import com.bioacupunt.clinic.domain.model.VisionRegion
import com.bioacupunt.clinic.domain.model.VisionResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TongueVisionTest {

    // --- TongueObservation ---

    @Test
    fun defaultObservation_hasCorrectDefaults() {
        val obs = TongueObservation(tenantId = 1L, patientId = 10L)
        assertEquals(0L, obs.id)
        assertEquals(TongueObservationStatus.DRAFT, obs.status)
        assertEquals(TongueObservationSource.MANUAL, obs.source)
        assertFalse(obs.status == TongueObservationStatus.CONFIRMED)
    }

    @Test
    fun bodyColors_allValuesExist() {
        assertEquals(7, TongueBodyColor.values().size)
        assertEquals("Pálida", TongueBodyColor.PALE.label)
        assertEquals("Vermelha", TongueBodyColor.RED.label)
        assertEquals("Roxa", TongueBodyColor.PURPLE.label)
    }

    @Test
    fun shapes_allValuesExist() {
        assertEquals(8, TongueShape.values().size)
        assertEquals("Dentada", TongueShape.SCALLOPED.label)
        assertEquals("Amadeirada", TongueShape.WOODEN.label)
    }

    @Test
    fun coatings_allValuesExist() {
        assertEquals(8, TongueCoating.values().size)
        assertEquals("Sem", TongueCoating.NONE.label)
        assertEquals("Espesso Amarelo", TongueCoating.THICK_YELLOW.label)
    }

    @Test
    fun moisture_allValuesExist() {
        assertEquals(5, TongueMoisture.values().size)
        assertEquals("Seca", TongueMoisture.DRY.label)
        assertEquals("Escorregadia", TongueMoisture.SLIPPERY.label)
    }

    @Test
    fun statusLifecycle_allValuesExist() {
        val statuses = TongueObservationStatus.values()
        assertEquals(6, statuses.size)
        assertTrue(statuses.contains(TongueObservationStatus.CAPTURED))
        assertTrue(statuses.contains(TongueObservationStatus.FEATURES_EXTRACTED))
        assertTrue(statuses.contains(TongueObservationStatus.DRAFT))
        assertTrue(statuses.contains(TongueObservationStatus.REVIEWED))
        assertTrue(statuses.contains(TongueObservationStatus.CONFIRMED))
        assertTrue(statuses.contains(TongueObservationStatus.REJECTED))
    }

    @Test
    fun regions_prepareCorrectly() {
        val obs = TongueObservation(
            tenantId = 1L, patientId = 10L,
            regionTip = "vermelha",
            regionCenter = "coating branco",
            regionRoot = "normal",
            regionLeft = "normal",
            regionRight = "normal",
        )
        assertEquals("vermelha", obs.regionTip)
        assertEquals("coating branco", obs.regionCenter)
        assertEquals("normal", obs.regionRoot)
    }

    @Test
    fun visionProvenance_storedWhenAIUsed() {
        val obs = TongueObservation(
            tenantId = 1L, patientId = 10L,
            source = TongueObservationSource.AI_EXTRACTED_DRAFT,
            visionModelName = "tongue-v1",
            visionModelVersion = "1.0.0",
            visionConfidence = 0.85,
        )
        assertEquals("tongue-v1", obs.visionModelName)
        assertEquals(0.85, obs.visionConfidence!!, 0.001)
        assertEquals(TongueObservationSource.AI_EXTRACTED_DRAFT, obs.source)
    }

    @Test
    fun observation_notAutoConfirmed() {
        val obs = TongueObservation(
            tenantId = 1L, patientId = 10L,
            source = TongueObservationSource.AI_EXTRACTED_DRAFT,
            visionConfidence = 0.95,
        )
        // AI output must always be DRAFT, never CONFIRMED
        assertFalse(obs.status == TongueObservationStatus.CONFIRMED)
        assertEquals(TongueObservationStatus.DRAFT, obs.status)
    }

    // --- VisionResult ---

    @Test
    fun visionResult_unavailable_hasCorrectState() {
        val result = VisionResult.unavailable()
        assertTrue(VisionResult.isUnavailable(result))
        assertEquals("none", result.modelName)
        assertEquals(0.0, result.overallConfidence, 0.001)
        assertTrue(result.features.isEmpty())
    }

    @Test
    fun visionResult_available_hasFeatures() {
        val result = VisionResult(
            features = listOf(
                VisionFeature(name = "color", value = "red", confidence = 0.9),
                VisionFeature(name = "coating", value = "thick_white", confidence = 0.8),
            ),
            regions = listOf(
                VisionRegion(name = "tip", confidence = 0.85),
            ),
            overallConfidence = 0.85,
            modelName = "tongue-v1",
            modelVersion = "1.0.0",
        )
        assertFalse(VisionResult.isUnavailable(result))
        assertEquals(2, result.features.size)
        assertEquals(1, result.regions.size)
        assertEquals("tongue-v1", result.modelName)
    }

    @Test
    fun visionFeature_storesConfidenceCorrectly() {
        val feature = VisionFeature(name = "moisture", value = "normal", confidence = 0.7, region = "center")
        assertEquals(0.7, feature.confidence, 0.001)
        assertEquals("center", feature.region)
    }

    // --- UnavailableVisionEngine ---

    @Test
    fun unavailableEngine_alwaysReturnsUnavailable() = runTest {
        val engine = UnavailableVisionEngine()
        assertFalse(engine.isAvailable())
        assertEquals(VisionProviderType.LOCAL, engine.providerType)

        val result = engine.analyzeTongue("content://media/1")
        assertTrue(result.isSuccess)
        assertTrue(VisionResult.isUnavailable(result.getOrThrow()))

        val generic = engine.analyzeImage("content://media/1")
        assertTrue(generic.isSuccess)
        assertTrue(VisionResult.isUnavailable(generic.getOrThrow()))
    }

    @Test
    fun visionConfidence_isNotDiagnosis() {
        // The confidence is an AI metric, not clinical certainty
        val obs = TongueObservation(
            tenantId = 1L, patientId = 10L,
            visionConfidence = 0.99,
            source = TongueObservationSource.AI_EXTRACTED_DRAFT,
        )
        // Even with 99% confidence, observation must remain DRAFT
        assertEquals(TongueObservationStatus.DRAFT, obs.status)
    }
}
