package com.bioacupunt.core.spellcheck

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MtcTermsDictionaryTest {

    @Test
    fun recognizesCommonMtcJargon_caseInsensitive() {
        assertTrue(MtcTermsDictionary.isKnownTerm("moxa"))
        assertTrue(MtcTermsDictionary.isKnownTerm("Moxa"))
        assertTrue(MtcTermsDictionary.isKnownTerm("ZANG"))
        assertTrue(MtcTermsDictionary.isKnownTerm("Qi"))
    }

    @Test
    fun recognizesAcupuncturePointCodes() {
        assertTrue(MtcTermsDictionary.isKnownTerm("LI4"))
        assertTrue(MtcTermsDictionary.isKnownTerm("st36"))
        assertTrue(MtcTermsDictionary.isKnownTerm("BL60"))
        assertTrue(MtcTermsDictionary.isKnownTerm("GV20"))
    }

    @Test
    fun rejectsOrdinaryPortugueseWords_soTheRealSpellCheckerStillRunsOnThem() {
        // O dicionário MTC não pode virar uma lista de exceção geral — só o jargão real.
        assertFalse(MtcTermsDictionary.isKnownTerm("cansasso"))
        assertFalse(MtcTermsDictionary.isKnownTerm("paciente"))
    }

    @Test
    fun ignoresSurroundingPunctuation() {
        assertTrue(MtcTermsDictionary.isKnownTerm("moxa."))
        assertTrue(MtcTermsDictionary.isKnownTerm("(qi)"))
    }
}
