package com.bioacupunt.biblioteca.domain.search

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
 *    encontrei na biblioteca" rather than letting the model improvise. An empty
 *    context is exactly the condition under which a model hallucinates most, so the
 *    correct engineering response is to not ask it.
 *  - Every passage is numbered so the answer can cite, and the doctor can verify.
 *
 * Grounding is a *retrieval* property, not a prompt property. The instruction text is
 * the weakest link here and is treated as such.
 *
 * ## Search backend
 * O ranking fica num [ArticleSearchBackend] (hoje SQLite FTS4, antes BM25 em
 * memória). O backend só diz *quais artigos casam*; **o portão mora aqui**. Essa
 * separação é deliberada: trocar o motor de busca não pode reabrir o portão por
 * acidente, e o teste que guarda a R2 roda em Kotlin puro, sem device.
 */
class MtcRetriever(
    private val backend: ArticleSearchBackend,
) {

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
     * O PORTÃO DA R2.
     *
     * Se o backend não devolve nada, devolvemos um [Grounding] vazio — e
     * [Grounding.hasEvidence] fica `false`, o que faz [AskLibraryUseCase] parar
     * antes de qualquer chamada ao modelo. É um `if`, não uma instrução de prompt.
     */
    suspend fun retrieve(question: String, maxPassages: Int = 4): Grounding {
        val hits = backend.search(question, maxPassages)
        if (hits.isEmpty()) return Grounding(question, emptyList())

        val terms = MtcSearchEngine.expand(MtcSearchEngine.tokenize(question)).toSet()
        val passages = hits.mapIndexed { i, hit ->
            Passage(
                ordinal = i + 1,
                articleId = hit.articleId,
                articleTitle = hit.title,
                text = extractBestSection(hit, terms),
            )
        }
        return Grounding(question, passages)
    }

    /**
     * Recorta do artigo a seção que mais casa com os termos da pergunta.
     *
     * Sem isto, um artigo longo entrega ao modelo 1.200 caracteres que podem não
     * conter a resposta — contexto que parece evidência e não é.
     */
    private fun extractBestSection(hit: RetrievedArticle, terms: Set<String>): String {
        val sections = hit.content
            .split(SECTION_HEADING)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (sections.isEmpty()) return hit.summary.take(MAX_SECTION_CHARS)

        val best = sections.maxByOrNull { section ->
            val sectionTokens = MtcSearchEngine.tokenize(section).toSet()
            terms.count { it in sectionTokens }
        }
        return (best ?: hit.summary).take(MAX_SECTION_CHARS)
    }

    companion object {
        private const val MAX_SECTION_CHARS = 1200
        private val SECTION_HEADING = Regex("(?m)^#{1,3} ")

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
