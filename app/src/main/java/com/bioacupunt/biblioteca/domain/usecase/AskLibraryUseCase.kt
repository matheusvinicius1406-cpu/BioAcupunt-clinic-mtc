package com.bioacupunt.biblioteca.domain.usecase

import com.bioacupunt.ai.core.AiRepository
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.biblioteca.domain.search.MtcRetriever

/**
 * "Perguntar à Biblioteca" — the only sanctioned way to ask the AI a knowledge
 * question in BioAcupunt.
 *
 * The gate is in [invoke], not in the prompt: when retrieval returns nothing, the
 * model is **never called**. This is deliberate. A prompt that says "do not make
 * things up" is a request; a branch that refuses to call the model is a guarantee.
 */
class AskLibraryUseCase(
    private val retriever: MtcRetriever,
    private val ai: AiRepository,
) {

    sealed interface Answer {
        /** Grounded answer, with the passages the practitioner can go verify. */
        data class Grounded(
            val text: String,
            val sources: List<MtcRetriever.Passage>,
            /** True when the model ran on-device and no data left the phone. */
            val local: Boolean,
        ) : Answer

        /** Retrieval found nothing. The model was not consulted. */
        data object NoEvidence : Answer

        data class Failed(val message: String) : Answer
    }

    suspend operator fun invoke(question: String): Answer {
        if (question.isBlank()) return Answer.NoEvidence

        val grounding = retriever.retrieve(question)

        // THE GATE. No evidence -> no generation. An empty context is the condition
        // under which a language model confabulates most confidently, so we simply
        // do not put it in that position.
        if (!grounding.hasEvidence) return Answer.NoEvidence

        val request = AiRequest(
            prompt = MtcRetriever.buildPrompt(grounding),
            systemPrompt = MtcRetriever.SYSTEM_PROMPT,
            // Low temperature: this is a lookup task, not a creative one.
            temperature = 0.2,
            maxTokens = 800,
            // Keep it on the device when the device can. Library questions often carry
            // patient context in the phrasing ("paciente gestante com insônia...").
            preferLocal = true,
            taskHint = "library-rag",
        )

        return ai.generate(request).fold(
            onSuccess = { result ->
                Answer.Grounded(
                    text = result.text,
                    sources = grounding.passages,
                    local = result.metadata["execution"] == "local",
                )
            },
            onFailure = { error ->
                Answer.Failed(error.message ?: "Falha ao consultar a IA")
            },
        )
    }
}
