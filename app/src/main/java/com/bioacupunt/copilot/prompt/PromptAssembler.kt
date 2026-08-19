package com.bioacupunt.copilot.prompt

import com.bioacupunt.copilot.retrieval.IntentType

/**
 * §40-41 PROMPT ASSEMBLER
 *
 * Builds structured prompts for the LLM with clear separation of concerns:
 * - system rules
 * - clinical context
 * - patient context
 * - retrieved evidence
 * - structured reasoning
 * - user query
 * - response schema
 *
 * Always requests structured output when supported.
 */
class PromptAssembler {

    data class PromptContext(
        val systemRules: String = "",
        val clinicalContext: String = "",
        val patientContext: String = "",
        val retrievedEvidence: String = "",
        val structuredReasoning: String = "",
        val userQuery: String,
        val responseSchema: String = "",
        val intent: IntentType = IntentType.KNOWLEDGE_SEARCH,
    )

    /**
     * Assemble a complete prompt from structured components.
     */
    fun assemble(context: PromptContext): String = buildString {
        // System rules (always first)
        if (context.systemRules.isNotBlank()) {
            appendLine("=== REGRAS DO SISTEMA ===")
            appendLine(context.systemRules)
            appendLine()
        }

        // Clinical context
        if (context.clinicalContext.isNotBlank()) {
            appendLine("=== CONTEXTO CLÍNICO ===")
            appendLine(context.clinicalContext)
            appendLine()
        }

        // Patient context
        if (context.patientContext.isNotBlank()) {
            appendLine("=== CONTEXTO DO PACIENTE ===")
            appendLine(context.patientContext)
            appendLine()
        }

        // Retrieved evidence
        if (context.retrievedEvidence.isNotBlank()) {
            appendLine("=== EVIDÊNCIA RECUPERADA ===")
            appendLine(context.retrievedEvidence)
            appendLine()
        }

        // Structured reasoning
        if (context.structuredReasoning.isNotBlank()) {
            appendLine("=== RACIOCÍNIO ESTRUTURADO ===")
            appendLine(context.structuredReasoning)
            appendLine()
        }

        // User query (always last)
        appendLine("=== PERGUNTA ===")
        appendLine(context.userQuery)
        appendLine()

        // Response schema
        if (context.responseSchema.isNotBlank()) {
            appendLine("=== SCHEMA DA RESPOSTA ===")
            appendLine(context.responseSchema)
        }
    }

    /**
     * Build system prompt for the copilot.
     */
    fun buildSystemPrompt(intent: IntentType): String {
        return buildString {
            appendLine("Você é um assistente clínico de Medicina Tradicional Chinesa.")
            appendLine()
            appendLine("REGRAS:")
            appendLine("1. NUNCA invente evidência, fonte ou relação.")
            appendLine("2. NUNCA altere o ranking de diferenciais.")
            appendLine("3. NUNCA crie diagnóstico definitivo.")
            appendLine("4. NUNCA prescreva automaticamente.")
            appendLine("5. NUNCA altere prontuário sem revisão humana.")
            appendLine("6. SEMPRE cite a fonte quando disponível.")
            appendLine("7. Se não houver evidência suficiente, diga EXPLICITAMENTE.")
            appendLine("8. Responda em português brasileiro.")
            appendLine()

            when (intent) {
                IntentType.KNOWLEDGE_SEARCH -> appendLine("Modo: Busca de conhecimento MTC.")
                IntentType.CLINICAL_ANALYSIS -> appendLine("Modo: Análise clínica — forneça análise baseada em evidências.")
                IntentType.PATIENT_SUMMARY -> appendLine("Modo: Resumo de paciente — gere rascunho revisável.")
                IntentType.DIFFERENTIAL_EXPLANATION -> appendLine("Modo: Explicação de diferenciais — explique o ranking.")
                IntentType.MISSING_DATA -> appendLine("Modo: Dados faltantes — identifique gaps.")
                IntentType.EVIDENCE_LOOKUP -> appendLine("Modo: Busca de evidência — trace a cadeia completa.")
                IntentType.POINT_LOOKUP -> appendLine("Modo: Busca de pontos — forneça informações do ponto.")
                IntentType.FORMULA_LOOKUP -> appendLine("Modo: Busca de fórmulas — forneça composição e indicações.")
                IntentType.PROTOCOL_LOOKUP -> appendLine("Modo: Busca de protocolos — forneça protocolo baseado em evidências.")
                IntentType.RESEARCH_QUERY -> appendLine("Modo: Pesquisa — forneça resultados da biblioteca curada.")
                IntentType.GENERAL_CLINICAL_QUERY -> appendLine("Modo: Consulta clínica geral.")
            }
        }
    }
}
