package com.bioacupunt.relatorios.domain.usecase

import com.bioacupunt.core.util.Result
import com.bioacupunt.relatorios.domain.model.Report
import com.bioacupunt.relatorios.domain.repository.ReportRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class ObserveReports(private val repo: ReportRepository, private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    operator fun invoke(): Flow<List<Report>> = repo.observeAll()
}

class SaveReport(private val repo: ReportRepository, private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    suspend operator fun invoke(report: Report): Result<Report> = repo.save(report)
}

class RelatoriosUseCases(
    val observe: ObserveReports,
    val save: SaveReport,
    val financialSummary: FinancialSummaryCase
) {
    constructor(repository: ReportRepository, dispatcher: CoroutineDispatcher = Dispatchers.IO) : this(
        observe = ObserveReports(repository, dispatcher),
        save = SaveReport(repository, dispatcher),
        financialSummary = FinancialSummaryCase(repository, dispatcher)
    )
}
