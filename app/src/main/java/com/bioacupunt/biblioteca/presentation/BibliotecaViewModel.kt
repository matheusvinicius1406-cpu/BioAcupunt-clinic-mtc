package com.bioacupunt.biblioteca.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.biblioteca.data.MtcKnowledgeBase
import com.bioacupunt.biblioteca.data.search.HybridSearchService
import com.bioacupunt.biblioteca.domain.model.MtcArticle
import com.bioacupunt.biblioteca.domain.usecase.AskLibraryUseCase
import com.bioacupunt.biblioteca.domain.usecase.ToggleFavoriteArticle
import com.bioacupunt.data.local.database.KnowledgeNodeDao
import com.bioacupunt.data.local.model.KnowledgeNodeEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Modo de busca da biblioteca. */
enum class SearchMode {
    /** Busca textual local no acervo fixo (MtcKnowledgeBase + curadoria). */
    LEGACY,
    /** Busca híbrida (FTS5 + sqlite-vec + RRF) nos nós do MKIS. */
    MKIS_HYBRID,
}

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
    /**
     * True when [observeApprovedArticles] failed and [allArticles] is degraded to just the
     * fixed 16 (no approved-via-Curadoria articles). The stats row must show "—" instead of
     * a real-looking count in this case — a failure must never render identically to "no
     * approvals yet".
     */
    val articlesUnavailable: Boolean = false,
    val favoriteIds: Set<String> = emptySet(),
    val askQuestion: String = "",
    val askAnswer: AskLibraryUseCase.Answer? = null,
    val asking: Boolean = false,
    /** Modo de busca ativo. */
    val searchMode: SearchMode = SearchMode.LEGACY,
    /** Resultados da busca híbrida MKIS (quando searchMode = MKIS_HYBRID). */
    val hybridResults: List<HybridResultItem> = emptyList(),
    val isSearching: Boolean = false,
    /** True quando a última busca híbrida falhou — distingue de "zero resultados" real. */
    val hybridSearchFailed: Boolean = false,
    /** Nó MKIS selecionado para exibir no detail sheet. */
    val selectedMkisNode: KnowledgeNodeEntity? = null,
    val selectedMkisNodeScore: Double = 0.0,
)

