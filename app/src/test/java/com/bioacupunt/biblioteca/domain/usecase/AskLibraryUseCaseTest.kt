package com.bioacupunt.biblioteca.domain.usecase

import com.bioacupunt.ai.core.AiRepository
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.ai.core.AiResult
import com.bioacupunt.biblioteca.domain.ingestion.Provenance
import com.bioacupunt.biblioteca.domain.search.ArticleSearchBackend
import com.bioacupunt.biblioteca.domain.search.MtcRetriever
import com.bioacupunt.biblioteca.domain.search.MtcSearchEngine
import com.bioacupunt.biblioteca.domain.search.RetrievedArticle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O PORTÃO DA R2, testado ONDE ele de fato importa: no [AskLibraryUseCase].
 *
 * O teste sagrado `unknownTopicYieldsNoEvidence_soTheModelIsNeverCalled`
 * (em MtcSearchEngineTest/MtcRetrieverTest) prova que o *retriever* não acha
 * evidência para um assunto fora da biblioteca — mas ele não toca no modelo. Ele
 * afirma `!grounding.hasEvidence`, e para aí.
 *
 * A garantia real da R2 é o `if (!grounding.hasEvidence) return Answer.NoEvidence`
 * dentro do use case: sem evidência, `ai.generate` **não pode ser chamado**. Estes
 * testes usam um espião no [AiRepository] que conta invocações e prova exatamente
 * isso — a linha de defesa é o galho, não o system prompt.
 */
class AskLibraryUseCaseTest {

    /** Conta quantas vezes o modelo foi de fato chamado. Zero é o contrato da R2. */
    private class SpyAiRepository : AiRepository {
        var generateCalls = 0
            private set

        override suspend fun generate(request: AiRequest): Result<AiResult> {
            generateCalls++
            return Result.success(
                AiResult(
                    text = "resposta fundamentada",
                    providerId = "fake",
                    modelId = "fake-model",
                    metadata = mapOf("execution" to "local"),
                ),
            )
        }

        override suspend fun stream(request: AiRequest): Flow<String> {
            generateCalls++ // um stream também seria uma chamada ao modelo
            return flowOf("nunca deveria acontecer sem evidência")
        }
    }

    private val baco = RetrievedArticle(
        articleId = "org_baco",
        title = "Síndromes do Baço",
        summary = "Deficiência de Qi do Baço.",
        content = """
            # Baço
            ## Deficiência de Qi do Baço
            Cansaço, fezes amolecidas, língua pálida e pulso fraco.
        """.trimIndent(),
        provenance = Provenance.VERIFICAVEL,
    )

    /** Backend fake: casa por token expandido; sem match ⇒ lista vazia (contrato). */
    private class FakeBackend(private val corpus: List<RetrievedArticle>) : ArticleSearchBackend {
        override suspend fun search(query: String, maxResults: Int): List<RetrievedArticle> {
            val terms = MtcSearchEngine.expand(MtcSearchEngine.tokenize(query))
            if (terms.isEmpty()) return emptyList()
            return corpus.filter { article ->
                val haystack = MtcSearchEngine
                    .tokenize("${article.title} ${article.summary} ${article.content}").toSet()
                terms.any { it in haystack }
            }.take(maxResults)
        }
    }

    // -- The gate: no evidence => the model is NEVER touched -------------------

    @Test
    fun unknownTopic_neverCallsTheModel() = runTest {
        val ai = SpyAiRepository()
        val useCase = AskLibraryUseCase(MtcRetriever(FakeBackend(listOf(baco))), ai)

        val answer = useCase("tratamento de fratura exposta com parafuso titânio")

        assertEquals(AskLibraryUseCase.Answer.NoEvidence, answer)
        assertEquals(
            "Sem evidência, ai.generate NÃO pode ter sido chamado — este é o portão da R2",
            0,
            ai.generateCalls,
        )
    }

    @Test
    fun emptyLibrary_neverCallsTheModel_forAnyQuestion() = runTest {
        val ai = SpyAiRepository()
        val useCase = AskLibraryUseCase(MtcRetriever(FakeBackend(emptyList())), ai)

        listOf("deficiencia de qi do baco", "baco", "qi", "").forEach { q ->
            val answer = useCase(q)
            assertEquals("'$q' num acervo vazio deve ser NoEvidence", AskLibraryUseCase.Answer.NoEvidence, answer)
        }
        assertEquals("Acervo vazio: o modelo nunca é consultado", 0, ai.generateCalls)
    }

    @Test
    fun blankQuestion_shortCircuitsBeforeAnySearchOrModelCall() = runTest {
        val ai = SpyAiRepository()
        val useCase = AskLibraryUseCase(MtcRetriever(FakeBackend(listOf(baco))), ai)

        val answer = useCase("   ")

        assertEquals(AskLibraryUseCase.Answer.NoEvidence, answer)
        assertEquals(0, ai.generateCalls)
    }

    // -- Positive control: with evidence, the model IS called exactly once ----

    @Test
    fun withEvidence_theModelIsCalledExactlyOnce_andAnswerIsGrounded() = runTest {
        val ai = SpyAiRepository()
        val useCase = AskLibraryUseCase(MtcRetriever(FakeBackend(listOf(baco))), ai)

        val answer = useCase("sintomas de deficiencia de qi do baco")

        assertTrue("Com evidência, a resposta deve ser fundamentada", answer is AskLibraryUseCase.Answer.Grounded)
        assertEquals("O modelo deve ser chamado uma única vez quando há evidência", 1, ai.generateCalls)
        val grounded = answer as AskLibraryUseCase.Answer.Grounded
        assertTrue("A resposta deve carregar as passagens para verificação", grounded.sources.isNotEmpty())
    }

    // -- Idioma da resposta --------------------------------------------------

    /**
     * 97% do acervo aberto (1.272 de 1.303 itens vindos de PubMed/WHO/Europe PMC) está
     * em INGLÊS. Um modelo pequeno espelha o idioma do contexto, então trechos em inglês
     * produziam respostas em inglês para uma médica brasileira. O prompt ser *escrito* em
     * português nunca bastou — ele precisa MANDAR responder em português.
     */
    @Test
    fun systemPromptDemandsBrazilianPortugueseOutput() {
        val prompt = MtcRetriever.SYSTEM_PROMPT.lowercase()
        assertTrue(
            "O prompt do RAG precisa exigir resposta em português do Brasil",
            prompt.contains("português do brasil") || prompt.contains("portugues do brasil"),
        )
        assertTrue(
            "O prompt precisa instruir a TRADUZIR quando o trecho vier em inglês",
            prompt.contains("tradu"),
        )
    }

    /**
     * Regressão: exigir tradução não pode virar licença para o modelo completar lacunas
     * com conhecimento próprio. As duas regras convivem — traduzir o que está no trecho,
     * nunca acrescentar o que não está.
     */
    @Test
    fun translationInstructionDidNotWeakenTheNoOwnKnowledgeRule() {
        val prompt = MtcRetriever.SYSTEM_PROMPT.lowercase()
        assertTrue(
            "O prompt precisa seguir proibindo conhecimento próprio",
            prompt.contains("não use conhecimento próprio") || prompt.contains("nao use conhecimento proprio"),
        )
        assertTrue(
            "O prompt precisa seguir proibindo inventar pontos/fórmulas/estudos",
            prompt.contains("nunca invente"),
        )
    }
}
