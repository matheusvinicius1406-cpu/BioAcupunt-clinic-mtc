package com.bioacupunt.core.util

/**
 * Splitter de seções Markdown por heading `#`/`##`/`###`.
 *
 * Única fonte da regex que antes vivia duplicada como `SECTION_HEADING` privada em
 * `MtcRetriever` (busca da biblioteca) — agora também usada pela extração de
 * flashcards a partir de artigo aprovado (Educação, R4: extração verbatim, nunca
 * geração).
 */
object MarkdownSections {
    private val SECTION_HEADING = Regex("(?m)^#{1,3} ")

    /** Idêntico byte-a-byte à cadeia histórica: split → trim → filtra vazios. */
    fun split(markdown: String): List<String> =
        markdown
            .split(SECTION_HEADING)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /** Primeira linha da seção (o texto do heading, sem o `#`/`##`/`###` — já removido pelo split). */
    fun titleOf(section: String): String =
        section.lineSequence().firstOrNull()?.trim().orEmpty()

    /** Corpo da seção: tudo após a primeira linha. */
    fun bodyOf(section: String): String =
        section.lineSequence().drop(1).joinToString("\n").trim()
}
