package com.bioacupunt.prontuario.domain.usecase

import com.bioacupunt.ai.core.AiRepository
import com.bioacupunt.ai.core.AiRequest
import org.json.JSONObject

/**
 * O que a médica já escreveu — reorganizado, nunca reinterpretado.
 *
 * Cada lista é uma sugestão dispensável/aceitável na UI; aceitar um item chama a
 * MESMA função (`SupremoViewModel.toggleRelieving`/`toggleAggravating`/
 * `toggleReviewOfSystems`) que um toque manual chamaria — não existe caminho de
 * escrita paralelo.
 */
data class ChiefComplaintExtraction(
    val aggravating: List<String> = emptyList(),
    val relieving: List<String> = emptyList(),
    val reviewOfSystemsHits: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = aggravating.isEmpty() && relieving.isEmpty() && reviewOfSystemsHits.isEmpty()

    companion object {
        val EMPTY = ChiefComplaintExtraction()
    }
}

/**
 * Estruturação EXTRATIVA do motivo da consulta — não é R2 (não responde pergunta
 * clínica da biblioteca) nem toca R1 (`ClinicalSafetyEngine` nunca importa isto).
 * É uma terceira coisa: reorganiza texto que a médica já autorou, nunca infere
 * diagnóstico, nunca sugere ponto/protocolo/CID. Cada resultado é uma sugestão que
 * ela aceita ou ignora — nunca gravado sozinho.
 *
 * Falha (provider indisponível, JSON malformado, resposta vazia) degrada
 * silenciosamente para [ChiefComplaintExtraction.EMPTY] — isto é um apoio, não um
 * caminho crítico; a médica nunca vê um erro por causa de uma sugestão que não veio.
 */
class StructureChiefComplaintUseCase(
    private val ai: AiRepository,
) {
    suspend operator fun invoke(text: String): ChiefComplaintExtraction {
        if (text.trim().length < MIN_LENGTH) return ChiefComplaintExtraction.EMPTY

        val request = AiRequest(
            prompt = text,
            systemPrompt = SYSTEM_PROMPT,
            temperature = 0.1,
            maxTokens = 400,
            preferLocal = true,
            taskHint = "chart-extraction",
        )

        return ai.generate(request).fold(
            onSuccess = { result -> runCatching { parse(result.text) }.getOrDefault(ChiefComplaintExtraction.EMPTY) },
            onFailure = { ChiefComplaintExtraction.EMPTY },
        )
    }

    private fun parse(raw: String): ChiefComplaintExtraction {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        val json = JSONObject(cleaned)

        fun stringList(key: String): List<String> {
            val arr = json.optJSONArray(key) ?: return emptyList()
            return (0 until arr.length()).mapNotNull { i -> arr.optString(i).takeIf { it.isNotBlank() } }
        }

        return ChiefComplaintExtraction(
            aggravating = stringList("aggravating"),
            relieving = stringList("relieving"),
            reviewOfSystemsHits = stringList("reviewOfSystems"),
        )
    }

    companion object {
        private const val MIN_LENGTH = 15

        private val SYSTEM_PROMPT = """
            Você é uma ferramenta de estruturação de texto, não uma assistente clínica.
            Sua única função é reorganizar em JSON o que a médica JÁ ESCREVEU sobre o
            motivo da consulta — nunca adicionar interpretação, diagnóstico, hipótese ou
            informação que não esteja explicitamente no texto.

            Regras absolutas:
            - NUNCA infira diagnóstico, causa ou padrão de Medicina Tradicional Chinesa.
            - NUNCA sugira ponto de acupuntura, protocolo, tratamento ou conduta.
            - Extraia SOMENTE o que está literalmente dito no texto.
            - Se o texto não mencionar algo, devolva lista vazia para aquele campo — nunca invente.

            Responda SOMENTE com um objeto JSON, sem texto antes ou depois, exatamente
            neste formato:
            {"aggravating": ["fator de piora citado", ...], "relieving": ["fator de melhora citado", ...], "reviewOfSystems": ["sintoma ou sistema citado", ...]}

            Cada item deve ser uma frase curta, preferencialmente nas palavras da própria
            médica.
        """.trimIndent()
    }
}
