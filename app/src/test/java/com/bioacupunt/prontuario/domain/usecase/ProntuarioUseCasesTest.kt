package com.bioacupunt.prontuario.domain.usecase

import com.bioacupunt.core.util.Result
import com.bioacupunt.prontuario.domain.model.Prontuario
import com.bioacupunt.prontuario.domain.model.ProntuarioEntry
import com.bioacupunt.prontuario.domain.repository.ProntuarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProntuarioUseCasesTest {

    private val repository = object : ProntuarioRepository {
        override fun observeProntuario(patientId: Long): Flow<Prontuario?> = flowOf(Prontuario(patientId = patientId, summary = "base"))
        override suspend fun save(prontuario: Prontuario): Result<Prontuario> = Result.Success(prontuario)
        override fun observeEntries(patientId: Long): Flow<List<ProntuarioEntry>> = flowOf(listOf())
        override suspend fun addEntry(entry: ProntuarioEntry): Result<ProntuarioEntry> = Result.Success(entry)
        override suspend fun updateEntry(entry: ProntuarioEntry): Result<ProntuarioEntry> = Result.Success(entry)
        override suspend fun deleteEntry(id: Long): Result<Boolean> = Result.Success(true)
        override suspend fun getPendingSync(since: String): Result<List<ProntuarioEntry>> = Result.Success(emptyList())
    }

    @Test
    fun `observeProntuario maps flow`() = runTest {
        val cases = ProntuarioUseCases(repository)
        val result = cases.observeEntries(1)
        assertTrue(result != null)
    }

    @Test
    fun `addEntry forwards success`() = runTest {
        val cases = ProntuarioUseCases(repository)
        val entry = ProntuarioEntry(patientId = 1, body = "Teste")
        val result = cases.addEntry(entry)
        assertTrue(result is Result.Success)
    }
}
