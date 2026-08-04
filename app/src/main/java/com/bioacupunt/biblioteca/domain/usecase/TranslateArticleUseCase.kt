package com.bioacupunt.biblioteca.domain.usecase

import com.bioacupunt.ai.core.AiRepository
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.biblioteca.domain.model.MtcArticle
import com.bioacupunt.biblioteca.domain.model.TranslationLanguage
import org.json.JSONObject

/**
 * Traduz UM artigo já aprovado para [TranslationLanguage] — transforma texto que a médica já
 * aprovou, nunca inventa fato clínico novo (mesmo espírito de R4 de
 * [com.bioacupunt.educacao.domain.usecase.GenerateStudyMaterialUseCase]: a única fonte
 * permitida é o próprio artigo).
 *
 * Seção por seção: o contexto do modelo local (Phi-4 Mini, 4096 tokens) não cabe um artigo
 * inteiro de uma vez com folga para a resposta. [chunkContent] preserva o NÍVEL do heading
 * (#, ##, ###) — de propósito NÃO reaproveita
 * [com.bioacupunt.core.util.MarkdownSections], que descarta essa informação (ótimo para
 * indexar por seção no RAG, ruim para reconstruir a formatação original aqui).
 *
 * Falha em qualquer seção aborta a tradução inteira — nunca publica um artigo
 * meio-traduzido sem avisar. Quem decide o que fazer com o erro (persistir status ERROR,
 * oferecer retry) é [com.bioacupunt.biblioteca.data.worker.ArticleTranslationWorker]; este
 * use case é puro, sem I/O além da chamada ao modelo.
 */
class TranslateArticleUseCase(
    private val ai: AiRepository,
) {
    sealed class Outcome {
        data class Success(
            val title: String,
            val summary: String,
            val tags: List<String>,
            val content: String,
        ) : Outcome()

        data class Error(val message: String) : Outcome()
    }

    suspend operator fun invoke(article: MtcArticle, targetLanguage: TranslationLanguage): Outcome {
        val metadata = translateMetadata(article, targetLanguage)
            ?: return Outcome.Error("Não foi possível traduzir título/resumo — modelo local indisponível ou resposta inválida.")

        val chunks = chunkContent(article.content)
        val translatedChunks = mutableListOf<String>()
        for ((index, chunk) in chunks.withIndex()) {
            val translated = translateChunk(chunk, targetLanguage)
                ?: return Outcome.Error("Falha ao traduzir seção ${index + 1} de ${chunks.size}.")
            translatedChunks += translated
        }

        return Outcome.Success(
            title = metadata.title,
            summary = metadata.summary,
            tags = metadata.tags,
            content = translatedChunks.joinToString("\n\n"),
        )
    }

    private data class Metadata(val title: String, val summary: String, val tags: List<String>)

    private suspend fun translateMetadata(article: MtcArticle, targetLanguage: TranslationLanguage): Metadata? {
        val prompt = buildString {
            appendLine("TITULO: ${article.title}")
            appendLine("RESUMO: ${article.summary}")
            if (article.tags.isNotEmpty()) appendLine("TAGS: ${article.tags.joinToString(", ")}")
        }
        val request = AiRequest(
            prompt = prompt,
            systemPrompt = metadataSystemPrompt(targetLanguage),
            temperature = 0.2,
            maxTokens = 512,
            preferLocal = true,
            taskHint = "article-translation-metadata",
        )
        return ai.generate(request).fold(
            onSuccess = { result -> runCatching { parseMetadata(result.text, article) }.getOrNull() },
            onFailure = { null },
        )
    }

    private fun parseMetadata(raw: String, article: MtcArticle): Metadata {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```JSON").removePrefix("```")
            .removeSuffix("```")
            .trim()
        val json = JSONObject(cleaned)
        val title = json.optString("title", article.title).trim().ifBlank { article.title }
        val summary = json.optString("summary", article.summary).trim()
        val tagsArr = json.optJSONArray("tags")
        val tags = if (tagsArr == null) {
            article.tags
        } else {
            (0 until tagsArr.length()).mapNotNull { i -> tagsArr.optString(i).takeIf { it.isNotBlank() } }
        }
        return Metadata(title = title, summary = summary, tags = tags)
    }

    private suspend fun translateChunk(chunk: String, targetLanguage: TranslationLanguage): String? {
        val request = AiRequest(
            prompt = chunk,
            systemPrompt = contentSystemPrompt(targetLanguage),
            temperature = 0.2,
            maxTokens = 1024,
            preferLocal = true,
            taskHint = "article-translation-content",
        )
        return ai.generate(request).fold(
            onSuccess = { result -> cleanTranslation(result.text).takeIf { it.isNotBlank() } },
            onFailure = { null },
        )
    }

    private fun cleanTranslation(text: String): String =
        text.trim()
            .removePrefix("```markdown").removePrefix("```md").removePrefix("```")
            .removeSuffix("```")
            .trim()

    /** Divide antes de cada heading, preservando o nível (#/##/###) dentro do chunk. */
    internal fun chunkContent(markdown: String): List<String> {
        val bySection = Regex("(?m)(?=^#{1,3} )").split(markdown).map { it.trim() }.filter { it.isNotEmpty() }
        val base = bySection.ifEmpty { listOf(markdown.trim()).filter { it.isNotEmpty() } }
        return base.flatMap(::splitIfTooLarge)
    }

    private fun splitIfTooLarge(chunk: String): List<String> {
        if (chunk.length <= MAX_CHUNK_CHARS) return listOf(chunk)
        val paragraphs = chunk.split("\n\n")
        val result = mutableListOf<StringBuilder>()
        for (p in paragraphs) {
            val last = result.lastOrNull()
            if (last == null || last.length + p.length > MAX_CHUNK_CHARS) {
                result += StringBuilder(p)
            } else {
                last.append("\n\n").append(p)
            }
        }
        return result.map { it.toString() }
    }

    private fun metadataSystemPrompt(targetLanguage: TranslationLanguage) = """
        Você é um sistema de TRADUÇÃO. Traduza o título e o resumo abaixo para ${targetLanguage.promptName}.
        Se já estiverem em ${targetLanguage.promptName}, devolva-os sem alteração.
        Mantenha nomes de pontos de acupuntura, termos em pinyin, siglas e nomes próprios exatamente como estão.
        Responda APENAS em JSON, neste formato exato, sem texto antes ou depois:
        {"title": "...", "summary": "...", "tags": ["...", "..."]}
    """.trimIndent()

    private fun contentSystemPrompt(targetLanguage: TranslationLanguage) = """
        Você é um sistema de TRADUÇÃO. Sua única tarefa é traduzir o texto fornecido para ${targetLanguage.promptName}.

        REGRAS ABSOLUTAS:
        1. Traduza apenas o texto — nunca resuma, explique, comente ou adicione informação que não está no original.
        2. Preserve EXATAMENTE a formatação Markdown: cabeçalhos (#, ##, ###), listas, tabelas (|), negrito/itálico, links e numeração de referências/citações.
        3. Preserve nomes de pontos de acupuntura (ex.: LI4, ST36), termos em pinyin, siglas e nomes de medicamentos exatamente como estão — não traduza nem adapte.
        4. Se um trecho já estiver em ${targetLanguage.promptName}, devolva-o sem alteração.
        5. Responda APENAS com o texto traduzido — nenhum texto antes ou depois, nenhum comentário.
    """.trimIndent()

    companion object {
        private const val MAX_CHUNK_CHARS = 1500
    }
}
