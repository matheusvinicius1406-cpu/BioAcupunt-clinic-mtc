package com.bioacupunt.educacao.domain.usecase

import com.bioacupunt.ai.core.AiRepository
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.biblioteca.domain.model.MtcArticle
import com.bioacupunt.educacao.domain.model.GeneratedCaseDraft
import com.bioacupunt.educacao.domain.model.GeneratedFlashcardDraft
import com.bioacupunt.educacao.domain.model.StudyMaterialDraft
import org.json.JSONObject

/**
 * Gera um RASCUNHO de material de estudo (até 10 flashcards + 1 caso clínico simulado)
 * a partir de UM artigo já aprovado na Biblioteca — chamado pela mesma tela de Curadoria
 * logo após a médica aprovar o artigo-fonte (ver `LibraryReviewViewModel.approve`).
 *
 * ## Por que isto não viola R4
 *
 * R4 proíbe gerar CONTEÚDO CLÍNICO a partir dos pesos do modelo para preencher o acervo
 * da Biblioteca (RAG) sem revisão humana. Isto é uma coisa diferente, no mesmo espírito
 * de [com.bioacupunt.prontuario.domain.usecase.ClinicalSynthesisUseCase] (terceiro
 * caminho da IA) e de [com.bioacupunt.prontuario.domain.usecase.StructureChiefComplaintUseCase]
 * (extrativo):
 * - A entrada é o texto de UM artigo JÁ aprovado por humano — não busca na web, não usa
 *   conhecimento livre do modelo. O grounding é o próprio artigo, passado inteiro no prompt.
 * - A saída NUNCA entra no acervo/RAG (não é um "artigo" — R4 continua intocado) nem no
 *   deck de flashcards/lista de casos até a médica aceitar EXPLICITAMENTE cada item (ou o
 *   lote) na mesma tela — [StudyMaterialDraft] existe só em memória (estado de ViewModel)
 *   até isso acontecer.
 * - O conteúdo é educacional (estuda o assunto do artigo), nunca clínico-real: o caso
 *   simulado é fictício por natureza, nunca referencia paciente de verdade, e o prompt
 *   proíbe explicitamente introduzir fato não presente no artigo-fonte.
 *
 * Falha (provider indisponível, JSON malformado) degrada para [StudyMaterialDraft.empty] —
 * apoio, não caminho crítico; a Curadoria nunca trava nem mostra erro por causa disto.
 */
