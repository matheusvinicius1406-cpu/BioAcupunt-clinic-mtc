package com.bioacupunt.relatorios.domain.repository

import com.bioacupunt.core.util.Result
import com.bioacupunt.relatorios.domain.model.Report
import kotlinx.coroutines.flow.Flow

interface ReportRepository {
    fun observeAll(): Flow<List<Report>>
    suspend fun save(report: Report): Result<Report>
    suspend fun financialSummary(start: String, end: String): Result<com.bioacupunt.relatorios.domain.model.FinancialSummary>
}
