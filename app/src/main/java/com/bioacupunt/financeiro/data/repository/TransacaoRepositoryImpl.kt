package com.bioacupunt.financeiro.data.repository

import com.bioacupunt.financeiro.data.local.TransacaoDao
import com.bioacupunt.financeiro.data.local.TransacaoEntity
import com.bioacupunt.financeiro.data.local.toDomain
import com.bioacupunt.financeiro.data.local.toEntity
import com.bioacupunt.financeiro.domain.model.Transacao
import com.bioacupunt.financeiro.domain.repository.TransacaoRepository
import com.bioacupunt.core.util.AppError
import com.bioacupunt.core.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class TransacaoRepositoryImpl(
    private val dao: TransacaoDao
) : TransacaoRepository {

    override fun observeAll(): Flow<List<Transacao>> {
        return dao.observeAll().map { it.map { e -> e.toDomain() } }.catch { emit(emptyList()) }
    }

    override fun observeByPatientAndRange(patientId: Long, start: String, end: String): Flow<List<Transacao>> {
        return dao.observeByPatientAndRange(patientId, start, end).map { it.map { e -> e.toDomain() } }.catch { emit(emptyList()) }
    }

    override suspend fun getById(id: Long): Result<Transacao> {
        return try {
            val entity = dao.getById(id)
            if (entity == null) Result.Error(AppError.DatabaseError())
            else Result.Success(entity.toDomain())
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e))
        }
    }

    override suspend fun save(transacao: Transacao): Result<Transacao> {
        return try {
            val now = java.time.Instant.now().toString()
            val savedId = dao.save(transacao.toEntity(now))
            val saved = transacao.copy(id = if (transacao.id == 0L) savedId else transacao.id)
            Result.Success(saved)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e))
        }
    }

    override suspend fun sumRevenue(start: String, end: String): Result<Double> {
        return try {
            val paid = dao.sumByStatusAndRange("PAGO", start, end, "PAGAMENTO")
            val refunds = dao.sumByStatusAndRange("REEMBOLSADO", start, end, "REEMBOLSO")
            Result.Success(paid - refunds)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e))
        }
    }

    override suspend fun sumPayments(start: String, end: String): Result<Double> {
        return try { Result.Success(dao.sumByStatusAndRange("PAGO", start, end, "PAGAMENTO")) } catch (e: Exception) { Result.Error(AppError.DatabaseError(e)) }
    }

    override suspend fun sumPending(start: String, end: String): Result<Double> {
        return try { Result.Success(dao.sumPending(start, end)) } catch (e: Exception) { Result.Error(AppError.DatabaseError(e)) }
    }
}