class GenerateStudyMaterialUseCase(
    private val ai: AiRepository,
) {
    suspend operator fun invoke(article: MtcArticle): StudyMaterialDraft {
        val body = article.content.take(MAX_CONTENT_CHARS)
        if (body.trim().length < MIN_CONTENT_LENGTH) return StudyMaterialDraft.empty(article.id, article.title)

        val prompt = buildString {
            appendLine("=== ARTIGO-FONTE (única fonte permitida) ===")
            appendLine("Título: ${article.title}")
            appendLine("Categoria: ${article.category}")
            appendLine()
            appendLine(body)
        }

        val request = AiRequest(
            prompt = prompt,
            systemPrompt = SYSTEM_PROMPT,
            temperature = 0.4,
            maxTokens = 2048,
            preferLocal = true,
            taskHint = "study-material-generation",
            allowWebSearch = false,
        )

        return ai.generate(request).fold(
            onSuccess = { result ->
                runCatching { parse(result.text, article) }.getOrDefault(StudyMaterialDraft.empty(article.id, article.title))
            },
            onFailure = { StudyMaterialDraft.empty(article.id, article.title) },
        )
    }

    private fun parse(raw: String, article: MtcArticle): StudyMaterialDraft {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```JSON").removePrefix("```")
            .removeSuffix("```")
            .trim()
        val json = JSONObject(cleaned)

        val flashcardsArr = json.optJSONArray("flashcards")
        val flashcards = if (flashcardsArr == null) emptyList() else (0 until flashcardsArr.length())
            .mapNotNull { i -> flashcardsArr.optJSONObject(i) }
            .mapNotNull { obj ->
                val front = obj.optString("front", "").trim()
                val back = obj.optString("back", "").trim()
                if (front.isBlank() || back.isBlank()) null
                else GeneratedFlashcardDraft(front = front, back = back, category = obj.optString("category", article.category).trim())
            }
            .take(10)

        val caseObj = json.optJSONObject("clinicalCase")
        val clinicalCase = caseObj?.let { obj ->
            val vignette = obj.optString("vignette", "").trim()
            if (vignette.isBlank()) return@let null
            val questionsArr = obj.optJSONArray("questions")
            val questions = if (questionsArr == null) emptyList() else (0 until questionsArr.length())
                .mapNotNull { i -> questionsArr.optString(i).takeIf { it.isNotBlank() } }
            GeneratedCaseDraft(
                title = obj.optString("title", article.title).trim(),
                vignette = vignette,
                questions = questions,
                answerKey = obj.optString("answerKey", "").trim(),
                category = obj.optString("category", article.category).trim(),
            )
        }

        return StudyMaterialDraft(
            articleId = article.id,
            articleTitle = article.title,
            flashcards = flashcards,
            clinicalCase = clinicalCase,
        )
    }

    companion object {
        private const val MIN_CONTENT_LENGTH = 100
        private const val MAX_CONTENT_CHARS = 6000

        private val SYSTEM_PROMPT = """
            Você é uma ferramenta de criação de material de ESTUDO, não uma assistente
            clínica. Sua única fonte de informação é o texto do artigo fornecido —
            nenhum outro conhecimento, nenhuma busca, nenhuma suposição.

            SUA TAREFA:
            A partir do artigo fornecido, gere até 10 flashcards de estudo e UM caso
            clínico SIMULADO (fictício, educacional — nunca um paciente real) que ilustre
            a aplicação do conteúdo do artigo.

            IDIOMA: os flashcards e o caso clínico DEVEM ser escritos em português do
            Brasil, mesmo quando o artigo-fonte estiver em inglês (a maior parte do acervo
            é literatura científica em inglês) — traduza o conteúdo. Mantenha entre
            parênteses o termo técnico original quando ele for a forma reconhecida
            (LI4/IG4, pinyin, nome de fórmula). Traduzir não é licença para adicionar:
            continua valendo a regra 1 abaixo.

            REGRAS ABSOLUTAS:
            1. Use SOMENTE fatos, termos, pontos e padrões que aparecem literalmente no
               artigo. NUNCA introduza informação externa, mesmo que pareça relacionada.
            2. Cada flashcard cobre UM fato atômico do artigo — pergunta curta em "front",
               resposta curta e precisa em "back". Não repita a mesma pergunta duas vezes.
            3. Se o artigo não tiver conteúdo suficiente para 10 flashcards distintos,
               gere menos — nunca invente para completar dez.
            4. O caso clínico é claramente FICTÍCIO e educacional: identifique-o com
               iniciais genéricas (nunca nome completo real), idade e sintomas coerentes
               SOMENTE com o que o artigo descreve. É material de estudo, não uma sugestão
               de diagnóstico ou tratamento para paciente nenhum.
            5. Se o artigo não permitir construir um caso coerente (ex.: é sobre história
               ou epistemologia da MTC, sem padrão clínico aplicável), devolva
               "clinicalCase": null — nunca force um caso artificial.
            6. NUNCA inclua texto fora do JSON pedido.

            RESPONDA EXCLUSIVAMENTE no formato JSON abaixo, sem texto antes ou depois:

            {
              "flashcards": [
                {"front": "Pergunta objetiva", "back": "Resposta objetiva", "category": "Categoria do artigo"}
              ],
              "clinicalCase": {
                "title": "Paciente [iniciais], [idade] anos",
                "vignette": "Queixa e história coerentes com o artigo, em markdown",
                "questions": ["Pergunta de estudo 1", "Pergunta de estudo 2"],
                "answerKey": "Resposta comentada em markdown, citando só o que está no artigo",
                "category": "Categoria do artigo"
              }
            }
        """.trimIndent()
    }
}
