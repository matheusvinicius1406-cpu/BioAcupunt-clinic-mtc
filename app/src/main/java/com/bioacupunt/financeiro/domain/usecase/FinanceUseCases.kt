package com.bioacupunt.financeiro.domain.usecase

import com.bioacupunt.financeiro.domain.model.Transacao
import com.bioacupunt.financeiro.domain.repository.TransacaoRepository
import com.bioacupunt.core.util.Result
import kotlinx.coroutines.flow.Flow

class ObserveTransactions(
    private val repository: TransacaoRepository
) {
    operator fun invoke(): Flow<List<Transacao>> = repository.observeAll()
}

class ObservePatientTransactions(
    private val repository: TransacaoRepository
) {
    operator fun invoke(patientId: Long, start: String, end: String): Flow<List<Transacao>> {
        return repository.observeByPatientAndRange(patientId, start, end)
    }
}

class SaveTransaction(
    private val repository: TransacaoRepository
) {
    suspend operator fun invoke(transacao: Transacao): Result<Transacao> {
        return repository.save(transacao)
    }
}

class GetFinancialSummary(
    private val repository: TransacaoRepository
) {
    suspend operator fun invoke(start: String, end: String): Result<Map<String, Double>> {
        val revenue = repository.sumRevenue(start, end)
        val payments = repository.sumPayments(start, end)
        if (revenue is Result.Error) return revenue
        if (payments is Result.Error) return payments
        return Result.Success(
            mapOf(
                "revenue" to (revenue as Result.Success).data,
                "payments" to (payments as Result.Success).data
            )
        )
    }
}