/** Item de resultado da busca híbrida, adaptado para exibição na lista. */
data class HybridResultItem(
    val id: String,
    val title: String,
    val summary: String,
    /** Score RRF combinado (0..1). */
    val score: Double,
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
    private val hybridSearchService: HybridSearchService?,
    private val knowledgeNodeDao: KnowledgeNodeDao?,
    observeFavorites: kotlinx.coroutines.flow.Flow<Set<String>>,
    observeApprovedArticles: kotlinx.coroutines.flow.Flow<List<MtcArticle>>,
) : ViewModel() {

    private val _state = MutableStateFlow(BibliotecaUiState())
    val state: StateFlow<BibliotecaUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Observa favoritos E artigos aprovados simultaneamente. O par (lista, falhou)
            // preserva a distinção entre "curadoria não aprovou nada ainda" (lista vazia,
            // falhou=false) e "a query de aprovados quebrou" (falhou=true) — sem isso, as
            // duas situações colapsavam no mesmo emptyList() e o card de estatísticas
            // mostrava um número plausível, porém incompleto, sem nenhum indício de falha.
            combine(
                observeFavorites.onStart { emit(emptySet()) }.catch { emit(emptySet()) },
                observeApprovedArticles
                    .map { it to false }
                    .onStart { emit(emptyList<MtcArticle>() to false) }
                    .catch { emit(emptyList<MtcArticle>() to true) },
            ) { favIds, (approved, approvedFailed) ->
                val fixed = MtcKnowledgeBase.articles
                // Merge: fixos têm prioridade. Aprovados complementam.
                val extra = approved.filter { it.id !in fixed.map { f -> f.id } }
                val merged = fixed + extra
                Triple(favIds, merged, approvedFailed)
            }.collect { (favIds, merged, approvedFailed) ->
                _state.update { state ->
                    val current = state.copy(
                        favoriteIds = favIds,
                        articles = merged,
                        allArticles = merged,
                        articlesUnavailable = approvedFailed,
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
            if (state.searchMode == SearchMode.MKIS_HYBRID) {
                // Limpar resultados (e o estado de falha) se a query foi limpa
                val cleared = if (query.isBlank()) emptyList<HybridResultItem>() else state.hybridResults
                val failedCleared = if (query.isBlank()) false else state.hybridSearchFailed
                state.copy(query = query, hybridResults = cleared, hybridSearchFailed = failedCleared)
            } else {
                state.copy(query = query, articles = filtered(query, state.category, state.allArticles))
            }
        }
        // Disparar busca híbrida se estiver nesse modo
        if (_state.value.searchMode == SearchMode.MKIS_HYBRID && query.isNotBlank()) {
            performHybridSearch(query)
        }
    }

    fun onCategorySelected(category: String?) {
        _state.update { state ->
            if (state.searchMode == SearchMode.MKIS_HYBRID) {
                state.copy(category = category)
            } else {
                state.copy(category = category, articles = filtered(state.query, category, state.allArticles))
            }
        }
        // Re-aplicar busca híbrida com filtro de categoria
        val s = _state.value
        if (s.searchMode == SearchMode.MKIS_HYBRID && s.query.isNotBlank()) {
            performHybridSearch(s.query)
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

    // ======================== Busca Híbrida MKIS ========================

    fun toggleSearchMode() {
        _state.update { state ->
            val newMode = if (state.searchMode == SearchMode.LEGACY) SearchMode.MKIS_HYBRID else SearchMode.LEGACY
            state.copy(
                searchMode = newMode,
                hybridResults = emptyList(),
                hybridSearchFailed = false,
                query = "",
                articles = state.allArticles,
            )
        }
    }

    /** Realiza busca nos nós do MKIS via [HybridSearchService]. */
    private fun performHybridSearch(query: String) {
        val svc = hybridSearchService ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, hybridSearchFailed = false) }
            try {
                val results = svc.search(query, maxResults = 20)
                // TODO: Usar score real do RRF quando o HybridSearchService expor scores normalizados
                val items = results.mapIndexed { i, r ->
                    HybridResultItem(
                        id = r.articleId,
                        title = r.title,
                        summary = r.summary,
                        score = 1.0 - (i.toDouble() / results.size.coerceAtLeast(1)),
                    )
                }
                _state.update { it.copy(hybridResults = items, isSearching = false) }
            } catch (e: Exception) {
                // Falha real de busca (FTS5/embeddings indisponível) nunca deve parecer
                // "zero resultados" — a tela distingue os dois pelo hybridSearchFailed.
                _state.update { it.copy(hybridResults = emptyList(), isSearching = false, hybridSearchFailed = true) }
            }
        }
    }

    fun onHybridResultClick(item: HybridResultItem) {
        val dao = knowledgeNodeDao ?: return
        viewModelScope.launch {
            val node = dao.getById(item.id)
            _state.update { it.copy(
                selectedMkisNode = node,
                selectedMkisNodeScore = item.score,
            )}
        }
    }

    fun clearSelectedNode() {
        _state.update { it.copy(selectedMkisNode = null, selectedMkisNodeScore = 0.0) }
    }
}

class BibliotecaViewModelFactory(
    private val askLibrary: AskLibraryUseCase,
    private val toggleFavoriteArticle: ToggleFavoriteArticle,
    private val hybridSearchService: HybridSearchService?,
    private val knowledgeNodeDao: KnowledgeNodeDao?,
    private val observeFavorites: kotlinx.coroutines.flow.Flow<Set<String>>,
    private val observeApprovedArticles: kotlinx.coroutines.flow.Flow<List<MtcArticle>>,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return BibliotecaViewModel(
            askLibrary,
            toggleFavoriteArticle,
            hybridSearchService,
            knowledgeNodeDao,
            observeFavorites,
            observeApprovedArticles,
        ) as T
    }
}
