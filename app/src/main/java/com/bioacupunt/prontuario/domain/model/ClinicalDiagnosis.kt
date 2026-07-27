package com.bioacupunt.prontuario.domain.model

/**
 * SÍNTESE CLÍNICA DA IA — sugestão diagnóstica + plano terapêutico gerados a
 * partir de TODOS os dados do prontuário.
 *
 * A médica REVISA, EDITA, ACEITA ou DESCARTA cada componente. NUNCA é salva
 * automaticamente no prontuário.
 *
 * ## Arquitetura (establishes no novo caminho de IA)
 *
 * Este modelo não toca R1 (ClinicalSafetyEngine continua Kotlin puro), não toca
 * R2 (AskLibraryUseCase continua com gate `if (!grounding.hasEvidence)`), e não
 * toca R4 (não gera conteúdo para o acervo da biblioteca). É um **terceiro
 * caminho** — sugestão clínica contextual, não resposta a pergunta, não definição
 * de protocolo.
 */
data class ClinicalSynthesis(
    val tcmDiagnosis: TcmDiagnosisSuggestion? = null,
    val biomedicalDiagnosis: BiomedicalDiagnosisSuggestion? = null,
    val differentialDiagnoses: List<DifferentialSuggestion> = emptyList(),
    val therapeuticSuggestion: TherapeuticSuggestion? = null,
    val overallConfidence: ConfidenceLevel = ConfidenceLevel.INSUFFICIENT_EVIDENCE,
    val evidenceSources: List<EvidenceSource> = emptyList(),
    val generatedAt: String = "",
) {
    val isEmpty: Boolean
        get() = tcmDiagnosis == null &&
            biomedicalDiagnosis == null &&
            differentialDiagnoses.isEmpty() &&
            therapeuticSuggestion == null

    companion object {
        val EMPTY = ClinicalSynthesis()
    }
}

/** Diagnóstico em Medicina Tradicional Chinesa: padrão Zang-Fu + Ba Gang. */
data class TcmDiagnosisSuggestion(
    /** Ex.: "Deficiência de Qi do Baço e do Pulmão com Umidade" */
    val patternName: String = "",
    /** Órgãos envolvidos: ["Baço", "Pulmão"] */
    val organInvolvement: List<String> = emptyList(),
    /** Classificação Ba Gang: ex. "Interior, Frio, Deficiência" */
    val baGangClassification: String = "",
    /** Raciocínio clínico detalhado — por que este padrão foi sugerido */
    val explanation: String = "",
    val confidence: ConfidenceLevel = ConfidenceLevel.MODERATE,
)

/** Diagnóstico biomédico / CID sugerido. */
data class BiomedicalDiagnosisSuggestion(
    val diagnosis: String = "",
    val cidCode: String = "",
    val cid11Code: String = "",
    val explanation: String = "",
    val confidence: ConfidenceLevel = ConfidenceLevel.MODERATE,
)

/** Diagnóstico diferencial. */
data class DifferentialSuggestion(
    val description: String,
    val rationale: String = "",
    val priority: Int = 0,
)

/** Sugestão de plano terapêutico — objetivos, técnicas, pontos, cuidados. */
data class TherapeuticSuggestion(
    val objectives: String = "",
    val recommendedTechniques: List<String> = emptyList(),
    val acupuncturePoints: List<String> = emptyList(),
    val pointCombinations: List<String> = emptyList(),
    val cautionAndContraindications: List<String> = emptyList(),
    val sessionCount: Int? = null,
    val frequency: String = "",
)

/** Fonte de evidência usada pela IA para fundamentar a sugestão. */
data class EvidenceSource(
    val type: SourceType,
    val title: String,
    val snippet: String = "",
    val url: String = "",
    val relevance: String = "",
)

enum class ConfidenceLevel(val label: String) {
    HIGH("Alto — múltiplos achados concordantes"),
    MODERATE("Moderado — padrão compatível, mais dados ajudariam"),
    LOW("Baixo — quadro atípico ou poucos dados"),
    INSUFFICIENT_EVIDENCE("Evidência insuficiente para sugerir"),
}

enum class SourceType(val label: String) {
    LIBRARY("Biblioteca revisada"),
    WEB("Busca online"),
    CLINICAL_DATA("Dados do prontuário"),
}
