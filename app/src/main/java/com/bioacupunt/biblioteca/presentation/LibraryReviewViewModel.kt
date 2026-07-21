package com.bioacupunt.biblioteca.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.biblioteca.data.repository.LibraryStagingRepository
import com.bioacupunt.biblioteca.domain.ingestion.LibraryContentPack
import com.bioacupunt.core.util.AppJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) : ViewModel() {

    val pending: StateFlow<List<LibraryStagingRepository.StagedArticle>> =
        repo.observePending().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    private fun now() = System.currentTimeMillis()

    fun approve(id: String) = viewModelScope.launch { repo.approve(id, now()) }
    fun reject(id: String) = viewModelScope.launch { repo.reject(id, now()) }

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
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LibraryReviewViewModel(repo) as T
    }
}
