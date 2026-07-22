package com.bioacupunt.ai.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.ai.domain.usecase.GenerateAiResponseUseCase
import com.bioacupunt.biblioteca.domain.search.MtcRetriever
import com.bioacupunt.biblioteca.domain.usecase.AskLibraryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UnifiedChatRole { USER, ASSISTANT }

data class UnifiedChatTurn(
    val role: UnifiedChatRole,
    val text: String,
    /** Only populated when the reply came from the RAG (Grounded) path. */
    val sources: List<MtcRetriever.Passage> = emptyList(),
)

data class UnifiedAiChatUiState(
    val messages: List<UnifiedChatTurn> = listOf(
        UnifiedChatTurn(
            UnifiedChatRole.ASSISTANT,
            "Olá, Dra. Sou a BioAI. Pergunte sobre padrões, pontos e protocolos de MTC, ou " +
                "qualquer dúvida sobre o app — quando a resposta vier da biblioteca revisada, " +
                "mostro as fontes.",
        ),
    ),
    val input: String = "",
    val thinking: Boolean = false,
    val contextLoaded: String? = null,
)

/**
 * O ÚNICO chat de IA do app — junta o antigo "Consultar IA" (RAG clínico) e o antigo
 * "Chat Geral" (assistente livre) numa conversa só, como pedido pela médica.
 *
 * ## O roteamento, e por que ele não abre uma brecha na R2
 *
 * Toda mensagem passa **sempre** primeiro por [askLibrary] — [AskLibraryUseCase],
 * sem nenhuma alteração no gate `if (!grounding.hasEvidence) return Answer.NoEvidence`.
 *
 *  - [AskLibraryUseCase.Answer.Grounded] -> mostra a resposta com as passagens/fontes da
 *    biblioteca, exatamente como a antiga aba "Consultar IA" mostrava.
 *  - [AskLibraryUseCase.Answer.Failed] -> mostra o erro, exatamente como antes. Uma falha de
 *    infraestrutura (timeout, modelo indisponível) NÃO cai no fallback de chat geral — cair
 *    fingiria uma segunda tentativa de resposta livre onde a primeira já tinha achado
 *    evidência e falhado ao gerar sobre ela.
 *  - [AskLibraryUseCase.Answer.NoEvidence] -> **só então** [runFallback] chama
 *    [generateAiResponse] (o antigo "Chat Geral"). Isso cobre tanto perguntas genéricas sobre
 *    o app (que nunca teriam evidência na biblioteca de MTC de qualquer forma) quanto
 *    perguntas clínicas para as quais a biblioteca não tem artigo — e para essas, o
 *    [buildFallbackSystemPrompt] instrui recusa explícita de diagnóstico/conselho clínico.
 *
 * A garantia real nunca foi o prompt — é o `if` dentro de `AskLibraryUseCase.invoke`, que já
 * rodou e já decidiu "sem evidência" antes deste fallback ser sequer chamado. Este ViewModel
 * não pula essa chamada em nenhum caminho, e não tem nenhuma forma de responder uma pergunta
 * clínica grounded sem passar pelo retriever primeiro.
 */
