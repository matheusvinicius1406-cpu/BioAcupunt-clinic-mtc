package com.bioacupunt.biblioteca.domain.search

import com.bioacupunt.biblioteca.domain.model.MtcArticle

/**
 * RETRIEVAL-AUGMENTED GENERATION OVER THE MTC LIBRARY
 *
 * The single most dangerous thing an LLM can do in a clinical app is answer a
 * question about acupuncture *from its own weights*. It will produce a fluent,
 * confident, well-formatted paragraph containing a point that does not exist, a
 * meridian trajectory that is wrong, or a citation to a study that was never written.
 * A practitioner cannot distinguish that from a correct answer — that is precisely
 * what makes it dangerous.
 *
 * So this retriever enforces a hard contract:
 *
 *  - The model only ever sees passages pulled from the curated library.
 *  - The prompt forbids using any knowledge outside those passages.
 *  - **If retrieval finds nothing, no model call is made at all.** The app says "não
 *    encontrei nada na biblioteca" rather than letting the model improvise. An empty
 *    context is exactly the condition under which a model hallucinates most, so the
 *    correct engineering response is to not ask it.
 *  - Every passage is numbered so the answer can cite, and the doctor can verify.
 *
 * Grounding is a *retrieval* property, not a prompt property. The instruction text is
 * the weakest link here and is treated as such.
 */
class MtcRetriever(
    private val articles: List<MtcArticle>,
    indexProvider: () -> MtcSearchEngine.Index = { MtcSearchEngine.index(articles) },
) {

    // Construído sob demanda, no primeiro search — que roda sempre dentro de uma
    // coroutine (viewModelScope). Antes era um default param avaliado no construtor,
    // e o construtor era tocado durante a composição ao abrir a Biblioteca/Assistente,
    // travando a 1ª navegação enquanto o índice BM25 de toda a base era montado na
    // thread de UI. Lazy move esse custo para fora dela sem mudar o comportamento.
    private val index: MtcSearchEngine.Index by lazy(indexProvider)

    data class Passage(
        val ordinal: Int,
        val articleId: String,
        val articleTitle: String,
        val text: String,
    )

    data class Grounding(
        val question: String,
        val passages: List<Passage>,
    ) {
        /** No passages means: do not call the model. */
        val hasEvidence: Boolean get() = passages.isNotEmpty()
    }

    /**
     * Splits article bodies on markdown headings, so a passage is a coherent section
     * rather than an arbitrary character window that cuts a sentence in half.
     */
    internal fun chunk(article: MtcArticle): List<String> {
        val body = article.content.trim()
        if (body.isEmpty()) return emptyList()

        val sections = body
            .split(Regex("(?m)^#{1,3} "))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        return sections
            .flatMap { section ->
                if (section.length <= MAX_CHARS) {
                    listOf(section)
                } else {
                    section.chunked(MAX_CHARS)
                }
            }
            .filter { it.length >= MIN_CHARS }
    }

    fun retrieve(question: String, maxPassages: Int = 4): Grounding {
        val hits = index.search(question, limit = maxPassages)
        if (hits.isEmpty()) return Grounding(question, emptyList())

        val terms = MtcSearchEngine.expand(MtcSearchEngine.tokenize(question))

        val passages = hits.flatMapIndexed { _, hit ->
            val best = chunk(hit.article)
                // Within the winning article, pick the section that actually mentions
                // the query terms — not just the first paragraph.
                .maxByOrNull { section ->
                    val tokens = MtcSearchEngine.tokenize(section)
                    terms.count { term -> term in tokens }
                }
            if (best.isNullOrBlank()) {
                emptyList()
            } else {
                listOf(hit.article to best)
            }
        }.mapIndexed { i, (article, text) ->
            Passage(
                ordinal = i + 1,
                articleId = article.id,
                articleTitle = article.title,
                text = text,
            )
        }

        return Grounding(question, passages)
    }

    companion object {
        private const val MAX_CHARS = 1200
        private const val MIN_CHARS = 40

        /**
         * The system prompt. Note what it does *not* do: it does not try to make the
         * model safe by asking nicely. The safety comes from [Grounding.hasEvidence]
         * gating the call at all. This text just shapes the answer.
         */
        val SYSTEM_PROMPT = """
            Você responde perguntas sobre Medicina Tradicional Chinesa usando
            EXCLUSIVAMENTE os trechos da biblioteca fornecidos abaixo.

            Regras:
            - Use SOMENTE o que está nos trechos. Não use conhecimento próprio.
            - Cite a fonte de cada afirmação com o número do trecho: [1], [2]...
            - Se os trechos não responderem à pergunta, diga exatamente:
              "Os trechos da biblioteca não respondem a esta pergunta."
              Não tente completar com suposições.
            - Nunca invente pontos, meridianos, fórmulas, autores ou estudos.
            - Este conteúdo é apoio ao estudo e ao raciocínio. Não é prescrição,
              e não substitui o julgamento da profissional.
        """.trimIndent()

        /** Renders the retrieved evidence into the user turn. */
        fun buildPrompt(grounding: Grounding): String = buildString {
            appendLine("Trechos da biblioteca:")
            appendLine()
            grounding.passages.forEach { p ->
                appendLine("[${p.ordinal}] ${p.articleTitle}")
                appendLine(p.text)
                appendLine()
            }
            appendLine("Pergunta: ${grounding.question}")
        }
    }
}
