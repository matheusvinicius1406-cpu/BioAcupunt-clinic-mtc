package com.bioacupunt.prontuario.domain.safety

import com.bioacupunt.prontuario.domain.model.ClinicalFlag
import com.bioacupunt.prontuario.domain.model.MtcAssessment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Clinical Safety Engine is the one component in BioAcupunt where a wrong
 * answer can hurt a patient. It is therefore tested as a specification, not as an
 * afterthought: every FORBIDDEN rule has a test that proves it fires, and a paired
 * test that proves it does NOT fire when the flag is absent (guarding against a
 * rule that blocks everything and looks "safe" while being useless).
 */
class ClinicalSafetyEngineTest {

    private val engine = ClinicalSafetyEngine()

    private fun assessment(
        flags: Set<ClinicalFlag> = emptySet(),
        weeks: Int? = null,
    ) = MtcAssessment(patientId = 1L, flags = flags, gestationalWeeks = weeks)

    private val li4 = Acupoint("LI4", "Hegu", BodyRegion.UPPER_LIMB)
    private val sp6 = Acupoint("SP6", "Sanyinjiao", BodyRegion.LOWER_LIMB)
    private val st36 = Acupoint("ST36", "Zusanli", BodyRegion.LOWER_LIMB)
    private val cv4 = Acupoint("CV4", "Guanyuan", BodyRegion.LOWER_ABDOMEN)

    // -- Pregnancy ----------------------------------------------------------

    @Test
    fun pregnancy_forbidsLi4AndSp6() {
        val verdict = engine.evaluate(
            TreatmentProposal(points = listOf(li4, sp6), techniques = setOf(Technique.NEEDLING)),
            assessment(flags = setOf(ClinicalFlag.PREGNANCY)),
        )

        assertTrue("LI4/SP6 devem bloquear na gestação", verdict.isBlocked)
        assertEquals(2, verdict.blocking.count { it.flag == ClinicalFlag.PREGNANCY })
        assertTrue(verdict.blocking.any { it.subject == "LI4" })
        assertTrue(verdict.blocking.any { it.subject == "SP6" })
    }

    @Test
    fun pregnancy_pointCodeIsNormalised() {
        val verdict = engine.evaluate(
            TreatmentProposal(
                points = listOf(Acupoint("li-4", region = BodyRegion.UPPER_LIMB)),
                techniques = setOf(Technique.NEEDLING),
            ),
            assessment(flags = setOf(ClinicalFlag.PREGNANCY)),
        )
        assertTrue("'li-4' deve ser reconhecido como LI4", verdict.isBlocked)
    }

    @Test
    fun pregnancy_forbidsLowerAbdomenWhenInvasive() {
        val verdict = engine.evaluate(
            TreatmentProposal(points = listOf(cv4), techniques = setOf(Technique.NEEDLING)),
            assessment(flags = setOf(ClinicalFlag.PREGNANCY)),
        )
        assertTrue(verdict.isBlocked)
    }

    @Test
    fun pregnancy_allowsLowerAbdomenWithLaser() {
        // Laser is non-penetrating: the regional restriction must not fire.
        val verdict = engine.evaluate(
            TreatmentProposal(points = listOf(cv4), techniques = setOf(Technique.LASER)),
            assessment(flags = setOf(ClinicalFlag.PREGNANCY)),
        )
        assertFalse("Laser não deve disparar restrição regional", verdict.isBlocked)
    }

    @Test
    fun pregnancy_forbidsElectroacupuncture() {
        val verdict = engine.evaluate(
            TreatmentProposal(
                points = listOf(st36),
                techniques = setOf(Technique.ELECTROACUPUNCTURE),
            ),
            assessment(flags = setOf(ClinicalFlag.PREGNANCY)),
        )
        assertTrue(verdict.isBlocked)
    }

    @Test
    fun pregnancy_firstTrimesterRaisesCaution() {
        val verdict = engine.evaluate(
            TreatmentProposal(points = listOf(st36), techniques = setOf(Technique.NEEDLING)),
            assessment(flags = setOf(ClinicalFlag.PREGNANCY), weeks = 8),
        )
        assertFalse("ST36 não é proibido na gestação", verdict.isBlocked)
        assertTrue(verdict.cautions.any { it.title.contains("trimestre") })
    }

    @Test
    fun noPregnancyFlag_li4IsAllowed() {
        // Guard against a rule that blocks unconditionally.
        val verdict = engine.evaluate(
            TreatmentProposal(points = listOf(li4, sp6), techniques = setOf(Technique.NEEDLING)),
            assessment(),
        )
        assertTrue("Sem gestação, LI4/SP6 são livres", verdict.isClear)
    }

    // -- Anticoagulation ----------------------------------------------------

    @Test
    fun anticoagulants_forbidBloodletting() {
        val verdict = engine.evaluate(
            TreatmentProposal(techniques = setOf(Technique.BLOODLETTING)),
            assessment(flags = setOf(ClinicalFlag.ANTICOAGULANTS)),
        )
        assertTrue(verdict.isBlocked)
        assertEquals(ClinicalFlag.ANTICOAGULANTS, verdict.blocking.first().flag)
    }

    @Test
    fun anticoagulants_needlingIsCautionNotBlock() {
        val verdict = engine.evaluate(
            TreatmentProposal(points = listOf(st36), techniques = setOf(Technique.NEEDLING)),
            assessment(flags = setOf(ClinicalFlag.ANTICOAGULANTS)),
        )
        assertFalse("Agulhar sob anticoagulante é cautela, não veto", verdict.isBlocked)
        assertTrue(verdict.cautions.isNotEmpty())
    }

