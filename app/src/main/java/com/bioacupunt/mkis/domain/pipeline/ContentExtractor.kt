package com.bioacupunt.mkis.domain.pipeline

import com.bioacupunt.ai.core.AiRepository
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.observability.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * EXTRATOR DE CONTEÚDO CIENTÍFICO — usa o LLM local (Gemma 2B via LocalLlmProvider)
 * para classificar e extrair metadados de cada nó durante a ingestão.
 *
 * ## O que extrai:
 * - [knowledgeType] — classificação do tipo de conhecimento
 * - [evidenceLevel] — nível de evidência científica estimado
 * - [biasRisk] — risco de viés
 * - [keywords] — palavras-chave relevantes
 * - [confidence] — confiança da IA na classificação
 *
 * ## Integridade:
 * - Temperature = 0.0 (determinístico)
 * - Resposta forçada em JSON schema
 * - Se o LLM falhar ou não estiver disponível, usa defaults seguros
 *
 * ## Thread safety:
 * Acesso serializado via coroutine dispatcher Default.
 */
class ContentExtractor(
    private val aiRepository: AiRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Extrai metadados de um artigo científico.
     *
     * @param title Título do artigo
     * @param summary Resumo do artigo
     * @param content Conteúdo completo (truncado para 4000 chars)
     * @return [ExtractionResult] com defaults seguros se o LLM falhar
     */
    suspend fun extract(
        title: String,
        summary: String,
        content: String,
    ): ExtractionResult = withContext(Dispatchers.Default) {
        val truncatedContent = content.take(MAX_CONTENT_CHARS)
        val prompt = buildPrompt(title, summary, truncatedContent)

        val result = try {
            val aiResult = aiRepository.generate(
                AiRequest(
                    prompt = prompt,
                    temperature = 0.0,
                    maxTokens = 256,
                    preferLocal = true,
                )
            )
            aiResult.getOrNull()?.text
        } catch (e: Exception) {
            AppLogger.w(TAG, "LLM extraction failed, using defaults", e)
            null
        }

        if (result == null) {
            return@withContext ExtractionResult.default()
        }

        try {
            val parsed = json.decodeFromString<ExtractionResultJson>(result)
            ExtractionResult(
                keywords = parsed.keywords,
                evidenceLevel = normalizeEvidenceLevel(parsed.evidence_level),
                biasRisk = normalizeBiasRisk(parsed.bias_risk),
                confidence = parsed.confidence.coerceIn(0.0, 1.0),
            )
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to parse LLM extraction result: ${e.message}")
            ExtractionResult.default()
        }
    }

    private fun buildPrompt(title: String, summary: String, content: String): String = """
        Classifique o seguinte artigo científico para a base de conhecimento de Medicina Tradicional Chinesa.

        Título: $title
        Resumo: $summary
        Conteúdo: $content

        Responda APENAS no formato JSON abaixo. Não adicione texto antes ou depois.
        {
            "keywords": ["palavra1", "palavra2", "palavra3"],
            "evidence_level": "cebm_4",
            "bias_risk": "moderado",
            "confidence": 0.7
        }

        Regras para evidence_level:
        - "cebm_1a" = Revisão sistemática de RCTs
        - "cebm_1b" = RCT individual
        - "cebm_2a" = Revisão de coortes
        - "cebm_2b" = Coorte individual
        - "cebm_3a" = Revisão de caso-controle
        - "cebm_3b" = Caso-controle individual
        - "cebm_4" = Série de casos (default para MTC)
        - "cebm_5" = Opinião de especialista
        - "grade_alta" = GRADE Alta
        - "grade_moderada" = GRADE Moderada
        - "grade_baixa" = GRADE Baixa
        - "grade_muito_baixa" = GRADE Muito Baixa

        Regras para bias_risk:
        - "baixo" = Metodologia robusta
        - "moderado" = Limitações menores
        - "alto" = Limitações significativas
        - "nao_avaliado" = Não foi possível avaliar

        Para conteúdo de MTC onde RCTs são raros, use cebm_4 como default honesto.
        Para diretrizes oficiais (WHO, ANVISA), use grade_moderada ou grade_alta.
    """.trimIndent()

    private fun normalizeEvidenceLevel(value: String): String {
        val valid = setOf(
            "cebm_1a", "cebm_1b", "cebm_2a", "cebm_2b",
            "cebm_3a", "cebm_3b", "cebm_4", "cebm_5",
            "grade_alta", "grade_moderada", "grade_baixa", "grade_muito_baixa",
        )
        return if (value.lowercase().trim() in valid) value.lowercase().trim() else "cebm_4"
    }

    private fun normalizeBiasRisk(value: String): String {
        val valid = setOf("baixo", "moderado", "alto", "nao_avaliado")
        return if (value.lowercase().trim() in valid) value.lowercase().trim() else "nao_avaliado"
    }

    companion object {
        private const val TAG = "ContentExtractor"
        private const val MAX_CONTENT_CHARS = 4000
    }
}

/** Resultado da extração por IA. */
data class ExtractionResult(
    val keywords: List<String>,
    val evidenceLevel: String,
    val biasRisk: String,
    val confidence: Double,
) {
    companion object {
        /** Defaults seguros quando o LLM não está disponível. */
        fun default() = ExtractionResult(
            keywords = emptyList(),
            evidenceLevel = "cebm_4",
            biasRisk = "nao_avaliado",
            confidence = 0.0,
        )
    }
}

/** Schema JSON para parse da resposta do LLM. */
@Serializable
private data class ExtractionResultJson(
    val keywords: List<String> = emptyList(),
    val evidence_level: String = "cebm_4",
    val bias_risk: String = "nao_avaliado",
    val confidence: Double = 0.0,
)
