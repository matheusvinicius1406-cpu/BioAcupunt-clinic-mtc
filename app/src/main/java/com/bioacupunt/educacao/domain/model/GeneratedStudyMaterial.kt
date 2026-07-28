package com.bioacupunt.educacao.domain.model

/**
 * Rascunho de UM flashcard sugerido pela IA a partir de um artigo aprovado — nunca salvo
 * sozinho. A médica aceita (grava em `flashcards` via [com.bioacupunt.educacao.domain.repository.FlashcardRepository.saveCard])
 * ou descarta cada um.
 */
data class GeneratedFlashcardDraft(
    val front: String,
    val back: String,
    val category: String,
)

/** Rascunho de UM caso clínico simulado sugerido pela IA a partir de um artigo aprovado. */
data class GeneratedCaseDraft(
    val title: String,
    val vignette: String,
    val questions: List<String>,
    val answerKey: String,
    val category: String,
)

/**
 * Resultado de [com.bioacupunt.educacao.domain.usecase.GenerateStudyMaterialUseCase] — até
 * 10 flashcards + 1 caso, sempre em memória até a médica aceitar item por item (ou o lote
 * inteiro) na mesma tela de Curadoria onde aprovou o artigo-fonte. R4: rascunho, nunca
 * gravado automaticamente.
 */
data class StudyMaterialDraft(
    val articleId: String,
    val articleTitle: String,
    val flashcards: List<GeneratedFlashcardDraft> = emptyList(),
    val clinicalCase: GeneratedCaseDraft? = null,
) {
    val isEmpty: Boolean get() = flashcards.isEmpty() && clinicalCase == null

    companion object {
        fun empty(articleId: String, articleTitle: String) = StudyMaterialDraft(articleId, articleTitle)
    }
}
