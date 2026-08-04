package com.bioacupunt.biblioteca.domain.usecase

import com.bioacupunt.ai.core.AiRepository
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.ai.core.AiResult
import com.bioacupunt.biblioteca.domain.model.MtcArticle
import com.bioacupunt.biblioteca.domain.model.TranslationLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * O tradutor transforma texto já aprovado pela médica (R4: nunca inventa fato novo — ver
 * javadoc de [TranslateArticleUseCase]). Estes testes cobrem as duas garantias que
 * importam: (1) o chunking preserva o NÍVEL do heading, não só o texto — é o que faz
 * "preservar formatação" ser verdade, não só uma frase no prompt; (2) uma seção que falha
 * aborta a tradução inteira, nunca publica um artigo meio-traduzido.
 *
 * Roda sob Robolectric pelo mesmo motivo de [com.bioacupunt.prontuario.domain.usecase.ClinicalSynthesisUseCaseTest]:
 * `parseMetadata` usa `org.json.JSONObject`, que só tem comportamento real sob o shadow do Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class TranslateArticleUseCaseTest {

    private class FakeAiRepository(
        private val metadataResponse: (AiRequest) -> Result<AiResult> = { defaultMetadata() },
        private val contentResponse: (AiRequest) -> Result<AiResult> = { defaultContent(it) },
    ) : AiRepository {
        var contentCalls = 0
            private set

        override suspend fun generate(request: AiRequest): Result<AiResult> = when (request.taskHint) {
            "article-translation-metadata" -> metadataResponse(request)
            "article-translation-content" -> {
                contentCalls++
                contentResponse(request)
            }
            else -> Result.failure(IllegalStateException("taskHint inesperado: ${request.taskHint}"))
        }

        override suspend fun stream(request: AiRequest): Flow<String> = flowOf("")

        companion object {
            fun defaultMetadata(): Result<AiResult> = Result.success(
                AiResult(
                    text = """{"title": "Translated Title", "summary": "Translated summary", "tags": ["tagA", "tagB"]}""",
                    providerId = "fake",
                    modelId = "fake-model",
                ),
            )

            fun defaultContent(request: AiRequest): Result<AiResult> = Result.success(
                AiResult(text = "TRANSLATED[${request.prompt}]", providerId = "fake", modelId = "fake-model"),
            )
        }
    }

    private fun article(content: String) = MtcArticle(
        id = "art-1",
        title = "Original title",
        category = "PONTOS",
        summary = "Original summary",
        content = content,
        tags = listOf("orig-tag"),
        provenance = "VERIFICAVEL",
    )

    @Test
    fun chunkContent_preservesHeadingLevel() {
        val useCase = TranslateArticleUseCase(FakeAiRepository())
        val markdown = "# Título\nIntro.\n\n## Sub A\nTexto A.\n\n### Sub B\nTexto B."

        val chunks = useCase.chunkContent(markdown)

        assertEquals(3, chunks.size)
        assertTrue(chunks[0].startsWith("# Título"))
        assertTrue(chunks[1].startsWith("## Sub A"))
        assertTrue(chunks[2].startsWith("### Sub B"))
    }

    @Test
    fun invoke_translatesMetadataAndEachSection_thenJoinsInOrder() = runTest {
        val ai = FakeAiRepository()
        val useCase = TranslateArticleUseCase(ai)
        val a = article("# A\nTexto A.\n\n## B\nTexto B.")

        val outcome = useCase(a, TranslationLanguage.EN)

        require(outcome is TranslateArticleUseCase.Outcome.Success)
        assertEquals("Translated Title", outcome.title)
        assertEquals("Translated summary", outcome.summary)
        assertEquals(listOf("tagA", "tagB"), outcome.tags)
        assertTrue("cada seção deve ter sido traduzida separadamente", outcome.content.contains("TRANSLATED[# A"))
        assertTrue(outcome.content.contains("TRANSLATED[## B"))
        assertEquals("uma chamada ao modelo por seção", 2, ai.contentCalls)
    }

    @Test
    fun invoke_metadataFailure_neverCallsContentTranslation() = runTest {
        val ai = FakeAiRepository(metadataResponse = { Result.failure(IllegalStateException("boom")) })
        val useCase = TranslateArticleUseCase(ai)

        val outcome = useCase(article("# A\nTexto A."), TranslationLanguage.EN)

        assertTrue(outcome is TranslateArticleUseCase.Outcome.Error)
        assertEquals("Falha de metadado não deve gastar chamada nenhuma traduzindo conteúdo", 0, ai.contentCalls)
    }

    @Test
    fun invoke_oneSectionFails_abortsWithoutTranslatingTheRest() = runTest {
        var call = 0
        val ai = FakeAiRepository(contentResponse = { req ->
            call++
            if (call == 2) Result.failure(IllegalStateException("modelo caiu")) else FakeAiRepository.defaultContent(req)
        })
        val useCase = TranslateArticleUseCase(ai)
        val a = article("# A\nTexto A.\n\n## B\nTexto B.\n\n### C\nTexto C.")

        val outcome = useCase(a, TranslationLanguage.EN)

        assertTrue(outcome is TranslateArticleUseCase.Outcome.Error)
        val error = outcome as TranslateArticleUseCase.Outcome.Error
        assertTrue("mensagem deve indicar qual seção falhou", error.message.contains("2"))
        assertEquals("não deve tentar traduzir a 3ª seção depois da 2ª falhar", 2, ai.contentCalls)
    }

    @Test
    fun invoke_withoutSourceLanguageDetection_alwaysCallsTheModel() = runTest {
        // Sem detecção de idioma-fonte (YAGNI documentado no use case) — quem decide "já
        // está no idioma certo, devolva sem alteração" é o PROMPT, não um atalho em código.
        val ai = FakeAiRepository()
        val useCase = TranslateArticleUseCase(ai)

        useCase(article("# A\nTexto A."), TranslationLanguage.PT_BR)

        assertEquals(1, ai.contentCalls)
    }
}
