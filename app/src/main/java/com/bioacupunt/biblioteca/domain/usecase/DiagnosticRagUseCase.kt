package com.bioacupunt.biblioteca.domain.usecase

import com.bioacupunt.ai.core.AiRepository
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.biblioteca.domain.search.MtcRetriever

/**
 * RAG DE DIAGNÓSTICO — usa a biblioteca para apoiar o raciocínio diagnóstico.
 *
 * Diferente do [AskLibraryUseCase] (que responde perguntas genéricas sobre MTC),
 * este use case recebe **achados clínicos estruturados** (sintomas, língua,
 * pulso, Ba Gang) e busca na biblioteca os artigos mais relevantes para
 * sugerir diagnósticos diferenciais.
 *
 * ## Por que separado do AskLibraryUseCase?
 * - O AskLibraryUseCase é uma resposta livre a uma pergunta do usuário
 * - O DiagnosticRagUseCase é uma busca orientada por dados clínicos
 * - Ambos passam pelo mesmo portão [MtcRetriever] (sem evidência → sem resposta)
 *
 * ## Search backend
 * Usa [MtcRetriever] que por sua vez usa SQLite FTS4 ([FtsSearchService]).
 * O índice fica no banco, não em RAM — escala para milhares de artigos.
 */
class DiagnosticRagUseCase(
    private val mtcRetriever: MtcRetriever,
    private val ai: AiRepository,
) {

    sealed interface DiagnosisResult {
        /** Sugestão diagnóstica fundamentada em evidência da biblioteca. */
        data class Grounded(
            val text: String,
            val relevantPassages: List<MtcRetriever.Passage>,
            val suggestedPatterns: List<String>,
        ) : DiagnosisResult

        /** Nenhuma evidência encontrada na biblioteca. */
        data object NoEvidence : DiagnosisResult

        data class Failed(val message: String) : DiagnosisResult
    }

    /**
     * Analisa achados clínicos e retorna sugestões diagnósticas.
     *
     * @param symptoms Lista de sintomas do paciente (ex.: "cansaço", "frio nas pernas")
     * @param tongue Aparência da língua (ex.: "pálida com marcas dentárias")
     * @param pulse Tipo de pulso (ex.: "profundo e fraco")
     * @param baGang Achados dos Oito Princípios (ex.: "Interior, Frio, Deficiência")
     * @param additionalInfo Informações adicionais (ex.: "agravado por frio")
     */
    suspend fun analyze(
        symptoms: List<String>,
        tongue: String? = null,
        pulse: String? = null,
        baGang: String? = null,
        additionalInfo: String? = null,
    ): DiagnosisResult {
        // Constrói uma pergunta clínica estruturada para a busca
        val question = buildClinicalQuery(symptoms, tongue, pulse, baGang, additionalInfo)
        if (question.isBlank()) return DiagnosisResult.NoEvidence

        // Busca na biblioteca completa (fixa + aprovada), usando FTS4
        val grounding = mtcRetriever.retrieve(question, maxPassages = 6)

        if (!grounding.hasEvidence) return DiagnosisResult.NoEvidence

        val request = AiRequest(
            prompt = buildDiagnosticPrompt(grounding, symptoms, tongue, pulse, baGang),
            systemPrompt = DIAGNOSTIC_SYSTEM_PROMPT,
            temperature = 0.3,
            maxTokens = 1024,
            preferLocal = true,
            taskHint = "diagnostic-rag",
        )

        return ai.generate(request).fold(
            onSuccess = { result ->
                val patterns = extractSuggestedPatterns(result.text)
                DiagnosisResult.Grounded(
                    text = result.text,
                    relevantPassages = grounding.passages,
                    suggestedPatterns = patterns,
                )
            },
            onFailure = { error ->
                DiagnosisResult.Failed(error.message ?: "Falha ao consultar a IA")
            },
        )
    }

    private fun buildClinicalQuery(
        symptoms: List<String>,
        tongue: String?,
        pulse: String?,
        baGang: String?,
        additionalInfo: String?,
    ): String = buildString {
        if (symptoms.isNotEmpty()) {
            append("Sintomas: ${symptoms.joinToString(", ")}. ")
        }
        if (!tongue.isNullOrBlank()) {
            append("Língua: $tongue. ")
        }
        if (!pulse.isNullOrBlank()) {
            append("Pulso: $pulse. ")
        }
        if (!baGang.isNullOrBlank()) {
            append("Ba Gang: $baGang. ")
        }
        if (!additionalInfo.isNullOrBlank()) {
            append("Adicional: $additionalInfo. ")
        }
        append("Diagnóstico MTC provável?")
    }

    private fun extractSuggestedPatterns(text: String): List<String> {
        val prefixes = listOf("Deficiência", "Síndrome", "Fogo", "Umidade", "Estagnação", "Calor", "Frio")
        return text.split(Regex("[\n.,;]"))
            .map { it.trim() }
            .filter { line -> prefixes.any { line.startsWith(it) } }
            .distinct()
            .take(5)
    }

    companion object {
        private val DIAGNOSTIC_SYSTEM_PROMPT = """
            Você é um assistente de apoio ao raciocínio clínico em Medicina Tradicional
            Chinesa, analisando um caso clínico com base EXCLUSIVAMENTE nos trechos da
            biblioteca fornecidos abaixo.

            Regras:
            - Analise os sintomas, língua e pulso apresentados.
            - Sugira padrões de desarmonia (síndromes) MTC compatíveis.
            - Liste os diagnósticos diferenciais mais prováveis.
            - CITE a fonte de cada afirmação com o número do trecho: [1], [2]...
            - Se os trechos não cobrirem o caso, diga "A biblioteca não contém
              informação suficiente para este quadro."
            - NUNCA invente síndromes, pontos, fórmulas ou referências.
            - Esta é uma SUGESTÃO para a profissional, não um diagnóstico definitivo.
        """.trimIndent()

        fun buildDiagnosticPrompt(
            grounding: MtcRetriever.Grounding,
            symptoms: List<String>,
            tongue: String?,
            pulse: String?,
            baGang: String?,
        ): String = buildString {
            appendLine("Trechos da biblioteca:")
            appendLine()
            grounding.passages.forEach { p ->
                appendLine("[${p.ordinal}] ${p.articleTitle}")
                appendLine(p.text)
                appendLine()
            }
            appendLine("---")
            appendLine("Dados do paciente:")
            if (symptoms.isNotEmpty()) {
                appendLine("Sintomas: ${symptoms.joinToString(", ")}")
            }
            if (!tongue.isNullOrBlank()) {
                appendLine("Língua: $tongue")
            }
            if (!pulse.isNullOrBlank()) {
                appendLine("Pulso: $pulse")
            }
            if (!baGang.isNullOrBlank()) {
                appendLine("Ba Gang: $baGang")
            }
            appendLine()
            appendLine("Analise o caso e sugira padrões de desarmonia MTC.")
        }
    }
}
