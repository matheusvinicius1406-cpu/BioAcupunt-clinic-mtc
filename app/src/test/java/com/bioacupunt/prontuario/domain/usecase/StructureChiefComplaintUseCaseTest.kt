package com.bioacupunt.prontuario.domain.usecase

import com.bioacupunt.ai.core.AiRepository
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.ai.core.AiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Esta extração é um apoio, não um caminho crítico — o contrato que importa é:
 * ela NUNCA propaga erro pra médica, e NUNCA inventa dado quando falha. Falha vira
 * [ChiefComplaintExtraction.EMPTY] em silêncio, sempre.
 *
 * Roda sob Robolectric (não um JUnit puro) porque `org.json.JSONObject` — usado
 * pelo parser desta classe — é um stub que lança em cima do android.jar puro; só o
 * shadow do Robolectric devolve o comportamento real.
 */
@RunWith(RobolectricTestRunner::class)
class StructureChiefComplaintUseCaseTest {

    private class FakeAiRepository(
        private val response: Result<AiResult> = Result.success(AiResult(text = "{}", providerId = "fake", modelId = "fake")),
    ) : AiRepository {
        var generateCalls = 0
            private set

        override suspend fun generate(request: AiRequest): Result<AiResult> {
            generateCalls++
            lastRequest = request
            return response
        }

        override suspend fun stream(request: AiRequest): Flow<String> = flowOf("")

        var lastRequest: AiRequest? = null
            private set
    }

    @Test
    fun `well formed JSON extracts every field`() = runTest {
        val json = """
            {"aggravating": ["frio", "ficar em pé muito tempo"], "relieving": ["repouso"], "reviewOfSystems": ["insônia"]}
        """.trimIndent()
        val ai = FakeAiRepository(Result.success(AiResult(text = json, providerId = "fake", modelId = "fake")))
        val useCase = StructureChiefComplaintUseCase(ai)

        val result = useCase("Dor lombar há 3 semanas, piora com frio e muito tempo em pé, melhora com repouso. Também relata insônia.")

        assertEquals(listOf("frio", "ficar em pé muito tempo"), result.aggravating)
        assertEquals(listOf("repouso"), result.relieving)
        assertEquals(listOf("insônia"), result.reviewOfSystemsHits)
    }

    @Test
    fun `JSON wrapped in markdown fences still parses`() = runTest {
        val fenced = "```json\n{\"aggravating\": [\"calor\"], \"relieving\": [], \"reviewOfSystems\": []}\n```"
        val ai = FakeAiRepository(Result.success(AiResult(text = fenced, providerId = "fake", modelId = "fake")))
        val useCase = StructureChiefComplaintUseCase(ai)

        val result = useCase("Dor que piora muito com calor, texto grande o bastante para passar do mínimo.")

        assertEquals(listOf("calor"), result.aggravating)
    }

    @Test
    fun `malformed JSON degrades to EMPTY, never throws`() = runTest {
        val ai = FakeAiRepository(Result.success(AiResult(text = "isto não é JSON de forma alguma", providerId = "fake", modelId = "fake")))
        val useCase = StructureChiefComplaintUseCase(ai)

        val result = useCase("Texto longo o suficiente pra passar do mínimo de caracteres exigido pela extração.")

        assertEquals(ChiefComplaintExtraction.EMPTY, result)
    }

    @Test
    fun `provider failure degrades to EMPTY, never propagates the error`() = runTest {
        val ai = FakeAiRepository(Result.failure(RuntimeException("sem provider disponível")))
        val useCase = StructureChiefComplaintUseCase(ai)

        val result = useCase("Texto longo o suficiente pra passar do mínimo de caracteres exigido pela extração.")

        assertEquals(ChiefComplaintExtraction.EMPTY, result)
    }

    @Test
    fun `text shorter than the minimum never calls the model`() = runTest {
        val ai = FakeAiRepository()
        val useCase = StructureChiefComplaintUseCase(ai)

        val result = useCase("dor")

        assertEquals(ChiefComplaintExtraction.EMPTY, result)
        assertEquals("Texto curto demais não deve gastar uma chamada de IA", 0, ai.generateCalls)
    }

    @Test
    fun `blank text never calls the model`() = runTest {
        val ai = FakeAiRepository()
        val useCase = StructureChiefComplaintUseCase(ai)

        useCase("   ")

        assertEquals(0, ai.generateCalls)
    }

    @Test
    fun `the request never asks for diagnosis or treatment suggestions`() = runTest {
        val ai = FakeAiRepository()
        val useCase = StructureChiefComplaintUseCase(ai)

        useCase("Texto longo o suficiente pra disparar uma chamada real de verdade ao provider fake.")

        val prompt = ai.lastRequest?.systemPrompt.orEmpty()
        assertTrue("O prompt deve proibir explicitamente inferência diagnóstica", prompt.contains("NUNCA infira diagnóstico"))
        assertTrue("O prompt deve proibir explicitamente sugestão de tratamento", prompt.contains("NUNCA sugira ponto"))
    }
}