class UnifiedAiChatViewModel(
    private val askLibrary: AskLibraryUseCase,
    private val generateAiResponse: GenerateAiResponseUseCase,
    private val contextBuilder: AppContextBuilder,
) : ViewModel() {

    private val _state = MutableStateFlow(UnifiedAiChatUiState())
    val state: StateFlow<UnifiedAiChatUiState> = _state.asStateFlow()

    private var fallbackSystemPrompt: String = buildFallbackSystemPrompt()

    init {
        // Carrega o contexto do app (consultas de hoje, nome da médica) de forma assíncrona.
        // Só é usado pelo caminho de fallback — o caminho RAG nunca recebe este contexto,
        // porque seu prompt é inteiramente definido por MtcRetriever.buildPrompt/SYSTEM_PROMPT.
        viewModelScope.launch {
            val ctx = runCatching { contextBuilder.build() }.getOrDefault("")
            _state.update { it.copy(contextLoaded = ctx) }
            fallbackSystemPrompt = buildFallbackSystemPrompt(ctx)
        }
    }

    fun onInputChanged(text: String) {
        _state.update { it.copy(input = text) }
    }

    /** Atualiza o contexto do app (chamado quando o app state muda, ex.: nova consulta). */
    fun refreshContext() {
        viewModelScope.launch {
            val ctx = runCatching { contextBuilder.build() }.getOrDefault("")
            _state.update { it.copy(contextLoaded = ctx) }
            fallbackSystemPrompt = buildFallbackSystemPrompt(ctx)
        }
    }

    fun send(question: String = _state.value.input) {
        if (question.isBlank() || _state.value.thinking) return
        _state.update {
            it.copy(
                messages = it.messages + UnifiedChatTurn(UnifiedChatRole.USER, question),
                input = "",
                thinking = true,
            )
        }
        viewModelScope.launch {
            val reply = when (val answer = askLibrary(question)) {
                is AskLibraryUseCase.Answer.Grounded ->
                    UnifiedChatTurn(UnifiedChatRole.ASSISTANT, answer.text, answer.sources)

                AskLibraryUseCase.Answer.NoEvidence -> runFallback(question)

                is AskLibraryUseCase.Answer.Failed -> UnifiedChatTurn(
                    UnifiedChatRole.ASSISTANT,
                    "Não consegui consultar a IA agora: ${answer.message}",
                )
            }
            _state.update { it.copy(messages = it.messages + reply, thinking = false) }
        }
    }

    /**
     * Chamado exclusivamente depois que [askLibrary] já devolveu [AskLibraryUseCase.Answer.NoEvidence]
     * — ou seja, depois que o gate R2 já rejeitou o caminho RAG para esta pergunta. Não é um atalho
     * paralelo: é o que acontece DEPOIS que o portão já fechou essa porta.
     */
    private suspend fun runFallback(question: String): UnifiedChatTurn {
        val request = AiRequest(
            prompt = question,
            systemPrompt = fallbackSystemPrompt,
            temperature = 0.7,
            maxTokens = 1024,
            preferLocal = true,
            taskHint = "general-chat",
        )
        return generateAiResponse(request).fold(
            onSuccess = { result -> UnifiedChatTurn(UnifiedChatRole.ASSISTANT, result.text) },
            onFailure = { error ->
                UnifiedChatTurn(
                    UnifiedChatRole.ASSISTANT,
                    "Não encontrei evidência na biblioteca, e não consegui processar agora: " +
                        (error.message?.lowercase() ?: "tente novamente."),
                )
            },
        )
    }

    /**
     * System prompt do caminho de fallback. Substitui o antigo prompt de "Chat Geral", que
     * mandava a médica para "a aba Consultar IA ao lado" — essa aba não existe mais, e a
     * fusão exige que o próprio modelo saiba recusar quando a pergunta é clínica.
     *
     * Isto é reforço de prompt, deliberadamente a linha mais fraca da defesa (como o
     * CLAUDE.md descreve para o SYSTEM_PROMPT do RAG). A garantia real é que este código só
     * roda depois que [AskLibraryUseCase] já procurou evidência e não achou nenhuma.
     */
    private fun buildFallbackSystemPrompt(context: String = ""): String = buildString {
        appendLine("Você é o assistente do aplicativo BioAcupunt — um sistema clínico de Medicina Tradicional Chinesa (MTC).")
        appendLine()
        appendLine("Esta pergunta chegou até você porque a busca na biblioteca de artigos revisados de MTC NÃO encontrou")
        appendLine("nenhum trecho relevante. Isso pode significar duas coisas:")
        appendLine("  (a) é uma dúvida genérica sobre o app, horário, agenda, organização etc.; ou")
        appendLine("  (b) é uma pergunta clínica de MTC para a qual não há, nesta consulta, uma fonte revisada.")
        appendLine()
        appendLine("REGRAS:")
        appendLine("- Se você não tiver certeza ABSOLUTA de que a pergunta é (a), trate-a como (b): diga que não")
        appendLine("  encontrou uma fonte revisada da biblioteca para responder com segurança clínica, e sugira")
        appendLine("  reformular a pergunta ou consultar a biblioteca diretamente. Não tente ser útil demais aqui.")
        appendLine("- NUNCA dê diagnóstico, tratamento, prescrição de pontos/fórmulas, ou interpretação de sintomas")
        appendLine("  a partir do seu próprio conhecimento. NUNCA invente uma resposta clínica.")
        appendLine("- Perguntas sobre o funcionamento do app (agenda, prontuário, financeiro, biblioteca etc.) você")
        appendLine("  pode responder livremente.")
        appendLine("- Seja breve e objetiva. A médica está ocupada.")
        appendLine("- Use português brasileiro natural.")
        appendLine("- Se não souber algo, diga que não sabe.")
        if (context.isNotBlank()) {
            appendLine()
            appendLine("CONTEXTO ATUAL DO APP (pode usar para personalizar respostas do tipo (a)):")
            appendLine(context.trim())
        }
    }
}

class UnifiedAiChatViewModelFactory(
    private val askLibrary: AskLibraryUseCase,
    private val generateAiResponse: GenerateAiResponseUseCase,
    private val contextBuilder: AppContextBuilder,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        UnifiedAiChatViewModel(askLibrary, generateAiResponse, contextBuilder) as T
}
