package com.bioacupunt.financeiro.domain.repository

import com.bioacupunt.financeiro.domain.model.Transacao
import com.bioacupunt.core.util.Result
import kotlinx.coroutines.flow.Flow

interface TransacaoRepository {
    fun observeAll(): Flow<List<Transacao>>
    fun observeByPatientAndRange(patientId: Long, start: String, end: String): Flow<List<Transacao>>
    suspend fun getById(id: Long): Result<Transacao>
    suspend fun save(transacao: Transacao): Result<Transacao>
    suspend fun sumRevenue(start: String, end: String): Result<Double>
    suspend fun sumPayments(start: String, end: String): Result<Double>
    suspend fun sumPending(start: String, end: String): Result<Double>
}
