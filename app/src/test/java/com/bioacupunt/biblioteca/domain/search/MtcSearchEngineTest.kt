package com.bioacupunt.biblioteca.domain.search

import com.bioacupunt.biblioteca.domain.model.MtcArticle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MtcSearchEngineTest {

    private val pulmao = MtcArticle(
        id = "mer_pulmao",
        title = "Meridiano do Pulmão (Shou Tai Yin)",
        category = "MERIDIANOS",
        summary = "Meridiano Yin da Mão com 11 pontos. Governa o Qi e a respiração.",
        content = """
            # Meridiano do Pulmão
            ## Funções
            Governa o Qi e a respiração. Regula as vias d'água.
            ## Pontos
            P7 Lieque é o ponto Luo.
        """.trimIndent(),
        tags = listOf("meridiano", "metal"),
    )

    private val baco = MtcArticle(
        id = "org_baco",
        title = "Síndromes do Baço (Pi)",
        category = "SINDROME_ORGAOS",
        summary = "Deficiência de Qi do Baço, umidade e fleuma.",
        content = """
            # Baço
            ## Deficiência de Qi do Baço
            Cansaço, fezes amolecidas, língua pálida com marcas de dentes.
            O Baço transforma e transporta. Xu de Qi leva a umidade.
        """.trimIndent(),
        tags = listOf("zangfu", "terra"),
    )

    private val lingua = MtcArticle(
        id = "sem_lingua",
        title = "Semiologia da Língua",
        category = "LINGUA",
        summary = "Cor, saburra, forma e umidade da língua.",
        content = """
            # Língua
            ## Cor
            Língua pálida indica deficiência de Qi ou Xue.
            Língua vermelha indica calor.
        """.trimIndent(),
        tags = listOf("diagnostico"),
    )

    private val index = MtcSearchEngine.index(listOf(pulmao, baco, lingua))

    // -- Accent folding: the bug that made the old search useless in Portuguese

    @Test
    fun normalizeFoldsPortugueseAccents() {
        assertEquals("pulmao", MtcSearchEngine.normalize("Pulmão"))
        assertEquals("baco", MtcSearchEngine.normalize("Baço"))
        assertEquals("lingua", MtcSearchEngine.normalize("Língua"))
        assertEquals("deficiencia", MtcSearchEngine.normalize("Deficiência"))
    }

    @Test
    fun findsAccentedArticleFromUnaccentedQuery() {
        // Nobody types the tilde on a phone. The old LIKE '%pulmao%' found nothing.
        val hits = index.search("pulmao")
        assertTrue(hits.isNotEmpty())
        assertEquals("mer_pulmao", hits.first().article.id)
    }

    @Test
    fun findsAccentedArticleFromUnaccentedQuery_baco() {
        val hits = index.search("baco")
        assertEquals("org_baco", hits.first().article.id)
    }

    // -- Ranking: the article *about* the topic must win

    @Test
    fun titleMatchOutranksIncidentalBodyMention() {
        // "língua" appears in the Baço body text, but Semiologia da Língua is *about* it.
        val hits = index.search("lingua")
        assertEquals(
            "O artigo sobre língua deve vencer a menção de passagem",
            "sem_lingua",
            hits.first().article.id,
        )
    }

    @Test
    fun scoresArePositiveAndOrderedDescending() {
        val hits = index.search("deficiencia qi")
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { it.score > 0.0 })
        assertEquals(hits.map { it.score }.sortedDescending(), hits.map { it.score })
    }

    // -- Bilingual vocabulary: pinyin <-> Portuguese

    @Test
    fun pinyinFindsPortugueseArticle() {
        // Searching "Pi" (pinyin for Spleen) must find "Síndromes do Baço".
        val hits = index.search("pi")
        assertTrue(
            "Pinyin deve encontrar o artigo em português",
            hits.any { it.article.id == "org_baco" },
        )
    }

    @Test
    fun portugueseFindsPinyinTerm() {
        // "deficiência" must reach content written as "Xu".
        val hits = index.search("deficiencia")
        assertTrue(hits.any { it.article.id == "org_baco" })
    }

    @Test
    fun qiAndEnergiaAreSynonyms() {
        val viaQi = index.search("qi").map { it.article.id }.toSet()
        val viaEnergia = index.search("energia").map { it.article.id }.toSet()
        assertEquals("'energia' e 'qi' devem buscar o mesmo", viaQi, viaEnergia)
    }

    // -- Contract

    @Test
    fun emptyOrStopwordOnlyQueryReturnsNothing() {
        assertTrue(index.search("").isEmpty())
        assertTrue(index.search("de da do").isEmpty())
    }

    @Test
    fun nonsenseQueryReturnsNothing_ratherThanRandomArticles() {
        // Critical for RAG: a miss must be an honest empty list, not a weak match.
        assertTrue(index.search("xyzzy plutonio").isEmpty())
    }

    @Test
    fun resultsAreStable_noJitterBetweenIdenticalSearches() {
        val a = index.search("qi").map { it.article.id }
        val b = index.search("qi").map { it.article.id }
        assertEquals(a, b)
    }
}

