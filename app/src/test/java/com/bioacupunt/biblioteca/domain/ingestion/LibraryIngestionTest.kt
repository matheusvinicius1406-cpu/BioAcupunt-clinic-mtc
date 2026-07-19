package com.bioacupunt.biblioteca.domain.ingestion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guarda o portão R4 da biblioteca: conteúdo sem proveniência humana revisada NÃO
 * pode ser encenado, e nada é publicado sem aval. Se estes testes forem apagados ou
 * "simplificados", a garantia que separa "ingestão revisada" de "lixo no acervo
 * clínico" some junto.
 */
class LibraryIngestionTest {

    private fun item(
        id: String = "a1",
        title: String = "Ponto E36 (Zusanli)",
        category: String = "PONTOS",
        content: String = "Ponto mar do He do Estômago...",
        citation: String = "Deadman, Manual of Acupuncture, p. 176",
        tags: List<String> = listOf("estômago"),
    ) = LibraryContentItem(id, title, category, "resumo", content, tags, citation)

    private fun pack(source: String = "Deadman 2007", vararg items: LibraryContentItem) =
        LibraryContentPack(source, items.toList())

    @Test
    fun itemWithoutCitationIsRejected_neverStaged() {
        val out = LibraryIngestion.stage(pack(items = arrayOf(item(citation = ""))), now = 1L)
        assertEquals(0, out.stagedCount)
        assertEquals(1, out.rejectedCount)
        assertTrue(out.rejected.first().reason.contains("citação"))
    }

    @Test
    fun packWithoutSourceRejectsEverything() {
        val out = LibraryIngestion.stage(pack(source = "", item(), item(id = "a2")), now = 1L)
        assertEquals(0, out.stagedCount)
        assertEquals(2, out.rejectedCount)
    }

    @Test
    fun invalidCategoryIsRejected() {
        val out = LibraryIngestion.stage(pack(items = arrayOf(item(category = "ASTROLOGIA"))), now = 1L)
        assertEquals(0, out.stagedCount)
        assertTrue(out.rejected.first().reason.contains("Categoria inválida"))
    }

    @Test
    fun emptyTitleOrContentIsRejected() {
        val out = LibraryIngestion.stage(
            pack(items = arrayOf(item(id = "x", title = ""), item(id = "y", content = ""))),
            now = 1L,
        )
        assertEquals(0, out.stagedCount)
        assertEquals(2, out.rejectedCount)
    }

    @Test
    fun validItemIsStagedAsPending_notApproved() {
        val out = LibraryIngestion.stage(pack(items = arrayOf(item())), now = 42L)
        assertEquals(1, out.stagedCount)
        val node = out.staged.first()
        assertEquals(ReviewStatus.PENDING, node.meta.status)
        assertFalse(
            "encenar não é publicar — nunca entra já aprovado",
            node.meta.status == ReviewStatus.APPROVED,
        )
        assertEquals("Deadman, Manual of Acupuncture, p. 176", node.meta.citation)
        assertEquals("Deadman 2007", node.meta.source)
        assertEquals(42L, node.meta.stagedAt)
        assertEquals("PONTOS", node.article.category)
    }

    @Test
    fun mixedPackStagesGoodRejectsBad() {
        val out = LibraryIngestion.stage(
            pack(items = arrayOf(item(id = "ok"), item(id = "bad", citation = ""))),
            now = 1L,
        )
        assertEquals(1, out.stagedCount)
        assertEquals(1, out.rejectedCount)
        assertEquals("ok", out.staged.first().article.id)
        assertEquals("bad", out.rejected.first().id)
    }
}
