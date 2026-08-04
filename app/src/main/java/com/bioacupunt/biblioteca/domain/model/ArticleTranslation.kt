package com.bioacupunt.biblioteca.domain.model

/**
 * Idiomas de destino suportados pelo tradutor automático da Biblioteca — conjunto fechado,
 * não texto livre. Um código BCP-47 inválido digitado por engano nunca deveria virar
 * prompt para o modelo local; um enum torna esse estado irrepresentável.
 */
enum class TranslationLanguage(val code: String, val label: String, val promptName: String) {
    PT_BR("pt-BR", "Português (Brasil)", "Brazilian Portuguese"),
    EN("en", "English", "English"),
    ES("es", "Español", "Spanish"),
    FR("fr", "Français", "French");

    companion object {
        val default: TranslationLanguage = PT_BR
        fun byCode(code: String): TranslationLanguage? = entries.find { it.code == code }
    }
}

enum class TranslationStatus { PENDING, PROCESSING, COMPLETED, ERROR }

/**
 * Tradução automática de UM artigo aprovado na Curadoria para [targetLanguage].
 *
 * Gerada por [com.bioacupunt.biblioteca.domain.usecase.TranslateArticleUseCase], executada
 * por [com.bioacupunt.biblioteca.data.worker.ArticleTranslationWorker] — nunca escrita à mão.
 *
 * Publicação é AUTOMÁTICA (decisão de produto, 2026-08-04: a médica pediu explicitamente
 * "traduzir e publicar direto", sem o gate de revisão humana que o resto do pipeline de
 * curadoria exige — R4). Por isso [status]/[content] nunca substituem o [MtcArticle]
 * original na busca/RAG (isso continua indexando só o texto aprovado por humano) — a
 * tradução é sempre um overlay de EXIBIÇÃO, e toda tela que a mostra identifica que é
 * automática e não revisada.
 */
data class ArticleTranslation(
    val articleId: String,
    val targetLanguage: TranslationLanguage,
    val status: TranslationStatus,
    val title: String = "",
    val summary: String = "",
    val content: String = "",
    val tags: List<String> = emptyList(),
    val errorMessage: String = "",
    val updatedAt: Long = 0L,
)
