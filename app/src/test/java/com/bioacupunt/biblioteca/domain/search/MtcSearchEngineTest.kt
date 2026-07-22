package com.bioacupunt.biblioteca.domain.search

import com.bioacupunt.biblioteca.domain.ingestion.Provenance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pré-processamento de texto — funções puras.
 *
 * O ranking mudou de BM25 em memória para SQLite FTS4, mas isto aqui não mudou e
 * não pode mudar: o FTS4 não sabe que "Baço" e "Pi" são a mesma coisa. Se a
 * normalização ou os sinônimos quebrarem, a busca fica muda em português — e uma
 * busca muda, no RAG, vira "não encontrei" para uma pergunta que a biblioteca
 * respondia.
 */
class MtcSearchEngineTest {

    // -- Accent folding: the bug that made the old search useless in Portuguese

    @Test
    fun normalizeFoldsPortugueseAccents() {
        assertEquals("pulmao", MtcSearchEngine.normalize("Pulmão"))
        assertEquals("baco", MtcSearchEngine.normalize("Baço"))
        assertEquals("lingua", MtcSearchEngine.normalize("Língua"))
        assertEquals("deficiencia", MtcSearchEngine.normalize("Deficiência"))
    }

    @Test
    fun tokenizeDropsStopwordsAndPunctuation() {
        // "de", "da", "do" não distinguem artigo nenhum; mantê-los só polui a query.
        assertEquals(listOf("sindromes", "baco"), MtcSearchEngine.tokenize("Síndromes do Baço"))
        assertEquals(listOf("deficiencia", "qi"), MtcSearchEngine.tokenize("Deficiência de Qi!"))
    }

    @Test
    fun tokenizeOfStopwordsOnlyIsEmpty_soTheQueryIsRejectedUpstream() {
        // Isto é o que faz "de da do" não virar uma busca que casa com tudo.
        assertTrue(MtcSearchEngine.tokenize("de da do").isEmpty())
        assertTrue(MtcSearchEngine.tokenize("").isEmpty())
    }

    // -- Bilingual vocabulary: pinyin <-> Portuguese

    @Test
    fun pinyinAndPortugueseExpandToTheSameGroup() {
        // Quem estudou em português e o livro escrito em pinyin não compartilham
        // vocabulário. A expansão é o que costura os dois.
        val viaPortugues = MtcSearchEngine.expand(listOf("baco")).toSet()
        val viaPinyin = MtcSearchEngine.expand(listOf("pi")).toSet()
        assertEquals(viaPortugues, viaPinyin)
        assertTrue("baco" in viaPortugues && "pi" in viaPortugues)
    }

    @Test
    fun qiAndEnergiaAreSynonyms() {
        val viaQi = MtcSearchEngine.expand(listOf("qi")).toSet()
        val viaEnergia = MtcSearchEngine.expand(listOf("energia")).toSet()
        assertEquals("'energia' e 'qi' devem buscar o mesmo", viaQi, viaEnergia)
    }

    @Test
    fun deficienciaReachesXu() {
        // "deficiência" precisa alcançar conteúdo escrito como "Xu".
        assertTrue("xu" in MtcSearchEngine.expand(listOf("deficiencia")))
    }

    @Test
    fun unknownTermExpandsToItself_notToNothing() {
        // Um termo fora do vocabulário não pode sumir na expansão: sumir silenciosamente
        // transformaria uma busca específica numa busca mais ampla.
        assertEquals(listOf("plutonio"), MtcSearchEngine.expand(listOf("plutonio")))
    }

    @Test
    fun expansionIsStable_noJitterBetweenIdenticalCalls() {
        val a = MtcSearchEngine.expand(MtcSearchEngine.tokenize("deficiencia de qi do baco"))
        val b = MtcSearchEngine.expand(MtcSearchEngine.tokenize("deficiencia de qi do baco"))
        assertEquals(a, b)
    }
}

/**
 * O PORTÃO DA R2, testado em Kotlin puro.
 *
 * O backend é um fake em memória — de propósito. O que está sob teste não é o FTS4,
 * é a decisão do [MtcRetriever] diante do que o backend devolve. Essa decisão é a
 * que impede o modelo de ser chamado sem evidência, e ela precisa ser verificável
 * sem device, como qualquer regra de segurança deste projeto.
 */
class MtcRetrieverTest {

    private val baco = RetrievedArticle(
        articleId = "org_baco",
        title = "Síndromes do Baço",
        summary = "Deficiência de Qi do Baço.",
        content = """
            # Baço
            ## Funções
            O Baço transforma e transporta os alimentos.
            ## Deficiência de Qi do Baço
            Cansaço, fezes amolecidas, língua pálida com marcas de dentes e pulso fraco.
        """.trimIndent(),
        provenance = Provenance.VERIFICAVEL,
    )

    /**
     * Backend fake: casa um artigo se algum token expandido da pergunta aparecer
     * no texto. Grosseiro por design — o ranking real é do FTS4. O que importa
     * aqui é honrar o contrato: **sem match ⇒ lista vazia**.
     */
    private class FakeBackend(private val corpus: List<RetrievedArticle>) : ArticleSearchBackend {
        var callCount = 0
            private set

