package com.bioacupunt.educacao.domain.repository

import com.bioacupunt.core.util.Result
import com.bioacupunt.educacao.domain.model.SimulatedCase
import kotlinx.coroutines.flow.Flow

interface SimulatedCaseRepository {
    /** União do caso fixo + casos aprovados pela médica. */
    fun observeAll(): Flow<List<SimulatedCase>>

    /** Recusa [SimulatedCase.builtin] = true — o caso fixo é read-only. */
    suspend fun save(case: SimulatedCase): Result<SimulatedCase>
}
