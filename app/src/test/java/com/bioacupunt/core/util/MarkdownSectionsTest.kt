package com.bioacupunt.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regressão: este splitter é a extração literal da regex que antes vivia duplicada
 * como `SECTION_HEADING` privada em `MtcRetriever`. O comportamento tem que
 * permanecer byte-a-byte idêntico — a busca da biblioteca (R2) e a extração de
 * flashcards de artigo dependem dele.
 */
class MarkdownSectionsTest {

    @Test
    fun `splits on h1 h2 and h3 but not h4`() {
        val markdown = """
            # Título
            Corpo do título.
            ## Subtítulo
            Corpo do subtítulo.
            ### Sub-subtítulo
            Corpo do sub-subtítulo.
            #### Não é seção
            Isto continua dentro da seção anterior.
        """.trimIndent()

        val sections = MarkdownSections.split(markdown)

        assertEquals(3, sections.size)
        assertTrue(sections[2].contains("#### Não é seção"))
    }

    @Test
    fun `text before the first heading becomes its own section`() {
        val markdown = "Texto solto antes de qualquer heading.\n# Primeira Seção\nCorpo."

        val sections = MarkdownSections.split(markdown)

        assertEquals(2, sections.size)
        assertEquals("Texto solto antes de qualquer heading.", sections[0])
    }

    @Test
    fun `an empty heading between two others is dropped, not kept as a blank section`() {
        // A segunda heading não tem título nem corpo — só a quebra de linha para a
        // próxima. Sem o filter, isso viraria uma seção "" no meio da lista.
        val markdown = "# A\n# \n# B\nCorpo"

        val sections = MarkdownSections.split(markdown)

        assertEquals(listOf("A", "B\nCorpo"), sections)
    }

    @Test
    fun `each section is trimmed`() {
        val markdown = "# Título   \n   Corpo com espaços ao redor   \n"

        val sections = MarkdownSections.split(markdown)

        assertEquals("Título   \n   Corpo com espaços ao redor", sections[0])
    }

    @Test
    fun `titleOf returns the first line`() {
        val section = "Título da Seção\nPrimeira linha do corpo.\nSegunda linha."

        assertEquals("Título da Seção", MarkdownSections.titleOf(section))
    }

    @Test
    fun `bodyOf returns everything after the first line, trimmed`() {
        val section = "Título da Seção\nPrimeira linha do corpo.\nSegunda linha."

        assertEquals("Primeira linha do corpo.\nSegunda linha.", MarkdownSections.bodyOf(section))
    }

    @Test
    fun `bodyOf is empty when the section has only a title`() {
        val section = "Só o título"

        assertEquals("", MarkdownSections.bodyOf(section))
    }

    @Test
    fun `no headings at all yields a single section with the whole trimmed text`() {
        val markdown = "  Texto corrido sem nenhum heading.  "

        val sections = MarkdownSections.split(markdown)

        assertEquals(1, sections.size)
        assertEquals("Texto corrido sem nenhum heading.", sections[0])
    }
}