class MtcRetrieverTest {

    private val baco = MtcArticle(
        id = "org_baco",
        title = "Síndromes do Baço",
        category = "SINDROME_ORGAOS",
        summary = "Deficiência de Qi do Baço.",
        content = """
            # Baço
            ## Funções
            O Baço transforma e transporta os alimentos.
            ## Deficiência de Qi do Baço
            Cansaço, fezes amolecidas, língua pálida com marcas de dentes e pulso fraco.
        """.trimIndent(),
        tags = listOf("zangfu"),
    )

    private val retriever = MtcRetriever(listOf(baco))

    @Test
    fun retrievesEvidenceForARelevantQuestion() {
        val grounding = retriever.retrieve("sintomas de deficiencia de qi do baco")
        assertTrue(grounding.hasEvidence)
        assertEquals("org_baco", grounding.passages.first().articleId)
    }

    @Test
    fun picksTheSectionThatActuallyAnswers_notJustTheFirstOne() {
        val grounding = retriever.retrieve("fezes amolecidas cansaco")
        val text = grounding.passages.first().text
        assertTrue(
            "Deve trazer a seção de Deficiência, não a de Funções",
            text.contains("Cansaço"),
        )
    }

    @Test
    fun unknownTopicYieldsNoEvidence_soTheModelIsNeverCalled() {
        // THE most important test in this file. If retrieval misses, the app must have
        // nothing to feed the model — an empty context is where hallucination lives.
        val grounding = retriever.retrieve("tratamento de fratura exposta com parafuso")
        assertFalse(grounding.hasEvidence)
        assertTrue(grounding.passages.isEmpty())
    }

    @Test
    fun passagesAreNumberedForCitation() {
        val grounding = retriever.retrieve("baco")
        grounding.passages.forEachIndexed { i, p -> assertEquals(i + 1, p.ordinal) }
    }

    @Test
    fun promptCarriesEveryPassageAndTheQuestion() {
        val grounding = retriever.retrieve("deficiencia de qi do baco")
        val prompt = MtcRetriever.buildPrompt(grounding)

        grounding.passages.forEach { p ->
            assertTrue("Prompt deve conter o trecho [${p.ordinal}]", prompt.contains("[${p.ordinal}]"))
            assertTrue(prompt.contains(p.articleTitle))
        }
        assertTrue(prompt.contains("deficiencia de qi do baco"))
    }

    @Test
    fun systemPromptForbidsOutsideKnowledge() {
        val sys = MtcRetriever.SYSTEM_PROMPT
        assertTrue(sys.contains("EXCLUSIVAMENTE"))
        assertTrue(sys.contains("Não use conhecimento próprio"))
        assertTrue(sys.contains("Nunca invente"))
    }
}
