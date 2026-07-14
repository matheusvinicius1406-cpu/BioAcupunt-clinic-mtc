package com.bioacupunt.biblioteca.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.biblioteca.data.MtcKnowledgeBase
import com.bioacupunt.biblioteca.domain.model.MtcArticle
import com.bioacupunt.biblioteca.domain.usecase.AskLibraryUseCase
import com.bioacupunt.biblioteca.domain.usecase.ToggleFavoriteArticle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BibliotecaUiState(
    val query: String = "",
    val category: String? = null,
    val articles: List<MtcArticle> = MtcKnowledgeBase.articles,
    val favoriteIds: Set<String> = emptySet(),
    val askQuestion: String = "",
    val askAnswer: AskLibraryUseCase.Answer? = null,
    val asking: Boolean = false,
)

/**
 * Backs the Biblioteca screen. Two content sources, kept deliberately separate:
 *
 *  - Browsing/filtering [MtcKnowledgeBase.articles] is a plain, deterministic list
 *    operation — no model involved, ever.
 *  - [ask] is the *only* place this screen touches the model, and it goes through
 *    [AskLibraryUseCase], which refuses to call the model when the library has no
 *    evidence for the question (R2). This ViewModel does not — and must not —
 *    add a second, ungated path to the AI.
 */
class BibliotecaViewModel(
    private val askLibrary: AskLibraryUseCase,
    private val toggleFavoriteArticle: ToggleFavoriteArticle,
    observeFavorites: kotlinx.coroutines.flow.Flow<Set<String>>,
) : ViewModel() {

    private val _state = MutableStateFlow(BibliotecaUiState())
    val state: StateFlow<BibliotecaUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeFavorites
                .catch { emit(emptySet()) }
                .collect { ids -> _state.update { it.copy(favoriteIds = ids) } }
        }
    }

    fun onQueryChanged(query: String) {
        _state.update { it.copy(query = query, articles = filtered(query, it.category)) }
    }

    fun onCategorySelected(category: String?) {
        _state.update { it.copy(category = category, articles = filtered(it.query, category)) }
    }

    private fun filtered(query: String, category: String?): List<MtcArticle> {
        var base = MtcKnowledgeBase.articles
        if (!category.isNullOrBlank()) base = base.filter { it.category == category }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            base = base.filter {
                it.title.lowercase().contains(q) || it.summary.lowercase().contains(q) ||
                    it.tags.any { t -> t.lowercase().contains(q) }
            }
        }
        return base
    }

    fun toggleFavorite(articleId: String) {
        val isFav = articleId in _state.value.favoriteIds
        viewModelScope.launch { toggleFavoriteArticle(articleId, makeFavorite = !isFav) }
    }

    fun onAskQuestionChanged(text: String) {
        _state.update { it.copy(askQuestion = text) }
    }

    /** The only sanctioned "ask the AI" path — see [AskLibraryUseCase]. */
    fun ask() {
        val question = _state.value.askQuestion
        if (question.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(asking = true) }
            val answer = askLibrary(question)
            _state.update { it.copy(asking = false, askAnswer = answer) }
        }
    }

    fun clearAnswer() {
        _state.update { it.copy(askAnswer = null, askQuestion = "") }
    }
}

class BibliotecaViewModelFactory(
    private val askLibrary: AskLibraryUseCase,
    private val toggleFavoriteArticle: ToggleFavoriteArticle,
    private val observeFavorites: kotlinx.coroutines.flow.Flow<Set<String>>,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return BibliotecaViewModel(askLibrary, toggleFavoriteArticle, observeFavorites) as T
    }
}
