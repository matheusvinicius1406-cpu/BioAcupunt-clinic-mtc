package com.bioacupunt.biblioteca.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.biblioteca.data.repository.LibraryStagingRepository
import com.bioacupunt.biblioteca.domain.ingestion.LibraryContentPack
import com.bioacupunt.biblioteca.domain.ingestion.Provenance
import com.bioacupunt.core.util.AppJson
import com.bioacupunt.educacao.domain.model.GeneratedCaseDraft
import com.bioacupunt.educacao.domain.model.GeneratedFlashcardDraft
import com.bioacupunt.educacao.domain.model.Flashcard
import com.bioacupunt.educacao.domain.model.SimulatedCase
import com.bioacupunt.educacao.domain.model.StudyMaterialDraft
import com.bioacupunt.educacao.domain.repository.FlashcardRepository
import com.bioacupunt.educacao.domain.repository.SimulatedCaseRepository
import com.bioacupunt.educacao.domain.usecase.GenerateStudyMaterialUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString

/**
 * Tela de CURADORIA — a fila de revisão da médica.
 *
 * Aqui, e só aqui, conteúdo curado vira acervo consultável: o import apenas *encena*
 * (fila PENDING, via o portão R4 do [com.bioacupunt.biblioteca.domain.ingestion.LibraryIngestion]);
 * é o toque em "Aprovar" que autoriza. Rejeitar mantém o item fora do RAG para sempre.
 * Nenhuma IA escreve nem aprova nada — quem decide é a responsável clínica.
 */
