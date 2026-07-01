package com.bioacupunt.relatorios.domain.model

enum class ReportStatus {
    DRAFT,
    READY,
    ARCHIVED
}

data class Report(
    val id: Long = 0,
    val type: String,
    val title: String,
    val body: String = "",
    val filtersJson: String = "{}",
    val generatedAt: String = "",
    val patientId: Long? = null,
    val status: ReportStatus = ReportStatus.DRAFT
)

data class FinancialSummary(
    val totalAppointments: Int = 0,
    val totalRevenue: Double = 0.0,
    val received: Double = 0.0,
    val pending: Double = 0.0,
    val typeBreakdown: Map<String, Double> = emptyMap()
)
