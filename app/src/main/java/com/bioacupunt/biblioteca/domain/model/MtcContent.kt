package com.bioacupunt.biblioteca.domain.model

import kotlinx.serialization.Serializable

enum class MtcCategory(val label: String, val emoji: String) {
    MERIDIANOS("Meridianos", "🫀"),
    PONTOS("Pontos de Acupuntura", "📍"),
    CINCO_ELEMENTOS("Cinco Elementos", "🌿"),
    BA_GANG("Ba Gang (8 Princípios)", "☯️"),
    SINDROME_ORGAOS("Síndromes de Órgãos", "🫁"),
    LINGUA("Semiologia da Língua", "👅"),
    PULSO("Semiologia do Pulso", "💓"),
    TECNICAS("Técnicas de Agulhamento", "🪡"),
    FITOTERAPIA("Fitoterapia Chinesa", "🌱"),
    MOXIBUSTAO("Moxibustão", "🔥"),
    DIETOTERAPIA("Dietoterapia MTC", "🥗"),
    QIGONG("Qigong e Tai Chi", "🧘"),
    CLINICA_MEDICA("Clínica Médica", "🏥")
}

@Serializable
data class MtcArticle(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val content: String,
    val tags: List<String> = emptyList(),
    /**
     * Proveniência do pipeline de curadoria (ver `domain/ingestion/IngestionModels.kt`).
     * Só artigos aprovados via curadoria carregam isto — os 16 artigos fixos do
     * [com.bioacupunt.biblioteca.data.MtcKnowledgeBase] deixam tudo abaixo em "",
     * nunca em erro. Mesmo espírito de "codec degrada, não quebra" do resto do app:
     * campo ausente vira vazio na UI, não N/A nem crash.
     */
    val citation: String = "",
    /** URL do documento-fonte, quando público. Vazio para fonte impressa ou artigo fixo. */
    val sourceUrl: String = "",
    /** Localizador dentro da fonte (ex.: "p. 26", "cap. 12"). Vazio se não houver. */
    val sourceRef: String = "",
    /**
     * Nome de um `com.bioacupunt.biblioteca.domain.ingestion.Provenance`
     * ("VERIFICAVEL"/"RASCUNHO"), guardado como String (mesmo padrão de [category])
     * para não acoplar este módulo de domínio ao pacote de ingestão. "" quando o
     * artigo não passou pelo pipeline de curadoria (acervo interno fixo).
     */
    val provenance: String = "",
)
