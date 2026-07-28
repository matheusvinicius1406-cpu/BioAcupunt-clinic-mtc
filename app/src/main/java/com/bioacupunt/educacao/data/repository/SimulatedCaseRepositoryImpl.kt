package com.bioacupunt.educacao.data.repository

import com.bioacupunt.core.util.AppError
import com.bioacupunt.core.util.Result
import com.bioacupunt.educacao.data.BuiltinSimulatedCases
import com.bioacupunt.educacao.data.local.SimulatedCaseDao
import com.bioacupunt.educacao.data.local.toDomain
import com.bioacupunt.educacao.data.local.toEntity
import com.bioacupunt.educacao.domain.model.SimulatedCase
import com.bioacupunt.educacao.domain.repository.SimulatedCaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * [tenantId] é lambda, não [com.bioacupunt.core.multitenancy.TenantManager] injetado direto —
 * mesmo raciocínio de [com.bioacupunt.educacao.data.repository.FlashcardRepositoryImpl]:
 * `TenantManager` não instancia em teste JVM puro sem Robolectric.
 */
class SimulatedCaseRepositoryImpl(
    private val dao: SimulatedCaseDao,
    private val tenantId: () -> Long,
) : SimulatedCaseRepository {

    override fun observeAll(): Flow<List<SimulatedCase>> =
        dao.observeCases(tenantId())
            .map { list -> BuiltinSimulatedCases.cases + list.map { it.toDomain() } }
            .catch { emit(BuiltinSimulatedCases.cases) }

    override suspend fun save(case: SimulatedCase): Result<SimulatedCase> {
        if (case.builtin) {
            return Result.Error(AppError.ValidationError("O caso fixo não pode ser editado."))
        }
        return try {
            val tid = tenantId()
            val now = java.time.Instant.now().toString()
            val savedId = dao.saveCase(case.toEntity(tid, now))
            val finalId = case.userRowId ?: savedId
            Result.Success(case.copy(userRowId = finalId, key = "user_$finalId"))
        } catch (e: Exception) {
            Result.Error(AppError.from(e))
        }
    }
}
