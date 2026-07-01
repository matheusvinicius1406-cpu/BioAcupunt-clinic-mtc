package com.bioacupunt.biblioteca.domain.usecase

import com.bioacupunt.biblioteca.domain.model.BibliotecaNode
import com.bioacupunt.biblioteca.domain.repository.BibliotecaRepository
import com.bioacupunt.core.util.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class ObserveBiblioteca(private val repo: BibliotecaRepository, private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    operator fun invoke(): Flow<List<BibliotecaNode>> = repo.observeAll()
}

class SearchBiblioteca(private val repo: BibliotecaRepository, private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    operator fun invoke(query: String): Flow<List<BibliotecaNode>> = repo.search(query)
}

class BibliotecaUseCases(
    val observe: ObserveBiblioteca,
    val search: SearchBiblioteca
) {
    constructor(repository: BibliotecaRepository, dispatcher: CoroutineDispatcher = Dispatchers.IO) : this(
        observe = ObserveBiblioteca(repository, dispatcher),
        search = SearchBiblioteca(repository, dispatcher)
    )
}
