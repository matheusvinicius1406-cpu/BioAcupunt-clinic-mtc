package com.bioacupunt.biblioteca.domain.search

import com.bioacupunt.biblioteca.domain.ingestion.Provenance

/**
 * O BACKEND DE BUSCA, VISTO PELO DOMÍNIO.
 *
 * ## Por que esta interface existe
 * O portão da R2 — *"sem evidência ⇒ nenhuma chamada ao modelo"* — é uma regra de
 * domínio. Ela precisa morar em Kotlin puro e ser testável sem device, exatamente
 * como o [com.bioacupunt.prontuario.domain.safety.ClinicalSafetyEngine].
 *
 * Quando o ranking foi movido do BM25 em memória para o SQLite FTS4, a lógica de
 * montagem do [MtcRetriever.Grounding] foi junto para a camada `data` — e o teste
 * que guarda o portão (`unknownTopicYieldsNoEvidence_soTheModelIsNeverCalled`)
 * ficou sem como rodar, porque passou a depender de um DAO do Room.
 *
 * Esta interface desfaz isso. O **backend** responde apenas "quais artigos casam com
 * esta busca" — pode ser FTS4, BM25 ou o que vier depois. A **decisão** de haver ou
 * não evidência, e o recorte das passagens, voltam para o [MtcRetriever].
 *
 * Consequência prática: trocar o motor de busca não pode reabrir o portão por
 * acidente, porque o portão não está no motor.
 */
interface ArticleSearchBackend {

    /**
     * Artigos que casam com a busca, já ranqueados (melhor primeiro).
     *
     * Contrato: uma busca sem resultado retorna **lista vazia**. Nunca um
     * "melhor esforço" com match fraco — é disso que o [MtcRetriever] depende
     * para decidir não chamar o modelo.
     */
    suspend fun search(query: String, maxResults: Int): List<RetrievedArticle>
}

/** Um artigo devolvido pela busca, sem detalhes de persistência. */
data class RetrievedArticle(
    val articleId: String,
    val title: String,
    val summary: String,
    val content: String,
    /**
     * Proveniência do artigo: [Provenance.VERIFICAVEL] se tem localizador de
     * fonte (página/capítulo), [Provenance.RASCUNHO] se não tem.
     *
     * A busca pode filtrar ou ranquear por proveniência — mas NUNCA deve
     * esconder itens RASCUNHO da médica que os aprovou. A decisão de confiar
     * é dela; o app só mostra o selo.
     */
    val provenance: Provenance = Provenance.RASCUNHO,
)