        override suspend fun search(query: String, maxResults: Int): List<RetrievedArticle> {
            callCount++
            val terms = MtcSearchEngine.expand(MtcSearchEngine.tokenize(query))
            if (terms.isEmpty()) return emptyList()
            return corpus.filter { article ->
                val haystack = MtcSearchEngine.tokenize(
                    "${article.title} ${article.summary} ${article.content}",
                ).toSet()
                terms.any { it in haystack }
            }.take(maxResults)
        }
    }

    private val backend = FakeBackend(listOf(baco))
    private val retriever = MtcRetriever(backend)

    @Test
    fun retrievesEvidenceForARelevantQuestion() = runBlocking {
        val grounding = retriever.retrieve("sintomas de deficiencia de qi do baco")
        assertTrue(grounding.hasEvidence)
        assertEquals("org_baco", grounding.passages.first().articleId)
    }

    @Test
    fun picksTheSectionThatActuallyAnswers_notJustTheFirstOne() = runBlocking {
        val grounding = retriever.retrieve("fezes amolecidas cansaco")
        val text = grounding.passages.first().text
        assertTrue(
            "Deve trazer a seção de Deficiência, não a de Funções",
            text.contains("Cansaço"),
        )
    }

    @Test
    fun unknownTopicYieldsNoEvidence_soTheModelIsNeverCalled() = runBlocking {
        // THE most important test in this file. If retrieval misses, the app must have
        // nothing to feed the model — an empty context is where hallucination lives.
        val grounding = retriever.retrieve("tratamento de fratura exposta com parafuso")
        assertFalse(grounding.hasEvidence)
        assertTrue(grounding.passages.isEmpty())
    }

    @Test
    fun emptyBackendResultIsAlwaysNoEvidence_whateverTheQuestionLooksLike() = runBlocking {
        // Blinda a decisão contra o backend: se ele devolve vazio, não existe pergunta
        // "boa o bastante" que faça o portão abrir.
        val silent = MtcRetriever(FakeBackend(emptyList()))
        listOf("deficiencia de qi do baco", "baco", "qi", "").forEach { q ->
            assertFalse("'$q' não pode produzir evidência de um acervo vazio", silent.retrieve(q).hasEvidence)
        }
    }

    @Test
    fun passagesAreNumberedForCitation() = runBlocking {
        val grounding = retriever.retrieve("baco")
        grounding.passages.forEachIndexed { i, p -> assertEquals(i + 1, p.ordinal) }
    }

    @Test
    fun promptCarriesEveryPassageAndTheQuestion() = runBlocking {
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

    // -- Provenance tests ------------------------------------------------

    @Test
    fun retrievedArticleCarriesProvenance() = runBlocking {
        // Itens com sourceRef são VERIFICAVEL; sem são RASCUNHO.
        val verArtigo = RetrievedArticle(
            articleId = "ver",
            title = "Verificável",
            summary = "Com página",
            content = "Conteúdo com referência.",
            provenance = Provenance.VERIFICAVEL,
        )
        val rasArtigo = RetrievedArticle(
            articleId = "ras",
            title = "Rascunho",
            summary = "Sem página",
            content = "Conteúdo sem referência específica.",
            provenance = Provenance.RASCUNHO,
        )
        assertEquals(Provenance.VERIFICAVEL, verArtigo.provenance)
        assertEquals(Provenance.RASCUNHO, rasArtigo.provenance)
    }

    @Test
    fun provenanceDefaultsToRascunho_forSafety() = runBlocking {
        // Itens sem proveniência explícita (legado) são RASCUNHO por segurança.
        val artigo = RetrievedArticle(
            articleId = "legado",
            title = "Legado",
            summary = "Sem proveniência",
            content = "...",
            // provenance não especificada → RASCUNHO
        )
        assertEquals(Provenance.RASCUNHO, artigo.provenance)
    }

    @Test
    fun backendDefaultsProvenanceToRascunho_whenNotSpecified() = runBlocking {
        // Artigos sem proveniência explícita no backend precisam ser RASCUNHO.
        // Este teste garante que o construtor de RetrievedArticle respeita o default.
        val semProveniencia = RetrievedArticle(
            articleId = "sem", title = "Sem", summary = "", content = "baco",
        )
        assertEquals(Provenance.RASCUNHO, semProveniencia.provenance)
    }

    @Test
    fun backendFiltersByProvenanceFake() = runBlocking {
        // Backend fake que filtra por proveniência — demonstra que o contrato
        // permite que o backend implemente filtragem sem quebrar o portão R2.
        val ver = RetrievedArticle(
            articleId = "v1", title = "Ver", summary = "", content = "baco",
            provenance = Provenance.VERIFICAVEL,
        )
        val ras = RetrievedArticle(
            articleId = "r1", title = "Ras", summary = "", content = "baco",
            provenance = Provenance.RASCUNHO,
        )
        val filtrado = object : ArticleSearchBackend {
            override suspend fun search(query: String, maxResults: Int): List<RetrievedArticle> {
                return listOf(ver, ras).filter { it.provenance == Provenance.VERIFICAVEL }
            }
        }
        val g = MtcRetriever(filtrado).retrieve("baco")
        assertTrue(g.hasEvidence)
        assertEquals(1, g.passages.size)
        assertEquals("v1", g.passages.first().articleId)
    }
}
