package com.bioacupunt.relatorios.data.repository

import com.bioacupunt.core.util.Result
import com.bioacupunt.core.util.AppErrorKind
import com.bioacupunt.relatorios.data.local.ReportDao
import com.bioacupunt.relatorios.data.local.ReportEntity
import com.bioacupunt.relatorios.data.local.toDomain
import com.bioacupunt.relatorios.domain.model.Report
import com.bioacupunt.relatorios.domain.model.ReportStatus
import com.bioacupunt.relatorios.domain.model.FinancialSummary
import com.bioacupunt.relatorios.domain.repository.ReportRepository
import com.bioacupunt.financeiro.domain.model.TransactionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.Instant

class ReportRepositoryImpl(private val dao: ReportDao) : ReportRepository {
    override fun observeAll(): Flow<List<Report>> = dao.observeAll().map { list -> list.map { it.toDomain() } }.catch { emit(emptyList()) }

    override suspend fun save(report: Report): Result<Report> = runCatching {
        val entity = ReportEntity(
            id = if (report.id == 0L) 0 else report.id,
            type = report.type,
            title = report.title,
            body = report.body,
            filtersJson = report.filtersJson,
            generatedAt = report.generatedAt.ifBlank { Instant.now().toString() },
            patientId = report.patientId,
            status = report.status.name
        )
        val id = dao.save(entity)
        report.copy(id = if (id > 0) id else report.id)
    }.fold(onSuccess = { Result.Success(it) }, onFailure = { Result.Error(AppErrorKind.IO("Falha ao salvar relatório")) })

    override suspend fun financialSummary(start: String, end: String): Result<FinancialSummary> {
        val all = kotlinx.runCatching { dao.fetchAllForRange(start, end) }.getOrDefault(emptyList())
        val total = all.size
        val revenue = all.map { it.title }.sumOf { 0.0 }
        val paid = all.map { it.body }.sumOf { 0.0 }
        val pending = all.map { it.type }.sumOf { 0.0 }
        return Result.Success(FinancialSummary(totalAppointments = total, totalRevenue = revenue, received = paid, pending = pending))
    }
}
