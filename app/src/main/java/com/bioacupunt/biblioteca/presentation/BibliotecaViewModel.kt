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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BibliotecaUiState(
    val query: String = "",
    val category: String? = null,
    val articles: List<MtcArticle> = MtcKnowledgeBase.articles,
    /**
     * O merge completo (fixos + aprovados), nunca filtrado por busca/categoria.
     * [articles] é o que a lista mostra (pode estar filtrado); isto é o universo
     * completo — necessário para achar "artigos relacionados" de um artigo aberto
     * mesmo quando a lista visível está filtrada por uma busca que os esconderia.
     */
    val allArticles: List<MtcArticle> = MtcKnowledgeBase.articles,
    val favoriteIds: Set<String> = emptySet(),
    val askQuestion: String = "",
    val askAnswer: AskLibraryUseCase.Answer? = null,
    val asking: Boolean = false,
)

/**
 * Backs the Biblioteca screen. Two content sources, kept deliberately separate:
 *
 *  - **Browsing/filtering** mescla [MtcKnowledgeBase.articles] (16 fixos) com os
 *    artigos aprovados via curadoria ([observeApprovedArticles]). Artigos fixos
 *    têm prioridade na mesma id — conteúdo revisado não é sobrescrito por
 *    curadoria.
 *  - **[ask]** is the *only* place this screen touches the model, and it goes through
 *    [AskLibraryUseCase], which refuses to call the model when the library has no
 *    evidence for the question (R2). This ViewModel does not — and must not —
 *    add a second, ungated path to the AI.
 */
class BibliotecaViewModel(
    private val askLibrary: AskLibraryUseCase,
    private val toggleFavoriteArticle: ToggleFavoriteArticle,
    observeFavorites: kotlinx.coroutines.flow.Flow<Set<String>>,
    observeApprovedArticles: kotlinx.coroutines.flow.Flow<List<MtcArticle>>,
) : ViewModel() {

    private val _state = MutableStateFlow(BibliotecaUiState())
    val state: StateFlow<BibliotecaUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Observa favoritos E artigos aprovados simultaneamente
            combine(
                observeFavorites.onStart { emit(emptySet()) }.catch { emit(emptySet()) },
                observeApprovedArticles.onStart { emit(emptyList()) }.catch { emit(emptyList()) },
            ) { favIds, approved ->
                val fixed = MtcKnowledgeBase.articles
                // Merge: fixos têm prioridade. Aprovados complementam.
                val approvedIds = approved.map { it.id }.toSet()
                val extra = approved.filter { it.id !in fixed.map { f -> f.id } }
                val merged = fixed + extra
                favIds to merged
            }.collect { (favIds, merged) ->
                _state.update { state ->
                    val current = state.copy(
                        favoriteIds = favIds,
                        articles = merged,
                        allArticles = merged,
                    )
                    // Re-aplica filtros ativos se houver query/categoria
                    if (current.query.isNotBlank() || !current.category.isNullOrBlank()) {
                        current.copy(articles = filtered(current.query, current.category, merged))
                    } else {
                        current
                    }
                }
            }
        }
    }

    fun onQueryChanged(query: String) {
        _state.update { state ->
            state.copy(query = query, articles = filtered(query, state.category, state.articles))
        }
    }

    fun onCategorySelected(category: String?) {
        _state.update { state ->
            state.copy(category = category, articles = filtered(state.query, category, state.articles))
        }
    }

    private fun filtered(query: String, category: String?, allArticles: List<MtcArticle>): List<MtcArticle> {
        var base = allArticles
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
    private val observeApprovedArticles: kotlinx.coroutines.flow.Flow<List<MtcArticle>>,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return BibliotecaViewModel(askLibrary, toggleFavoriteArticle, observeFavorites, observeApprovedArticles) as T
    }
}
