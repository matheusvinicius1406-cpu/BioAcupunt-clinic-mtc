package com.bioacupunt.prontuario.domain.usecase

import com.bioacupunt.ai.core.AiRepository
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.biblioteca.domain.search.MtcRetriever
import com.bioacupunt.prontuario.domain.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * SÍNTESE CLÍNICA — a IA analisa TODO o prontuário e sugere diagnóstico + plano.
 *
 * Este use case representa o **terceiro caminho** da IA no app:
 * - Diferente do [AskLibraryUseCase] (R2: responde perguntas com RAG, gate `if (!hasEvidence)`)
 * - Diferente do [StructureChiefComplaintUseCase] (extrativo: só reorganiza texto já escrito)
 * - Este é **generativo contextual**: recebe dados estruturados do prontuário, busca evidência na
 *   biblioteca E online (quando a nuvem está habilitada), e sintetiza uma hipótese diagnóstica
 *   completa que a médica revisa, edita, aceita ou descarta.
 *
 * ## Guardrails clínicos
 * - R1 intacto: `ClinicalSafetyEngine` nunca é importado aqui — a sugestão de plano não passa
 *   pelo motor de segurança. A médica leva a sugestão para a aba Plano, onde o motor roda.
 * - R2 intacto: `AskLibraryUseCase` continua com seu gate inalterado.
 * - R4 intacto: não gera conteúdo para a biblioteca.
 * - A sugestão NUNCA é salva automaticamente no prontuário.
 * - Falha (provider indisponível, JSON malformado) degrada para [ClinicalSynthesis.EMPTY].
 */
