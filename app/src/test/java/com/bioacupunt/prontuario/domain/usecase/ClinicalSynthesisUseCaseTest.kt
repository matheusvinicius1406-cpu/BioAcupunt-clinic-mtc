package com.bioacupunt.prontuario.domain.usecase

import com.bioacupunt.ai.core.AiRepository
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.ai.core.AiResult
import com.bioacupunt.biblioteca.domain.ingestion.Provenance
import com.bioacupunt.biblioteca.domain.search.ArticleSearchBackend
import com.bioacupunt.biblioteca.domain.search.MtcRetriever
import com.bioacupunt.biblioteca.domain.search.MtcSearchEngine
import com.bioacupunt.biblioteca.domain.search.RetrievedArticle
import com.bioacupunt.prontuario.domain.model.ConfidenceLevel
import com.bioacupunt.prontuario.domain.model.MtcAssessment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * O "terceiro caminho" ([ClinicalSynthesisUseCase]) nunca teve cobertura de teste —
 * achado na auditoria de 2026-07-29. Roda sob Robolectric pelo mesmo motivo de
 * [StructureChiefComplaintUseCaseTest]: o parser usa `org.json.JSONObject`, que só
 * tem comportamento real sob o shadow do Robolectric.
 *
 * O que importa travar aqui não é a qualidade do JSON gerado (isso é responsabilidade
 * do modelo) — é o contrato de degradação: falha em qualquer ponto (provider
 * indisponível, JSON malformado) nunca propaga exceção pra médica, sempre vira
 * [ClinicalSynthesis.EMPTY].
 */
@RunWith(RobolectricTestRunner::class)
class ClinicalSynthesisUseCaseTest {

    private class FakeAiRepository(
        private val response: Result<AiResult> = Result.success(AiResult(text = "{}", providerId = "fake", modelId = "fake")),
    ) : AiRepository {
        var lastRequest: AiRequest? = null
            private set

        override suspend fun generate(request: AiRequest): Result<AiResult> {
            lastRequest = request
            return response
        }

        override suspend fun stream(request: AiRequest): Flow<String> = flowOf("")
    }

    private val bacoArticle = RetrievedArticle(
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

    private fun assessmentWithComplaint(complaint: String) = MtcAssessment(patientId = 1L, chiefComplaint = complaint)

    // -- A síntese sempre prefere o modelo local (único provider do app) ----

    @Test
    fun alwaysPrefersLocal_withOrWithoutEvidence() = runTest {
        val ai = FakeAiRepository()
        val withEvidence = ClinicalSynthesisUseCase(ai, MtcRetriever(FakeBackend(listOf(bacoArticle))))
        withEvidence(assessmentWithComplaint("cansaço e fezes amolecidas, deficiência de qi do baço"))
        assertTrue("Requisição deve ter sido feita", ai.lastRequest != null)
        assertTrue("Deve preferir o modelo local", ai.lastRequest!!.preferLocal)

        val withoutEvidence = ClinicalSynthesisUseCase(ai, MtcRetriever(FakeBackend(emptyList())))
        withoutEvidence(assessmentWithComplaint("quadro sem nenhum artigo correspondente no acervo"))
        assertTrue("Requisição deve ter sido feita mesmo sem evidência", ai.lastRequest != null)
        assertTrue("Deve preferir o modelo local mesmo sem evidência (não há mais fallback de nuvem)", ai.lastRequest!!.preferLocal)
    }

    // -- Degradação: falha do provider nunca propaga exceção -----------------

    @Test
    fun providerFailure_degradesToEmpty_neverThrows() = runTest {
        val ai = FakeAiRepository(Result.failure(RuntimeException("provider indisponível")))
        val useCase = ClinicalSynthesisUseCase(ai, MtcRetriever(FakeBackend(listOf(bacoArticle))))

        val result = useCase(assessmentWithComplaint("cansaço, deficiência de qi do baço"))

        assertTrue("Falha do provider deve degradar para síntese vazia, nunca lançar", result.isEmpty)
    }

    // -- Degradação: JSON malformado nunca propaga exceção -------------------

    @Test
    fun malformedJson_degradesToEmpty_neverThrows() = runTest {
        val ai = FakeAiRepository(Result.success(AiResult(text = "isto não é JSON nenhum {{{", providerId = "fake", modelId = "fake")))
        val useCase = ClinicalSynthesisUseCase(ai, MtcRetriever(FakeBackend(listOf(bacoArticle))))

        val result = useCase(assessmentWithComplaint("cansaço, deficiência de qi do baço"))

        assertTrue("JSON malformado deve degradar para síntese vazia, nunca lançar", result.isEmpty)
    }

    // -- Parsing feliz: campos estruturados chegam corretos -------------------

    @Test
    fun wellFormedJson_parsesStructuredFields() = runTest {
        val json = """
            {
              "tcmDiagnosis": {"patternName": "Deficiência de Qi do Baço", "confidence": "alto"},
              "biomedicalDiagnosis": {"diagnosis": "Síndrome do intestino irritável", "cidCode": "K58"},
              "differentialDiagnoses": [{"description": "Deficiência de Yang do Baço", "priority": 1}],
              "overallConfidence": "moderado"
            }
        """.trimIndent()
        val ai = FakeAiRepository(Result.success(AiResult(text = json, providerId = "fake", modelId = "fake")))
        val useCase = ClinicalSynthesisUseCase(ai, MtcRetriever(FakeBackend(listOf(bacoArticle))))

        val result = useCase(assessmentWithComplaint("cansaço, deficiência de qi do baço"))

        assertEquals("Deficiência de Qi do Baço", result.tcmDiagnosis?.patternName)
        assertEquals(ConfidenceLevel.HIGH, result.tcmDiagnosis?.confidence)
        assertEquals("K58", result.biomedicalDiagnosis?.cidCode)
        assertEquals(1, result.differentialDiagnoses.size)
        assertEquals(ConfidenceLevel.MODERATE, result.overallConfidence)
        assertFalse("Síntese com campos preenchidos não deve ser considerada vazia", result.isEmpty)
    }

    // -- Fontes da biblioteca sempre acompanham a síntese quando há evidência -

    @Test
    fun withEvidence_evidenceSourcesComeFromLibraryPassages() = runTest {
        val ai = FakeAiRepository(Result.success(AiResult(text = "{}", providerId = "fake", modelId = "fake")))
        val useCase = ClinicalSynthesisUseCase(ai, MtcRetriever(FakeBackend(listOf(bacoArticle))))

        val result = useCase(assessmentWithComplaint("cansaço e fezes amolecidas, deficiência de qi do baço"))

        assertTrue(
            "Com evidência, a síntese deve carregar a fonte da biblioteca usada",
            result.evidenceSources.any { it.title == bacoArticle.title },
        )
    }
}
