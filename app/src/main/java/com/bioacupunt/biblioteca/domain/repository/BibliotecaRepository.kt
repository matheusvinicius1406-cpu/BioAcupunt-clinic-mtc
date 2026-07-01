package com.bioacupunt.biblioteca.domain.repository

import com.bioacupunt.biblioteca.domain.model.BibliotecaNode
import kotlinx.coroutines.flow.Flow

interface BibliotecaRepository {
    fun observeAll(): Flow<List<BibliotecaNode>>
    suspend fun syncFromRemote(): Result<Int>
    fun search(query: String): Flow<List<BibliotecaNode>>
}
