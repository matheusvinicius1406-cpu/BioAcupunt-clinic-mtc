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
    QIGONG("Qigong e Tai Chi", "🧘")
}

@Serializable
data class MtcArticle(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val content: String,
    val tags: List<String> = emptyList()
)
