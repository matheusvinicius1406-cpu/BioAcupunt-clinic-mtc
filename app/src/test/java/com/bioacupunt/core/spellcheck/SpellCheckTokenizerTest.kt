package com.bioacupunt.core.spellcheck

import org.junit.Assert.assertEquals
import org.junit.Test

class SpellCheckTokenizerTest {

    @Test
    fun splitsOnWhitespaceAndPunctuation() {
        val words = tokenizeForSpellCheck("Paciente relata dor, cansaço e insônia.")

        assertEquals(listOf("Paciente", "relata", "dor", "cansaço", "e", "insônia"), words)
    }

    @Test
    fun neverIncludesTokensWithDigits() {
        // Dose, código de ponto, data — nenhum é candidato a correção ortográfica.
        val words = tokenizeForSpellCheck("Dose 500mg via LI4, retorno em 2026-08-10.")

        assertEquals(listOf("Dose", "mg", "via", "LI", "retorno", "em"), words)
    }

    @Test
    fun keepsHyphenatedAndApostrophizedWordsAsOneToken() {
        val words = tokenizeForSpellCheck("Pós-agulhamento a paciente disse 'ok'.")

        assertEquals(listOf("Pós-agulhamento", "a", "paciente", "disse", "ok"), words)
    }

    @Test
    fun emptyOrPunctuationOnlyText_yieldsNoWords() {
        assertEquals(emptyList<String>(), tokenizeForSpellCheck(""))
        assertEquals(emptyList<String>(), tokenizeForSpellCheck("... , ; !"))
    }
}
