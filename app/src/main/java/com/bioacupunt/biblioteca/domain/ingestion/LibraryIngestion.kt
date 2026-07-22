package com.bioacupunt.biblioteca.domain.ingestion

import com.bioacupunt.biblioteca.domain.model.MtcArticle
import com.bioacupunt.biblioteca.domain.model.MtcCategory

/**
 * O PORTÃO R4 — Kotlin puro, determinístico, testável sem device.
 *
 * Decide o que pode ser *encenado* para revisão e o que é rejeitado na entrada. É o
 * equivalente da biblioteca ao [com.bioacupunt.prontuario.domain.safety.ClinicalSafetyEngine]:
 * a garantia mora aqui, num `when`/`if`, não numa instrução de prompt que se deixa
 * convencer.
 *
 * Regras (todas fail-closed — na dúvida, rejeita):
 *  - Sem **citação** de fonte humana revisada ⇒ rejeitado. É a linha que separa
 *    "conteúdo revisado ingerido" de "texto qualquer despejado no acervo clínico".
 *  - Sem **fonte** no pacote ⇒ rejeitado (proveniência do lote).
 *  - Título ou conteúdo vazio ⇒ rejeitado.
 *  - Categoria fora de [MtcCategory] ⇒ rejeitado (não deixa lixo virar seção do acervo).
 *
 * Encenar NÃO é publicar. Todo item aprovado no portão entra como [ReviewStatus.PENDING];
 * só a médica move para [ReviewStatus.APPROVED]. Nada aqui coloca conteúdo no RAG.
 */
object LibraryIngestion {

    data class StagedNode(val article: MtcArticle, val meta: ReviewMeta)
    data class Rejection(val id: String, val reason: String)
    data class Outcome(val staged: List<StagedNode>, val rejected: List<Rejection>) {
        val stagedCount: Int get() = staged.size
        val rejectedCount: Int get() = rejected.size
    }

    private val validCategories: Set<String> = MtcCategory.entries.map { it.name }.toSet()

    fun stage(pack: LibraryContentPack, now: Long): Outcome {
        val staged = mutableListOf<StagedNode>()
        val rejected = mutableListOf<Rejection>()

        if (pack.source.isBlank()) {
            // Um lote sem proveniência é rejeitado inteiro — não dá para revisar o que
            // não se sabe de onde veio.
            return Outcome(emptyList(), pack.items.map { Rejection(it.id, "Pacote sem fonte revisada") })
        }

        for (item in pack.items) {
            val reason = when {
                item.id.isBlank() -> "Item sem id"
                item.title.isBlank() -> "Item sem título"
                item.content.isBlank() -> "Item sem conteúdo"
                item.citation.isBlank() -> "Item sem citação de fonte revisada (R4)"
                item.category !in validCategories -> "Categoria inválida: '${item.category}'"
                else -> null
            }
            if (reason != null) {
                rejected.add(Rejection(item.id, reason))
                continue
            }
            staged.add(
                StagedNode(
                    article = MtcArticle(
                        id = item.id,
                        title = item.title.trim(),
                        category = item.category,
                        summary = item.summary.trim(),
                        content = item.content.trim(),
                        tags = item.tags.map { it.trim() }.filter { it.isNotEmpty() },
                    ),
                    meta = ReviewMeta(
                        status = ReviewStatus.PENDING,
                        source = pack.source.trim(),
                        citation = item.citation.trim(),
                        stagedAt = now,
                        sourceUrl = item.sourceUrl.trim(),
                        sourceRef = item.sourceRef.trim(),
                        // Conferível só quando há ONDE olhar. Um nome de documento sem
                        // localizador não permite checagem — classificar como rascunho
                        // é fail-closed: na dúvida, a médica lê com desconfiança.
                        provenance = if (item.sourceRef.isNotBlank()) {
                            Provenance.VERIFICAVEL
                        } else {
                            Provenance.RASCUNHO
                        },
                    ),
                ),
            )
        }
        return Outcome(staged, rejected)
    }
}