class ClinicalSynthesisUseCase(
    private val ai: AiRepository,
    private val mtcRetriever: MtcRetriever,
) {
    /**
     * Gera uma síntese clínica a partir de todos os dados disponíveis.
     *
     * @param assessment Avaliação MTC atual (BaGang, ZangFu, Língua, Pulso, flags, etc.)
     * @param history Avaliações anteriores do mesmo paciente (para linha do tempo)
     * @param clinicalSummary Resumo textual adicional (ex.: evolução recente)
     * @param labSummary Resumo de exames laboratoriais (ex.: "Hb 12.5, PCR 3.2")
     * @param activeMedications Medicações em uso
     * @param allergySummary Alergias registradas do paciente
     */
    suspend operator fun invoke(
        assessment: MtcAssessment,
        history: List<MtcAssessment> = emptyList(),
        clinicalSummary: String = "",
        labSummary: String = "",
        activeMedications: String = "",
        allergySummary: String = "",
    ): ClinicalSynthesis {
        // 1. Monta o perfil clínico completo
        val clinicalProfile = buildClinicalProfile(assessment, history, clinicalSummary, labSummary, activeMedications, allergySummary)

        // 2. Busca evidência na biblioteca curada
        val searchQuery = buildLibraryQuery(assessment)
        val grounding = mtcRetriever.retrieve(searchQuery, maxPassages = 8)

        // 3. Constrói o prompt com ou sem evidência da biblioteca
        val evidenceSection = if (grounding.hasEvidence) {
            buildEvidenceSection(grounding)
        } else {
            "Nenhum artigo específico encontrado na biblioteca para este quadro clínico."
        }

        // 4. Chama a IA — se a biblioteca tiver evidência, prefere processamento local (RAG).
        //    Se não tiver, permite busca na web via cloud (Gemini) para encontrar info relevante.
        //    Nota: preferLocal=false permite que o orquestrador escolha o cloud provider que
        //    suporta webSearch — quando true, o provider local ganha +1000 de score.
        val request = AiRequest(
            prompt = buildPrompt(clinicalProfile, evidenceSection),
            systemPrompt = CLINICAL_SYNTHESIS_PROMPT,
            temperature = 0.3,
            maxTokens = 3072,
            preferLocal = grounding.hasEvidence,
            taskHint = "clinical-synthesis",
            allowWebSearch = !grounding.hasEvidence,
        )

        return ai.generate(request).fold(
            onSuccess = { result ->
                parseResult(result.text, grounding)
            },
            onFailure = {
                ClinicalSynthesis.EMPTY.copy(generatedAt = Instant.now().toString())
            },
        )
    }

    // ── Construção do perfil clínico ────────────────────────────────────

    private fun buildClinicalProfile(
        assessment: MtcAssessment,
        history: List<MtcAssessment>,
        clinicalSummary: String,
        labSummary: String,
        activeMedications: String,
        allergySummary: String,
    ): String = buildString {
        appendLine("=== PERFIL CLÍNICO COMPLETO ===")
        appendLine()

        // Motivo da consulta
        appendLine("--- MOTIVO DA CONSULTA ---")
        appendLine(assessment.chiefComplaint.ifBlank { "(não registrado)" })
        appendLine()

        // Ba Gang
        appendLine("--- BA GANG (OITO PRINCÍPIOS) ---")
        with(assessment.baGang) {
            appendLine("Polaridade: ${polarityLabel(polarity)}")
            appendLine("Profundidade: ${depthLabel(depth)}")
            appendLine("Temperatura: ${temperatureLabel(temperature)}")
            appendLine("Força: ${strengthLabel(strength)}")
        }
        appendLine()

        // Zang Fu
        if (assessment.patterns.isNotEmpty()) {
            appendLine("--- ZANG FU (PADRÕES ORGÂNICOS) ---")
            assessment.patterns.forEach { pattern ->
                appendLine("- ${pattern.organ.label}: ${pattern.factors.joinToString { it.label }}")
                if (pattern.notes.isNotBlank()) {
                    appendLine("  Notas: ${pattern.notes}")
                }
            }
            appendLine()
        }

        // Língua
        with(assessment.tongue) {
            appendLine("--- LÍNGUA ---")
            appendLine("Cor: ${bodyColor.label}")
            appendLine("Saburra: ${coatingColor.label} · ${coatingThickness.label}")
            appendLine("Umidade: ${moisture.label}")
            if (shapes.isNotEmpty()) {
                appendLine("Forma: ${shapes.joinToString { it.label }}")
            }
            if (notes.isNotBlank()) {
                appendLine("Notas: $notes")
            }
            appendLine()
        }

        // Pulso
        with(assessment.pulse) {
            appendLine("--- PULSO ---")
            rateBpm?.let { appendLine("Frequência: $it bpm") }
            if (readings.isNotEmpty()) {
                readings.forEach { reading ->
                    appendLine("- ${reading.wrist.label} ${reading.position.label} (${reading.depth.label}): " +
                        reading.qualities.joinToString { it.label })
                }
            }
            if (notes.isNotBlank()) {
                appendLine("Notas: $notes")
            }
            appendLine()
        }

        // Fatores de agravamento/melhora
        if (assessment.aggravatingFactors.isNotEmpty()) {
            appendLine("--- FATORES DE PIORA ---")
            appendLine(assessment.aggravatingFactors.joinToString(", "))
            appendLine()
        }
        if (assessment.relievingFactors.isNotEmpty()) {
            appendLine("--- FATORES DE MELHORA ---")
            appendLine(assessment.relievingFactors.joinToString(", "))
            appendLine()
        }

        // Revisão de sistemas
        if (assessment.reviewOfSystems.isNotEmpty()) {
            appendLine("--- REVISÃO DE SISTEMAS ---")
            appendLine(assessment.reviewOfSystems.joinToString(", "))
            appendLine()
        }

        // Marcas corporais (mapa da dor)
        if (assessment.bodyMarks.isNotEmpty()) {
            appendLine("--- MAPA CORPORAL ---")
            assessment.bodyMarks.forEach { mark ->
                appendLine("- ${mark.region?.label ?: mark.id}: intensidade ${mark.intensity}/10, " +
                    "${mark.sensation.label}")
            }
            appendLine()
        }

        // ContrainDicações clínicas
        if (assessment.flags.isNotEmpty()) {
            appendLine("--- ALERTAS CLÍNICOS ---")
            assessment.flags.forEach { flag ->
                appendLine("- ${flag.label}")
            }
            appendLine()
        }

        // Impressão clínica
        if (assessment.clinicalImpression.isNotBlank()) {
            appendLine("--- IMPRESSÃO CLÍNICA (já registrada pela médica) ---")
            appendLine(assessment.clinicalImpression)
            appendLine()
        }

        // Orientações
        if (assessment.orientations.isNotBlank()) {
            appendLine("--- ORIENTAÇÕES REGISTRADAS ---")
            appendLine(assessment.orientations)
            appendLine()
        }

        // Dados adicionais
        if (clinicalSummary.isNotBlank()) {
            appendLine("--- RESUMO CLÍNICO ADICIONAL ---")
            appendLine(clinicalSummary)
            appendLine()
        }
        if (labSummary.isNotBlank()) {
            appendLine("--- EXAMES LABORATORIAIS ---")
            appendLine(labSummary)
            appendLine()
        }
        if (activeMedications.isNotBlank()) {
            appendLine("--- MEDICAÇÕES EM USO ---")
            appendLine(activeMedications)
            appendLine()
        }
        if (allergySummary.isNotBlank()) {
            appendLine("--- ALERGIAS REGISTRADAS ---")
            appendLine(allergySummary)
            appendLine()
        }

        // Histórico de consultas anteriores
        if (history.isNotEmpty()) {
            appendLine("--- CONSULTAS ANTERIORES (últimas ${history.size}) ---")
            history.takeLast(5).forEach { prev ->
                appendLine("Data: ${prev.date.take(10)}")
                if (prev.clinicalImpression.isNotBlank()) {
                    appendLine("  Impressão: ${prev.clinicalImpression}")
                }
                if (prev.patterns.isNotEmpty()) {
                    appendLine("  Padrões: ${prev.patterns.joinToString { it.organ.label }}")
                }
            }
            appendLine()
        }
    }

    // ── Busca na biblioteca ─────────────────────────────────────────────

    private fun buildLibraryQuery(assessment: MtcAssessment): String = buildString {
        // Combina sintomas principais + padrões + achados de língua/pulso
        if (assessment.chiefComplaint.isNotBlank()) {
            append(assessment.chiefComplaint.take(300))
            append(". ")
        }
        if (assessment.patterns.isNotEmpty()) {
            append("Padrões: ${assessment.patterns.joinToString { "${it.organ.label} ${it.factors.joinToString { f -> f.label }}" }}. ")
        }
        with(assessment.tongue) {
            if (bodyColor != TongueBodyColor.UNSET) {
                append("Língua: ${bodyColor.label}, saburra ${coatingColor.label} ${coatingThickness.label}. ")
            }
        }
        with(assessment.pulse) {
            if (allQualities.isNotEmpty()) {
                append("Pulso: ${allQualities.joinToString { it.label }}. ")
            }
        }
        // Adiciona Ba Gang
        with(assessment.baGang) {
            val parts = mutableListOf<String>()
            if (polarity != BaGangPolarity.UNSET) parts.add(polarityLabel(polarity))
            if (depth != BaGangDepth.UNSET) parts.add(depthLabel(depth))
            if (temperature != BaGangTemperature.UNSET) parts.add(temperatureLabel(temperature))
            if (strength != BaGangStrength.UNSET) parts.add(strengthLabel(strength))
            if (parts.isNotEmpty()) {
                append("Ba Gang: ${parts.joinToString(", ")}.")
            }
        }
        append(" Diagnóstico MTC e plano terapêutico.")
    }

    // ── Evidência da biblioteca ─────────────────────────────────────────

    private fun buildEvidenceSection(grounding: MtcRetriever.Grounding): String = buildString {
        appendLine("=== EVIDÊNCIA DA BIBLIOTECA REVISADA ===")
        appendLine()
        grounding.passages.forEach { passage ->
            appendLine("[${passage.ordinal}] ${passage.articleTitle}")
            appendLine(passage.text)
            appendLine()
        }
    }

    // ── Prompt para a IA ────────────────────────────────────────────────

    private fun buildPrompt(clinicalProfile: String, evidenceSection: String): String = buildString {
        appendLine(clinicalProfile)
        appendLine()
        appendLine(evidenceSection)
        appendLine()
        appendLine("Com base APENAS nos dados clínicos acima e na evidência da biblioteca (quando disponível),")
        appendLine("gere uma síntese diagnóstica completa no formato JSON especificado.")
    }

    // ── Parsing da resposta ─────────────────────────────────────────────

    private fun parseResult(raw: String, grounding: MtcRetriever.Grounding): ClinicalSynthesis {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```JSON").removePrefix("```")
            .removeSuffix("```")
            .trim()

        return runCatching {
            val json = JSONObject(cleaned)

            fun confidenceFromString(value: String?): ConfidenceLevel = when {
                value == null -> ConfidenceLevel.MODERATE
                value.contains("alto", ignoreCase = true) ||
                    value.contains("high", ignoreCase = true) -> ConfidenceLevel.HIGH
                value.contains("baixo", ignoreCase = true) ||
                    value.contains("low", ignoreCase = true) -> ConfidenceLevel.LOW
                value.contains("insuficiente", ignoreCase = true) ||
                    value.contains("insufficient", ignoreCase = true) -> ConfidenceLevel.INSUFFICIENT_EVIDENCE
                else -> ConfidenceLevel.MODERATE
            }

            fun stringList(key: String): List<String> {
                val arr = json.optJSONArray(key) ?: return emptyList()
                return (0 until arr.length()).mapNotNull { i -> arr.optString(i).takeIf { it.isNotBlank() } }
            }

            // TCM Diagnosis
            val tcmObj = json.optJSONObject("tcmDiagnosis")
            val tcmDiag = tcmObj?.let { obj ->
                TcmDiagnosisSuggestion(
                    patternName = obj.optString("patternName", ""),
                    organInvolvement = obj.optJSONArray("organInvolvement")
                        ?.let { arr -> (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { v -> v.isNotBlank() } } }
                        ?: emptyList(),
                    baGangClassification = obj.optString("baGangClassification", ""),
                    explanation = obj.optString("explanation", ""),
                    confidence = confidenceFromString(obj.optString("confidence", "")),
                )
            }

            // Biomedical Diagnosis
            val bioObj = json.optJSONObject("biomedicalDiagnosis")
            val bioDiag = bioObj?.let { obj ->
                BiomedicalDiagnosisSuggestion(
                    diagnosis = obj.optString("diagnosis", ""),
                    cidCode = obj.optString("cidCode", ""),
                    cid11Code = obj.optString("cid11Code", ""),
                    explanation = obj.optString("explanation", ""),
                    confidence = confidenceFromString(obj.optString("confidence", "")),
                )
            }

            // Differential diagnoses
            val diagArr = json.optJSONArray("differentialDiagnoses") ?: JSONArray()
            val differentials = (0 until diagArr.length()).mapNotNull { i ->
                val obj = diagArr.optJSONObject(i) ?: return@mapNotNull null
                DifferentialSuggestion(
                    description = obj.optString("description", ""),
                    rationale = obj.optString("rationale", ""),
                    priority = obj.optInt("priority", 0),
                )
            }

            // Therapeutic suggestion
            val therapyObj = json.optJSONObject("therapeuticSuggestion")
            val therapy = therapyObj?.let { obj ->
                TherapeuticSuggestion(
                    objectives = obj.optString("objectives", ""),
                    recommendedTechniques = stringListFromArray(obj, "recommendedTechniques"),
                    acupuncturePoints = stringListFromArray(obj, "acupuncturePoints"),
                    pointCombinations = stringListFromArray(obj, "pointCombinations"),
                    cautionAndContraindications = stringListFromArray(obj, "cautionAndContraindications"),
                    sessionCount = if (obj.has("sessionCount") && !obj.isNull("sessionCount"))
                        obj.getInt("sessionCount") else null,
                    frequency = obj.optString("frequency", ""),
                )
            }

            // Overall confidence
            val overall = confidenceFromString(json.optString("overallConfidence", ""))

            // Evidence sources from grounding
            val sources = grounding.passages.map { passage ->
                EvidenceSource(
                    type = SourceType.LIBRARY,
                    title = passage.articleTitle,
                    snippet = passage.text.take(200),
                    relevance = "Trecho ${passage.ordinal} — ${passage.articleTitle}",
                )
            }

            ClinicalSynthesis(
                tcmDiagnosis = tcmDiag,
                biomedicalDiagnosis = bioDiag,
                differentialDiagnoses = differentials,
                therapeuticSuggestion = therapy,
                overallConfidence = overall,
                evidenceSources = sources,
                generatedAt = Instant.now().toString(),
            )
        }.getOrDefault(ClinicalSynthesis.EMPTY.copy(generatedAt = Instant.now().toString()))
    }

    private fun stringListFromArray(obj: JSONObject, key: String): List<String> {
        val arr = obj.optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i -> arr.optString(i).takeIf { it.isNotBlank() } }
    }

    // ── Helpers de label ────────────────────────────────────────────────

    private fun polarityLabel(p: BaGangPolarity) = when (p) {
        BaGangPolarity.YIN -> "Yin"
        BaGangPolarity.YANG -> "Yang"
        BaGangPolarity.UNSET -> "Não registrado"
    }
    private fun depthLabel(d: BaGangDepth) = when (d) {
        BaGangDepth.EXTERIOR -> "Exterior"
        BaGangDepth.INTERIOR -> "Interior"
        BaGangDepth.UNSET -> "Não registrado"
    }
    private fun temperatureLabel(t: BaGangTemperature) = when (t) {
        BaGangTemperature.COLD -> "Frio"
        BaGangTemperature.HEAT -> "Calor"
        BaGangTemperature.UNSET -> "Não registrado"
    }
    private fun strengthLabel(s: BaGangStrength) = when (s) {
        BaGangStrength.DEFICIENCY -> "Deficiência"
        BaGangStrength.EXCESS -> "Excesso"
        BaGangStrength.UNSET -> "Não registrado"
    }

    companion object {
        private val CLINICAL_SYNTHESIS_PROMPT = """
            Você é uma assistente de apoio ao raciocínio clínico em Medicina Tradicional
            Chinesa (MTC) e Biomedicina, integrada ao sistema BioAcupunt.

            SUA FUNÇÃO:
            Analisar o perfil clínico completo de um paciente e gerar uma SUGESTÃO de
            diagnóstico e plano terapêutico. Você é uma ferramenta de apoio — a decisão
            final é sempre da médica responsável.

            REGRAS ABSOLUTAS:
            1. BASEIE-SE nos dados fornecidos — sintomas, língua, pulso, Ba Gang, exames,
               histórico, medicações e alergias. NÃO invente informações que não estão
               nos dados. Cada achado citado no "explanation" precisa apontar para um
               dado específico do perfil (ex.: "saburra amarela + pulso rápido" em vez de
               generalidades que serviriam para qualquer paciente).
            2. Quando evidência da biblioteca estiver disponível, CITE as fontes.
            3. Se os dados forem insuficientes para um diagnóstico confiante, diga isso
               explicitamente com confidence baixo — não complete a lacuna com achismo.
            4. NUNCA afirme um diagnóstico como definitivo — use linguagem de sugestão.
            5. NUNCA prescreva automaticamente — a sugestão terapêutica é uma proposta
               que a médica avalia. Se houver medicação em uso ou alergia registrada,
               mencione explicitamente em "cautionAndContraindications" quando relevante
               ao plano sugerido (ex.: interação, sensibilização).
            6. Inclua raciocínio clínico para cada componente sugerido, explicando POR
               QUE determinado padrão ou técnica foi selecionado — não apenas O QUE foi
               selecionado.
            7. Gere pelo menos um diagnóstico diferencial sempre que o quadro permitir
               mais de uma leitura plausível — com "rationale" ligado a achado concreto,
               não uma lista genérica de possibilidades.

            RESPONDA EXCLUSIVAMENTE no formato JSON abaixo, sem texto antes ou depois:

            {
              "tcmDiagnosis": {
                "patternName": "Nome do padrão de desarmonia MTC",
                "organInvolvement": ["Órgão1", "Órgão2"],
                "baGangClassification": "Exemplo: Interior, Frio, Deficiência",
                "explanation": "Raciocínio clínico detalhado ligando os achados ao padrão",
                "confidence": "HIGH | MODERATE | LOW | INSUFFICIENT_EVIDENCE"
              },
              "biomedicalDiagnosis": {
                "diagnosis": "Nome do diagnóstico biomédico",
                "cidCode": "Código CID-10",
                "cid11Code": "Código CID-11 (se disponível)",
                "explanation": "Raciocínio clínico para o diagnóstico biomédico",
                "confidence": "HIGH | MODERATE | LOW | INSUFFICIENT_EVIDENCE"
              },
              "differentialDiagnoses": [
                {
                  "description": "Descrição do diagnóstico diferencial",
                  "rationale": "Por que considerar este diferencial",
                  "priority": 1
                }
              ],
              "therapeuticSuggestion": {
                "objectives": "Objetivos terapêuticos principais",
                "recommendedTechniques": ["Acupuntura", "Moxabustão"],
                "acupuncturePoints": ["LI4", "SP6", "ST36"],
                "pointCombinations": ["LI4 + SP6 para mover Qi e Xue"],
                "cautionAndContraindications": ["Evitar eletroacupuntura se gestante"],
                "sessionCount": 10,
                "frequency": "1x por semana"
              },
              "overallConfidence": "HIGH | MODERATE | LOW | INSUFFICIENT_EVIDENCE"
            }

            Se algum campo não tiver dados suficientes para sugerir, deixe o valor como
            string vazia ou array vazio — nunca invente.
        """.trimIndent()
    }
}