    @Test
    fun bleedingDisorder_forbidsWetCupping() {
        val verdict = engine.evaluate(
            TreatmentProposal(techniques = setOf(Technique.WET_CUPPING)),
            assessment(flags = setOf(ClinicalFlag.BLEEDING_DISORDER)),
        )
        assertTrue(verdict.isBlocked)
    }

    // -- Pacemaker ----------------------------------------------------------

    @Test
    fun pacemaker_forbidsElectroacupuncture() {
        val verdict = engine.evaluate(
            TreatmentProposal(techniques = setOf(Technique.ELECTROACUPUNCTURE)),
            assessment(flags = setOf(ClinicalFlag.PACEMAKER)),
        )
        assertTrue(verdict.isBlocked)
        assertEquals(ClinicalFlag.PACEMAKER, verdict.blocking.first().flag)
    }

    @Test
    fun pacemaker_allowsManualNeedling() {
        val verdict = engine.evaluate(
            TreatmentProposal(points = listOf(st36), techniques = setOf(Technique.NEEDLING)),
            assessment(flags = setOf(ClinicalFlag.PACEMAKER)),
        )
        assertFalse("Agulhamento manual não é vetado por marca-passo", verdict.isBlocked)
    }

    // -- Oncology / lymphedema ---------------------------------------------

    @Test
    fun lymphedema_forbidsNeedlingAffectedLimb() {
        val verdict = engine.evaluate(
            TreatmentProposal(
                points = listOf(Acupoint("LI11", region = BodyRegion.UPPER_LIMB, side = Laterality.LEFT)),
                techniques = setOf(Technique.NEEDLING),
                lymphedemaSide = Laterality.LEFT,
            ),
            assessment(flags = setOf(ClinicalFlag.LYMPHEDEMA)),
        )
        assertTrue(verdict.isBlocked)
    }

    @Test
    fun lymphedema_allowsContralateralLimb() {
        val verdict = engine.evaluate(
            TreatmentProposal(
                points = listOf(Acupoint("LI11", region = BodyRegion.UPPER_LIMB, side = Laterality.RIGHT)),
                techniques = setOf(Technique.NEEDLING),
                lymphedemaSide = Laterality.LEFT,
            ),
            assessment(flags = setOf(ClinicalFlag.LYMPHEDEMA)),
        )
        assertFalse("Membro contralateral é permitido", verdict.isBlocked)
    }

    @Test
    fun activeOncology_forbidsBloodlettingAndWarns() {
        val verdict = engine.evaluate(
            TreatmentProposal(techniques = setOf(Technique.BLOODLETTING)),
            assessment(flags = setOf(ClinicalFlag.ONCOLOGY_ACTIVE)),
        )
        assertTrue(verdict.isBlocked)
        assertTrue(verdict.cautions.isNotEmpty())
    }

    // -- Local / misc -------------------------------------------------------

    @Test
    fun skinLesion_forbidsApplication() {
        val verdict = engine.evaluate(
            TreatmentProposal(points = listOf(st36), techniques = setOf(Technique.NEEDLING)),
            assessment(flags = setOf(ClinicalFlag.SKIN_LESION_LOCAL)),
        )
        assertTrue(verdict.isBlocked)
    }

    @Test
    fun diabetes_moxibustionIsCaution() {
        val verdict = engine.evaluate(
            TreatmentProposal(points = listOf(st36), techniques = setOf(Technique.MOXIBUSTION)),
            assessment(flags = setOf(ClinicalFlag.DIABETES)),
        )
        assertFalse(verdict.isBlocked)
        assertTrue(verdict.cautions.any { it.flag == ClinicalFlag.DIABETES })
    }

    // -- Engine contract ----------------------------------------------------

    @Test
    fun emptyAssessment_yieldsClearVerdict() {
        val verdict = engine.evaluate(
            TreatmentProposal(points = listOf(li4, sp6, cv4), techniques = Technique.entries.toSet()),
            assessment(),
        )
        assertTrue("Sem flags, nada deve ser levantado", verdict.isClear)
    }

    @Test
    fun multipleFlags_accumulateAndDeduplicate() {
        val verdict = engine.evaluate(
            TreatmentProposal(
                points = listOf(li4),
                techniques = setOf(Technique.BLOODLETTING, Technique.ELECTROACUPUNCTURE),
            ),
            assessment(
                flags = setOf(
                    ClinicalFlag.PREGNANCY,
                    ClinicalFlag.ANTICOAGULANTS,
                    ClinicalFlag.PACEMAKER,
                ),
            ),
        )
        assertTrue(verdict.isBlocked)
        // LI4 (pregnancy) + electro (pregnancy) + bloodletting (anticoag) + electro (pacemaker)
        assertTrue("Deve acumular vetos de várias flags", verdict.blocking.size >= 4)

        val distinct = verdict.blocking.map { Triple(it.severity, it.flag, it.subject) }
        assertEquals("Não pode haver achados duplicados", distinct.size, distinct.distinct().size)
    }

    @Test
    fun aFailingRuleCannotCrashTheEngine() {
        val exploding = SafetyRule { _, _ -> error("boom") }
        val resilient = ClinicalSafetyEngine(
            rules = listOf(exploding) + ClinicalSafetyEngine.DEFAULT_RULES,
        )

        val verdict = resilient.evaluate(
            TreatmentProposal(points = listOf(li4), techniques = setOf(Technique.NEEDLING)),
            assessment(flags = setOf(ClinicalFlag.PREGNANCY)),
        )
        assertTrue("Uma regra quebrada não pode derrubar as demais", verdict.isBlocked)
    }
}
