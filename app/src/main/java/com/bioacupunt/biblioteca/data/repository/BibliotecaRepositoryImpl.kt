package com.bioacupunt.biblioteca.data.repository

import com.bioacupunt.biblioteca.data.local.BibliotecaDao
import com.bioacupunt.biblioteca.data.local.BibliotecaNodeEntity
import com.bioacupunt.biblioteca.data.local.toDomain
import com.bioacupunt.biblioteca.domain.model.BibliotecaNode
import com.bioacupunt.biblioteca.domain.repository.BibliotecaRepository
import com.bioacupunt.core.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class BibliotecaRepositoryImpl(private val dao: BibliotecaDao) : BibliotecaRepository {
    override fun observeAll(): Flow<List<BibliotecaNode>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override fun search(query: String): Flow<List<BibliotecaNode>> = dao.search(query).map { list -> list.map { it.toDomain() } }
    override suspend fun syncFromRemote(): Result<Int> {
        // TODO: Configurar produção - sync remota placeholder.
        return Result.Success(0)
    }
}
