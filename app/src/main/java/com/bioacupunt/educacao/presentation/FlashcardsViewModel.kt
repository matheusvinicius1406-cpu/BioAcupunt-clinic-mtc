package com.bioacupunt.educacao.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.biblioteca.domain.model.MtcArticle
import com.bioacupunt.biblioteca.domain.model.MtcCategory
import com.bioacupunt.core.util.MarkdownSections
import com.bioacupunt.core.util.Result
import com.bioacupunt.educacao.domain.model.Flashcard
import com.bioacupunt.educacao.domain.model.StudyCard
import com.bioacupunt.educacao.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado do picker "Criar de artigo": artigo → seção → (fecha, abre `editor` pré-preenchido). */
data class CreateFromArticleState(
    val articles: List<MtcArticle> = emptyList(),
    val selectedArticle: MtcArticle? = null,
    val sections: List<String> = emptyList(),
    val isLoading: Boolean = false,
)

data class FlashcardsUiState(
    /** Reativo: união 12 fixos + cards da médica, cada um com progresso. Atualiza a cada review. */
    val deck: List<StudyCard> = emptyList(),
    /**
     * SNAPSHOT: só rebuilda em troca de categoria/Reiniciar/CRUD/"Estudar mesmo assim" —
     * nunca reordena debaixo do dedo da médica a meio da sessão.
     */
    val queue: List<StudyCard> = emptyList(),
    val queueIndex: Int = 0,
    val dueCount: Int = 0,
    /** true quando a fila atual inclui cards ainda não vencidos (via "Estudar mesmo assim"). */
    val studyingAheadOfSchedule: Boolean = false,
    val selectedCategory: String? = null,
    val knownCount: Int = 0,
    val unknownCount: Int = 0,
    /** Fim de fila alcançado respondendo — resumo de sessão. Nunca true para uma fila vazia de saída. */
    val sessionComplete: Boolean = false,
    val editor: Flashcard? = null,
    val createFromArticle: CreateFromArticleState? = null,
    /** Snackbar informativo (ex.: falha ao salvar progresso). Nunca bloqueia a sessão. */
    val message: String? = null,
) {
    val currentCard: StudyCard? get() = queue.getOrNull(queueIndex)

    /** Vencimento mais próximo entre os cards da categoria atual — para "próxima revisão em <data>". */
    val nextDueAtEpochMs: Long?
        get() = deckForCategory().mapNotNull { it.progress?.dueAtEpochMs }.minOrNull()

    fun deckForCategory(): List<StudyCard> =
        if (selectedCategory == null) deck else deck.filter { it.card.category == selectedCategory }
}

class FlashcardsViewModelFactory(
    private val repository: FlashcardRepository,
    private val sourceArticles: suspend () -> List<MtcArticle>,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return FlashcardsViewModel(repository, sourceArticles) as T
    }
}

