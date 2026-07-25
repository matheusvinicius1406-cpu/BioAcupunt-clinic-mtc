package com.bioacupunt.educacao.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Protege o namespace de key usado pelo LazyColumn (`key = card.key`) e o
 * contrato de conteúdo dos 12 fixos: revisados por humano, read-only.
 */
class BuiltinFlashcardsTest {

    @Test
    fun `there are exactly 12 builtin cards`() {
        assertEquals(12, BuiltinFlashcards.cards.size)
    }

    @Test
    fun `every key is unique and prefixed builtin_`() {
        val keys = BuiltinFlashcards.cards.map { it.key }
        assertEquals("nenhuma key duplicada", keys.size, keys.toSet().size)
        assertTrue(keys.all { it.startsWith("builtin_") })
    }

    @Test
    fun `every card is marked builtin and has no user row`() {
        assertTrue(BuiltinFlashcards.cards.all { it.builtin })
        assertTrue(BuiltinFlashcards.cards.all { it.userRowId == null })
    }

    @Test
    fun `no card has a blank front, back, or category`() {
        BuiltinFlashcards.cards.forEach { card ->
            assertTrue("front vazio em ${card.key}", card.front.isNotBlank())
            assertTrue("back vazio em ${card.key}", card.back.isNotBlank())
            assertTrue("category vazia em ${card.key}", card.category.isNotBlank())
        }
    }
}
