package com.bioacupunt.clinic

import com.bioacupunt.clinic.domain.model.ClinicalMedia
import com.bioacupunt.clinic.domain.model.ClinicalMediaSource
import com.bioacupunt.clinic.domain.model.ClinicalMediaStatus
import com.bioacupunt.clinic.domain.model.ClinicalMediaType
import com.bioacupunt.clinic.domain.model.MediaSizeLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalMediaTest {

    @Test
    fun defaultMedia_hasCorrectDefaults() {
        val media = ClinicalMedia(
            tenantId = 1L,
            patientId = 10L,
            type = ClinicalMediaType.IMAGE,
            uri = "content://media/1",
            mimeType = "image/jpeg",
        )
        assertEquals(0L, media.id)
        assertEquals(ClinicalMediaStatus.CAPTURED, media.status)
        assertEquals(ClinicalMediaSource.CAMERA, media.source)
        assertTrue(media.hash.isEmpty())
        assertTrue(media.description.isEmpty())
    }

    @Test
    fun sizeLimits_maxForType() {
        assertEquals(20L * 1024 * 1024, MediaSizeLimits.maxForType(ClinicalMediaType.IMAGE))
        assertEquals(100L * 1024 * 1024, MediaSizeLimits.maxForType(ClinicalMediaType.AUDIO))
        assertEquals(500L * 1024 * 1024, MediaSizeLimits.maxForType(ClinicalMediaType.VIDEO))
        assertEquals(50L * 1024 * 1024, MediaSizeLimits.maxForType(ClinicalMediaType.DOCUMENT))
    }

    @Test
    fun statusLifecycle_allValuesExist() {
        val statuses = ClinicalMediaStatus.values()
        assertEquals(9, statuses.size)
        assertTrue(statuses.contains(ClinicalMediaStatus.CAPTURED))
        assertTrue(statuses.contains(ClinicalMediaStatus.VALIDATED))
        assertTrue(statuses.contains(ClinicalMediaStatus.STORED))
        assertTrue(statuses.contains(ClinicalMediaStatus.PROCESSING))
        assertTrue(statuses.contains(ClinicalMediaStatus.PROCESSED))
        assertTrue(statuses.contains(ClinicalMediaStatus.REVIEWED))
        assertTrue(statuses.contains(ClinicalMediaStatus.CONFIRMED))
        assertTrue(statuses.contains(ClinicalMediaStatus.REJECTED))
        assertTrue(statuses.contains(ClinicalMediaStatus.DELETED))
    }

    @Test
    fun mediaTypes_allValuesExist() {
        val types = ClinicalMediaType.values()
        assertEquals(4, types.size)
        assertEquals("Imagem", ClinicalMediaType.IMAGE.label)
        assertEquals("Áudio", ClinicalMediaType.AUDIO.label)
        assertEquals("Vídeo", ClinicalMediaType.VIDEO.label)
        assertEquals("Documento", ClinicalMediaType.DOCUMENT.label)
    }

    @Test
    fun mediaSource_allValuesExist() {
        val sources = ClinicalMediaSource.values()
        assertEquals(8, sources.size)
        assertTrue(sources.contains(ClinicalMediaSource.CAMERA))
        assertTrue(sources.contains(ClinicalMediaSource.IMAGE_PICKER))
        assertTrue(sources.contains(ClinicalMediaSource.FHIR_IMPORT))
        assertTrue(sources.contains(ClinicalMediaSource.AI_PROCESSED))
    }

    @Test
    fun equality_twoIdenticalMedia_areEqual() {
        val a = ClinicalMedia(tenantId = 1L, patientId = 10L, type = ClinicalMediaType.IMAGE, uri = "x", mimeType = "image/jpeg")
        val b = a.copy()
        assertEquals(a, b)
    }

    @Test
    fun equality_differentUri_areNotEqual() {
        val a = ClinicalMedia(tenantId = 1L, patientId = 10L, type = ClinicalMediaType.IMAGE, uri = "x", mimeType = "image/jpeg")
        val b = a.copy(uri = "y")
        assertNotEquals(a, b)
    }

    @Test
    fun encounterIdOptional_canBeNull() {
        val media = ClinicalMedia(tenantId = 1L, patientId = 10L, type = ClinicalMediaType.IMAGE, uri = "x", mimeType = "image/jpeg")
        assertFalse(media.encounterId != null && media.encounterId!! > 0)
    }

    @Test
    fun category_categorizesMediaCorrectly() {
        val tongue = ClinicalMedia(
            tenantId = 1L, patientId = 10L,
            type = ClinicalMediaType.IMAGE, uri = "x", mimeType = "image/jpeg",
            category = "tongue_photo"
        )
        assertEquals("tongue_photo", tongue.category)
    }

    @Test
    fun hashUsedForIntegrity_detection() {
        val media = ClinicalMedia(
            tenantId = 1L, patientId = 10L,
            type = ClinicalMediaType.IMAGE, uri = "x", mimeType = "image/jpeg",
            hash = "abc123sha256"
        )
        assertEquals("abc123sha256", media.hash)
    }
}
