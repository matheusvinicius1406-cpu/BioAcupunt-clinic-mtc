package com.bioacupunt.biblioteca.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.biblioteca.domain.search.MtcRetriever
import com.bioacupunt.biblioteca.domain.usecase.AskLibraryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ChatRole { USER, ASSISTANT }

data class ChatTurn(
    val role: ChatRole,
    val text: String,
    val sources: List<MtcRetriever.Passage> = emptyList(),
)

data class AiAssistantUiState(
    val messages: List<ChatTurn> = listOf(
        ChatTurn(
            ChatRole.ASSISTANT,
            "Olá, Dra. Sou a BioAI. Pergunte sobre padrões, pontos ou protocolos de MTC — respondo só com base nos artigos revisados da biblioteca.",
        )
    ),
    val input: String = "",
    val thinking: Boolean = false,
)

/**
 * The ONLY AI chat surface in the app that talks about clinical/MTC content —
 * and it only ever talks through [AskLibraryUseCase], which refuses to call the
 * model when the library has no evidence (R2). There is deliberately no
 * "if the model fails, show a canned diagnosis" fallback here: an empty or
 * failed answer is shown as exactly that, never dressed up as a real one.
 */
class AiAssistantViewModel(private val askLibrary: AskLibraryUseCase) : ViewModel() {

    private val _state = MutableStateFlow(AiAssistantUiState())
    val state: StateFlow<AiAssistantUiState> = _state.asStateFlow()

    fun onInputChanged(text: String) {
        _state.update { it.copy(input = text) }
    }

    fun send(question: String = _state.value.input) {
        if (question.isBlank() || _state.value.thinking) return
        _state.update { it.copy(messages = it.messages + ChatTurn(ChatRole.USER, question), input = "", thinking = true) }
        viewModelScope.launch {
            val reply = when (val answer = askLibrary(question)) {
                is AskLibraryUseCase.Answer.Grounded -> ChatTurn(ChatRole.ASSISTANT, answer.text, answer.sources)
                AskLibraryUseCase.Answer.NoEvidence -> ChatTurn(
                    ChatRole.ASSISTANT,
                    "Não encontrei evidência na biblioteca para responder isso. Tente reformular, ou procure diretamente na Biblioteca.",
                )
                is AskLibraryUseCase.Answer.Failed -> ChatTurn(ChatRole.ASSISTANT, "Não consegui consultar a IA agora: ${answer.message}")
            }
            _state.update { it.copy(messages = it.messages + reply, thinking = false) }
        }
    }
}

class AiAssistantViewModelFactory(private val askLibrary: AskLibraryUseCase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AiAssistantViewModel(askLibrary) as T
    }
}
