package com.bioacupunt.core.spellcheck

/**
 * Extrai palavras candidatas a checagem ortográfica de um texto livre.
 *
 * Tokens com dígito (dose "500mg", código de ponto "LI4", data) nunca viram candidato —
 * `\p{L}+` só casa letra. Função pura, sem dependência de Android: testável fora de
 * Robolectric, ao contrário de [SpellCheckService] (que depende de
 * `android.view.textservice`, framework real só sob shadow do Robolectric/device).
 */
internal fun tokenizeForSpellCheck(text: String): List<String> =
    WORD_PATTERN.findAll(text).map { it.value }.toList()

private val WORD_PATTERN = Regex("[\\p{L}]+(?:['-][\\p{L}]+)*")
