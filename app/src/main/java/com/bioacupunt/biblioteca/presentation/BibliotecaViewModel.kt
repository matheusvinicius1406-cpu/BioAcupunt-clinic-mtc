package com.bioacupunt.biblioteca.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.biblioteca.domain.model.BibliotecaNode
import com.bioacupunt.biblioteca.domain.usecase.ObserveBiblioteca
import com.bioacupunt.biblioteca.domain.usecase.SearchBiblioteca
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class BibliotecaUiState(
    val query: String = "",
    val results: List<BibliotecaNode> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class BibliotecaViewModelFactory(
    private val observe: ObserveBiblioteca,
    private val search: SearchBiblioteca
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return BibliotecaViewModel(observe, search) as T
    }
}

class BibliotecaViewModel(
    private val observe: ObserveBiblioteca,
    private val search: SearchBiblioteca
) : ViewModel() {

    private val _state = MutableStateFlow(BibliotecaUiState())
    val state: StateFlow<BibliotecaUiState> = _state.asStateFlow()

    init {
        observeAll()
    }

    fun onQueryChanged(query: String) {
        _state.update { it.copy(query = query) }
        if (query.isBlank()) {
            observeAll()
        } else {
            search(query)
        }
    }

    private fun observeAll() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            observe()
                .catch { e -> _state.update { it.copy(loading = false, error = it.error ?: e.localizedMessage.orEmpty()) } }
                .collect { list ->
                    val current = _state.value
                    val filtered = if (current.query.isBlank()) list else filterQuery(list, current.query)
                    _state.update { it.copy(results = filtered, loading = false, error = null) }
                }
        }
    }

    private fun search(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            search(query)
                .catch { e -> _state.update { it.copy(loading = false, error = it.error ?: e.localizedMessage.orEmpty()) } }
                .collect { list ->
                    _state.update { it.copy(results = list, loading = false, error = null) }
                }
        }
    }

    private fun filterQuery(items: List<BibliotecaNode>, query: String): List<BibliotecaNode> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return items
        return items.filter { it.title.lowercase().contains(q) || it.content.lowercase().contains(q) || it.tags.any { t -> t.lowercase().contains(q) } }
    }
}
