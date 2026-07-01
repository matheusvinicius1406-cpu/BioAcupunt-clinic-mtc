package com.bioacupunt.prontuario.domain.repository

import com.bioacupunt.core.util.Result
import com.bioacupunt.prontuario.domain.model.Prontuario
import com.bioacupunt.prontuario.domain.model.ProntuarioEntry
import kotlinx.coroutines.flow.Flow

interface ProntuarioRepository {
    fun observeProntuario(patientId: Long): Flow<Prontuario?>
    suspend fun save(prontuario: Prontuario): Result<Prontuario>
    fun observeEntries(patientId: Long): Flow<List<ProntuarioEntry>>
    suspend fun addEntry(entry: ProntuarioEntry): Result<ProntuarioEntry>
    suspend fun updateEntry(entry: ProntuarioEntry): Result<ProntuarioEntry>
    suspend fun deleteEntry(id: Long): Result<Boolean>
    suspend fun getPendingSync(since: String): Result<List<ProntuarioEntry>>
}