class LibraryReviewViewModel(
    private val repo: LibraryStagingRepository,
    private val onContentChanged: (() -> Unit)? = null,
    private val generateStudyMaterial: GenerateStudyMaterialUseCase? = null,
    private val flashcardRepository: FlashcardRepository? = null,
    private val simulatedCaseRepository: SimulatedCaseRepository? = null,
) : ViewModel() {

    val pending: StateFlow<List<LibraryStagingRepository.StagedArticle>> =
        repo.observePending().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Filtros da fila de curadoria — só de EXIBIÇÃO. Não afetam o portão de
     * ingestão ([com.bioacupunt.biblioteca.domain.ingestion.LibraryIngestion]) nem o que
     * é aceito na entrada; escondem/mostram itens já validados que aguardam revisão.
     * `null` em categoria/proveniência = "Todas" (sem filtro).
     */
    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()

    private val _provenanceFilter = MutableStateFlow<Provenance?>(null)
    val provenanceFilter: StateFlow<Provenance?> = _provenanceFilter.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    /** Fila pendente já filtrada — é isto que a tela deve renderizar e contar. */
    val filteredPending: StateFlow<List<LibraryStagingRepository.StagedArticle>> =
        combine(pending, _categoryFilter, _provenanceFilter, _searchText) { items, category, provenance, text ->
            items.filter { staged ->
                (category == null || staged.article.category == category) &&
                    (provenance == null || staged.meta.provenance == provenance) &&
                    (text.isBlank() || staged.article.title.contains(text, ignoreCase = true))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setCategoryFilter(category: String?) { _categoryFilter.value = category }
    fun setProvenanceFilter(provenance: Provenance?) { _provenanceFilter.value = provenance }
    fun setSearchText(text: String) { _searchText.value = text }

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    // ── Flashcards + Caso simulado sugeridos do artigo recém-aprovado ──────
    //
    // Rascunho em memória só (R4): nada aqui grava em `flashcards`/`simulated_cases`
    // sem a médica aceitar item por item (ou o lote) logo abaixo, na mesma tela onde
    // ela aprovou o artigo-fonte.
    private val _studyMaterialDraft = MutableStateFlow<StudyMaterialDraft?>(null)
    val studyMaterialDraft: StateFlow<StudyMaterialDraft?> = _studyMaterialDraft.asStateFlow()

    private val _generatingStudyMaterial = MutableStateFlow(false)
    val generatingStudyMaterial: StateFlow<Boolean> = _generatingStudyMaterial.asStateFlow()

    private fun now() = System.currentTimeMillis()

    fun approve(id: String) = viewModelScope.launch {
        val article = pending.value.firstOrNull { it.article.id == id }?.article
        if (repo.approve(id, now())) {
            onContentChanged?.invoke()
            if (article != null) triggerStudyMaterialGeneration(article)
        } else {
            _feedback.value = "Não foi possível aprovar este item — ele pode ter sido removido ou tem um registro corrompido."
        }
    }
    fun reject(id: String) = viewModelScope.launch {
        if (repo.reject(id, now())) {
            onContentChanged?.invoke()
        } else {
            _feedback.value = "Não foi possível rejeitar este item — ele pode ter sido removido ou tem um registro corrompido."
        }
    }

    private fun triggerStudyMaterialGeneration(article: com.bioacupunt.biblioteca.domain.model.MtcArticle) {
        val useCase = generateStudyMaterial ?: return
        viewModelScope.launch {
            _generatingStudyMaterial.value = true
            _studyMaterialDraft.value = useCase(article).takeUnless { it.isEmpty }
            _generatingStudyMaterial.value = false
        }
    }

    /** Grava só o rascunho no repositório — sem tocar no estado, pra ser seguro em sequência dentro de um laço. */
    private suspend fun persistFlashcard(draft: GeneratedFlashcardDraft, articleId: String): Boolean {
        val repository = flashcardRepository ?: return false
        val result = repository.saveCard(
            Flashcard(
                key = "",
                front = draft.front,
                back = draft.back,
                category = draft.category,
                builtin = false,
                sourceArticleId = articleId,
            ),
        )
        if (result is com.bioacupunt.core.util.Result.Error) _feedback.value = result.kind.userMessage
        return result is com.bioacupunt.core.util.Result.Success
    }

    /** A médica aceitou ESTE flashcard sugerido — grava em `flashcards` e o remove do rascunho. */
    fun acceptFlashcard(draft: GeneratedFlashcardDraft) = viewModelScope.launch {
        val current = _studyMaterialDraft.value ?: return@launch
        if (persistFlashcard(draft, current.articleId)) {
            _studyMaterialDraft.value = _studyMaterialDraft.value?.let { it.copy(flashcards = it.flashcards - draft) }
        }
    }

    /**
     * Aceita todos os flashcards sugeridos ainda no rascunho de uma vez — sequencial numa
     * coroutine só (não uma por card): salvar N cards em paralelo, cada um lendo/escrevendo
     * o mesmo `StateFlow`, perderia atualizações (a última escrita vence, as outras somem).
     */
    fun acceptAllFlashcards() = viewModelScope.launch {
        val current = _studyMaterialDraft.value ?: return@launch
        val remaining = current.flashcards.toMutableList()
        for (draft in current.flashcards) {
            if (persistFlashcard(draft, current.articleId)) remaining.remove(draft)
        }
        _studyMaterialDraft.value = _studyMaterialDraft.value?.copy(flashcards = remaining)
    }

    /** A médica aceitou o caso clínico sugerido — grava em `simulated_cases` e o remove do rascunho. */
    fun acceptCase(draft: GeneratedCaseDraft) = viewModelScope.launch {
        val repository = simulatedCaseRepository ?: return@launch
        val current = _studyMaterialDraft.value ?: return@launch
        val result = repository.save(
            SimulatedCase(
                key = "",
                title = draft.title,
                vignette = draft.vignette,
                questions = draft.questions,
                answerKey = draft.answerKey,
                category = draft.category,
                builtin = false,
                sourceArticleId = current.articleId,
            ),
        )
        if (result is com.bioacupunt.core.util.Result.Error) {
            _feedback.value = result.kind.userMessage
            return@launch
        }
        _studyMaterialDraft.value = current.copy(clinicalCase = null)
    }

    /** Descarta o rascunho inteiro sem gravar nada — a médica não quer usar. */
    fun dismissStudyMaterial() {
        _studyMaterialDraft.value = null
    }

    /**
     * Importa um pacote curado (JSON) que a médica/curadora escolheu no armazenamento.
     * O conteúdo vem de um arquivo humano, nunca dos pesos de um modelo (R4).
     */
    fun importPackJson(json: String) = viewModelScope.launch {
        val result = runCatching {
            val pack = AppJson.decodeFromString<LibraryContentPack>(json)
            repo.stagePack(pack, now())
        }
        _feedback.value = result.fold(
            onSuccess = { o -> "Importado: ${o.stagedCount} para revisão, ${o.rejectedCount} rejeitado(s)." },
            onFailure = { "Não foi possível ler o pacote: ${it.message}" },
        )
    }

    fun clearFeedback() { _feedback.value = null }
}

class LibraryReviewViewModelFactory(
    private val repo: LibraryStagingRepository,
    private val onContentChanged: (() -> Unit)? = null,
    private val generateStudyMaterial: GenerateStudyMaterialUseCase? = null,
    private val flashcardRepository: FlashcardRepository? = null,
    private val simulatedCaseRepository: SimulatedCaseRepository? = null,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LibraryReviewViewModel(repo, onContentChanged, generateStudyMaterial, flashcardRepository, simulatedCaseRepository) as T
    }
}