class FlashcardsViewModel(
    private val repository: FlashcardRepository,
    private val sourceArticles: suspend () -> List<MtcArticle>,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    private val _state = MutableStateFlow(FlashcardsUiState())
    val state: StateFlow<FlashcardsUiState> = _state.asStateFlow()

    /** true até a primeira fila ser construída a partir do deck carregado. */
    private var queueDirty = true

    init {
        viewModelScope.launch {
            repository.observeDeck()
                .catch { emit(emptyList()) }
                .collect { deck -> onDeckLoaded(deck) }
        }
    }

    private fun onDeckLoaded(deck: List<StudyCard>) {
        val nowMs = clock()
        _state.update { it.copy(deck = deck, dueCount = deck.count { sc -> sc.isDue(nowMs) }) }
        if (queueDirty) {
            queueDirty = false
            rebuildQueue()
        }
    }

    fun onCategorySelected(category: String?) {
        _state.update { it.copy(selectedCategory = category) }
        rebuildQueue()
    }

    fun restartSession() = rebuildQueue()

    /** A médica pediu para estudar mesmo sem nada vencido — puxa os não-vencidos também. */
    fun studyAnyway() = rebuildQueue(includeNotDue = true)

    private fun rebuildQueue(includeNotDue: Boolean = false) {
        val nowMs = clock()
        val filtered = _state.value.deckForCategory()
        val due = filtered.filter { it.isDue(nowMs) }
            .sortedWith(compareBy({ it.progress?.box ?: 0 }, { it.progress?.dueAtEpochMs ?: 0L }))
        val queue = if (!includeNotDue) due else {
            val notDue = filtered.filterNot { it.isDue(nowMs) }
                .sortedBy { it.progress?.dueAtEpochMs ?: Long.MAX_VALUE }
            due + notDue
        }
        _state.update {
            it.copy(
                queue = queue,
                queueIndex = 0,
                knownCount = 0,
                unknownCount = 0,
                sessionComplete = false,
                studyingAheadOfSchedule = includeNotDue,
            )
        }
    }

    fun onAnswer(remembered: Boolean) {
        val studyCard = _state.value.currentCard ?: return
        viewModelScope.launch {
            val result = repository.recordReview(studyCard.card.key, remembered, clock())
            if (result is Result.Error) {
                _state.update { it.copy(message = "Não foi possível salvar o progresso. A sessão continua.") }
            }
            _state.update {
                val nextIndex = it.queueIndex + 1
                it.copy(
                    knownCount = it.knownCount + if (remembered) 1 else 0,
                    unknownCount = it.unknownCount + if (!remembered) 1 else 0,
                    queueIndex = nextIndex,
                    sessionComplete = nextIndex >= it.queue.size,
                )
            }
        }
    }

    // ── Editor: criar/editar card da médica ──────────────────────────────

    fun openNewCardEditor() {
        _state.update {
            it.copy(editor = Flashcard(key = "", front = "", back = "", category = it.selectedCategory.orEmpty(), builtin = false))
        }
    }

    fun openEditCardEditor(card: Flashcard) {
        if (card.builtin) return // a UI já esconde editar/apagar de fixos; o VM não confia nisso sozinho
        _state.update { it.copy(editor = card) }
    }

    fun updateEditor(transform: (Flashcard) -> Flashcard) {
        _state.update { s -> s.editor?.let { s.copy(editor = transform(it)) } ?: s }
    }

    fun cancelEditor() {
        _state.update { it.copy(editor = null) }
    }

    fun confirmEditor() {
        val draft = _state.value.editor ?: return
        if (draft.front.isBlank() || draft.back.isBlank()) {
            _state.update { it.copy(message = "Preencha frente e verso antes de salvar.") }
            return
        }
        viewModelScope.launch {
            when (val result = repository.saveCard(draft)) {
                is Result.Success -> {
                    queueDirty = true
                    _state.update { it.copy(editor = null) }
                }
                is Result.Error -> _state.update { it.copy(message = result.kind.userMessage) }
                else -> Unit
            }
        }
    }

    fun deleteCard(userRowId: Long) {
        viewModelScope.launch {
            when (val result = repository.deleteCard(userRowId)) {
                is Result.Error -> _state.update { it.copy(message = result.kind.userMessage) }
                else -> queueDirty = true
            }
        }
    }

    // ── Criar de artigo aprovado: artigo → seção → editor pré-preenchido ─

    fun startCreateFromArticle() {
        _state.update { it.copy(createFromArticle = CreateFromArticleState(isLoading = true)) }
        viewModelScope.launch {
            val articles = runCatching { sourceArticles() }.getOrDefault(emptyList())
            _state.update { it.copy(createFromArticle = it.createFromArticle?.copy(articles = articles, isLoading = false)) }
        }
    }

    fun selectArticleForCreate(article: MtcArticle) {
        val sections = MarkdownSections.split(article.content)
        _state.update {
            it.copy(createFromArticle = it.createFromArticle?.copy(selectedArticle = article, sections = sections))
        }
    }

    /** Nada é salvo aqui — só fecha o picker e abre o [FlashcardsUiState.editor] pré-preenchido. */
    fun selectSectionForCreate(section: String) {
        val article = _state.value.createFromArticle?.selectedArticle ?: return
        val title = MarkdownSections.titleOf(section)
        val body = MarkdownSections.bodyOf(section)
        val category = runCatching { MtcCategory.valueOf(article.category).label }.getOrDefault(article.category)
        _state.update {
            it.copy(
                editor = Flashcard(
                    key = "",
                    front = title,
                    back = body,
                    category = category,
                    builtin = false,
                    sourceArticleId = article.id,
                    sourceSection = title,
                ),
                createFromArticle = null,
            )
        }
    }

    fun cancelCreateFromArticle() {
        _state.update { it.copy(createFromArticle = null) }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }
}
