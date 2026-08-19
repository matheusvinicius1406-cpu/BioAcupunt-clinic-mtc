package com.bioacupunt.copilot.rag

import com.bioacupunt.ai.core.AiRepository
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.copilot.retrieval.IntentType
import com.bioacupunt.copilot.retrieval.PatientContext

/**
 * §22-23 GROUNDED RESPONSE GENERATOR
 *
 * Generates LLM responses using structured context from retrieval.
 * The LLM is a LANGUAGE/EXPLANATION LAYER, not a source of truth.
 *
 * Flow:
 * ```text
 * userQuery + structuredContext + evidence + clinicalIntelligence + patientContext + responseRules
 *     ↓
 * PromptAssembly
 *     ↓
 * AiRepository.generate()
 *     ↓
 * GroundedResponse (answer, claims, citations, evidenceIds, reasoningPaths, uncertainties, warnings)
 * ```
 */
class GroundedResponseGenerator(
    private val aiRepository: AiRepository,
    private val evidenceResolutionService: EvidenceResolutionService,
) {

    data class GroundedResponse(
        val answer: String,
        val claims: List<String> = emptyList(),
        val citations: List<String> = emptyList(),
        val evidenceIds: List<String> = emptyList(),
        val reasoningPaths: List<String> = emptyList(),
        val uncertainties: List<String> = emptyList(),
        val warnings: List<String> = emptyList(),
        val confidence: String = "MODERATE",
        val knowledgeVersion: String? = null,
    )

    /**
     * Generate a grounded response using structured context.
     */
    suspend fun generate(
        query: String,
        context: ContextBuilder.StructuredContext,
        intent: IntentType,
        patientContext: PatientContext? = null,
    ): GroundedResponse {
        // 1. Assemble prompt
        val prompt = assemblePrompt(query, context, intent, patientContext)

        // 2. Call LLM
        val request = AiRequest(
            prompt = prompt,
            systemPrompt = SYSTEM_PROMPT,
            preferLocal = true,
        )

        val result = aiRepository.generate(request)

        return if (result.isSuccess) {
            val raw = result.getOrNull()?.text ?: ""
            parseResponse(raw, context.evidenceIds)
        } else {
            GroundedResponse(
                answer = "Não foi possível gerar resposta. O modelo local pode estar indisponível.",
                warnings = listOf("MODEL_UNAVAILABLE"),
                confidence = "INSUFFICIENT",
            )
        }
    }

    private fun assemblePrompt(
        query: String,
        context: ContextBuilder.StructuredContext,
        intent: IntentType,
        patientContext: PatientContext?,
    ): String = buildString {
        appendLine("=== INSTRUÇÕES ===")
        appendLine("Você é um assistente clínico de Medicina Tradicional Chinesa.")
        appendLine("Responda SOMENTE com base no contexto fornecido abaixo.")
        appendLine("NUNCA invente informações, fontes ou evidências.")
        appendLine("Sempre cite a fonte quando disponível.")
        appendLine("Se o contexto não contiver informação suficiente, diga explicitamente.")
        appendLine()

        appendLine("=== CONTEXTO ===")
        appendLine(ContextBuilder().formatForPrompt(context))
        appendLine()

        if (patientContext != null) {
            appendLine("=== CONTEXTO DO PACIENTE ===")
            appendLine("Paciente ativo: ${patientContext.activePatient}")
            if (patientContext.currentAssessment != null) {
                appendLine("Avaliação atual: ${patientContext.currentAssessment}")
            }
            appendLine()
        }

        appendLine("=== PERGUNTA DO USUÁRIO ===")
        appendLine(query)
        appendLine()

        appendLine("=== RESPOSTA ESPERADA ===")
        appendLine("Responda em JSON estruturado com:")
        appendLine("- answer: resposta em texto claro")
        appendLine("- claims: lista de afirmações feitas")
        appendLine("- citations: fontes citadas")
        appendLine("- uncertainties: incertezas identificadas")
        appendLine("- warnings: avisos relevantes")
    }

    private fun parseResponse(raw: String, evidenceIds: List<String>): GroundedResponse {
        // Simple JSON parsing — degrade gracefully if malformed
        return try {
            val cleaned = raw
                .replace(Regex("```json\\s*"), "")
                .replace(Regex("```\\s*"), "")
                .trim()

            val answer = extractJsonField(cleaned, "answer") ?: raw
            val claims = extractJsonList(cleaned, "claims")
            val citations = extractJsonList(cleaned, "citations")
            val uncertainties = extractJsonList(cleaned, "uncertainties")
            val warnings = extractJsonList(cleaned, "warnings")

            GroundedResponse(
                answer = answer,
                claims = claims,
                citations = citations,
                evidenceIds = evidenceIds,
                uncertainties = uncertainties,
                warnings = warnings,
                confidence = if (evidenceIds.isNotEmpty()) "MODERATE" else "LOW",
            )
        } catch (e: Exception) {
            GroundedResponse(
                answer = raw,
                evidenceIds = evidenceIds,
                confidence = "LOW",
                warnings = listOf("PARSE_ERROR"),
            )
        }
    }

    private fun extractJsonField(json: String, field: String): String? {
        val regex = Regex("\"$field\"\\s*:\\s*\"([^\"]*)\"")
        return regex.find(json)?.groupValues?.get(1)
    }

    private fun extractJsonList(json: String, field: String): List<String> {
        val regex = Regex("\"$field\"\\s*:\\s*\\[([^\\]]*)\\]")
        val match = regex.find(json) ?: return emptyList()
        return match.groupValues[1]
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }
    }

    companion object {
        val SYSTEM_PROMPT = """
Você é um assistente clínico especializado em Medicina Tradicional Chinesa.

REGRAS INVIOLÁVEIS:
1. NUNCA invente evidência, fonte ou relação.
2. NUNCA altere o ranking de diferenciais.
3. NUNCA crie diagnóstico definitivo.
4. NUNCA prescreva automaticamente.
5. NUNCA altere prontuário sem revisão humana explícita.
6. SEMPRE cite a fonte quando disponível.
7. Se o contexto não contiver informação suficiente, diga EXPLICITAMENTE "não há evidência suficiente".
8. Responda em português brasileiro.
5. Formato: JSON estruturado com answer, claims, citations, uncertainties, warnings.
        """.trimIndent()
    }
}
