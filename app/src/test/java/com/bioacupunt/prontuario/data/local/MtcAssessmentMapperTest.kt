package com.bioacupunt.prontuario.data.local

import com.bioacupunt.prontuario.domain.model.BaGang
import com.bioacupunt.prontuario.domain.model.BaGangDepth
import com.bioacupunt.prontuario.domain.model.BaGangPolarity
import com.bioacupunt.prontuario.domain.model.BaGangStrength
import com.bioacupunt.prontuario.domain.model.BaGangTemperature
import com.bioacupunt.prontuario.domain.model.BodyMark
import com.bioacupunt.prontuario.domain.model.BodySide
import com.bioacupunt.prontuario.domain.model.ClinicalFlag
import com.bioacupunt.prontuario.domain.model.MtcAssessment
import com.bioacupunt.prontuario.domain.model.Organ
import com.bioacupunt.prontuario.domain.model.PatternFactor
import com.bioacupunt.prontuario.domain.model.PulseDepth
import com.bioacupunt.prontuario.domain.model.PulseFinding
import com.bioacupunt.prontuario.domain.model.PulsePosition
import com.bioacupunt.prontuario.domain.model.PulseQuality
import com.bioacupunt.prontuario.domain.model.PulseReading
import com.bioacupunt.prontuario.domain.model.SensationType
import com.bioacupunt.prontuario.domain.model.TongueBodyColor
import com.bioacupunt.prontuario.domain.model.TongueCoatingColor
import com.bioacupunt.prontuario.domain.model.TongueFinding
import com.bioacupunt.prontuario.domain.model.TongueShape
import com.bioacupunt.prontuario.domain.model.Wrist
import com.bioacupunt.prontuario.domain.model.ZangFuPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A chart that fails to load mid-consultation is a clinical failure, not just a bug.
 * These tests pin the two properties that matter: a full assessment survives a
 * round-trip unchanged, and corrupt/unknown data degrades gracefully instead of
 * throwing.
 */
class MtcAssessmentMapperTest {

    private val full = MtcAssessment(
        id = 7,
        patientId = 42,
        date = "2026-07-12",
        chiefComplaint = "Cervicalgia há 3 meses",
        baGang = BaGang(
            polarity = BaGangPolarity.YANG,
            depth = BaGangDepth.INTERIOR,
            temperature = BaGangTemperature.HEAT,
            strength = BaGangStrength.EXCESS,
        ),
        patterns = listOf(
            ZangFuPattern(
                organ = Organ.LIVER,
                factors = setOf(PatternFactor.QI_STAGNATION, PatternFactor.INTERNAL_HEAT),
                notes = "Irritabilidade, pulso em corda",
            ),
        ),
        tongue = TongueFinding(
            bodyColor = TongueBodyColor.RED,
            coatingColor = TongueCoatingColor.YELLOW,
            shapes = setOf(TongueShape.CRACKED, TongueShape.TOOTH_MARKED),
            photoUri = "content://tongue/1",
            notes = "Bordas mais vermelhas",
        ),
        pulse = PulseFinding(
            rateBpm = 88,
            readings = listOf(
                PulseReading(
                    wrist = Wrist.LEFT,
                    position = PulsePosition.GUAN,
                    depth = PulseDepth.MIDDLE,
                    qualities = setOf(PulseQuality.WIRY, PulseQuality.RAPID),
                ),
            ),
        ),
        bodyMarks = listOf(
            BodyMark("m1", BodySide.POSTERIOR, 0.5f, 0.22f, SensationType.PAIN, 7, "C5-C6"),
        ),
        flags = setOf(ClinicalFlag.PREGNANCY, ClinicalFlag.ANTICOAGULANTS),
        gestationalWeeks = 20,
        clinicalImpression = "Estagnação de Qi do Fígado com Calor",
    )

    @Test
    fun roundTrip_preservesEverything() {
        val restored = full.toEntity().toDomain()
        assertEquals(full, restored)
    }

    @Test
    fun roundTrip_preservesFlags_whichTheSafetyEngineDependsOn() {
        val restored = full.toEntity().toDomain()
        assertTrue(ClinicalFlag.PREGNANCY in restored.flags)
        assertTrue(ClinicalFlag.ANTICOAGULANTS in restored.flags)
        assertEquals(20, restored.gestationalWeeks)
    }

    @Test
    fun flagsAreStoredQueryable_notBuriedInJson() {
        val csv = full.toEntity().flagsCsv
        assertTrue("SQL precisa achar a flag por LIKE", csv.contains("PREGNANCY"))
    }

    @Test
    fun corruptJson_degradesGracefully_ratherThanThrowing() {
        val entity = full.toEntity().copy(
            tongueJson = "{ this is not json",
            pulseJson = "[]",           // right JSON, wrong shape
            patternsJson = "garbage",
            bodyMarksJson = "",
        )

        val restored = entity.toDomain()

        // The broken fields fall back to empty...
        assertEquals(TongueFinding(), restored.tongue)
        assertEquals(PulseFinding(), restored.pulse)
        assertTrue(restored.patterns.isEmpty())
        assertTrue(restored.bodyMarks.isEmpty())

        // ...but the rest of the chart still loads. This is the whole point.
        assertEquals("Cervicalgia há 3 meses", restored.chiefComplaint)
        assertEquals(BaGangTemperature.HEAT, restored.baGang.temperature)
        assertTrue(ClinicalFlag.PREGNANCY in restored.flags)
    }

    @Test
    fun unknownEnumValues_doNotCrashTheChart() {
        val entity = full.toEntity().copy(
            baGangTemperature = "PLASMA_HEAT",              // from a future build
            flagsCsv = "PREGNANCY,TIME_TRAVEL,PACEMAKER",   // unknown flag in the middle
        )

        val restored = entity.toDomain()

        assertEquals(BaGangTemperature.UNSET, restored.baGang.temperature)
        assertEquals(
            "Flags conhecidas devem sobreviver a uma desconhecida",
            setOf(ClinicalFlag.PREGNANCY, ClinicalFlag.PACEMAKER),
            restored.flags,
        )
    }

    @Test
    fun emptyAssessment_roundTrips() {
        val empty = MtcAssessment(patientId = 1)
        assertEquals(empty, empty.toEntity().toDomain())
    }
}
