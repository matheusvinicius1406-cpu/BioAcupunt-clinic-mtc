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
    /**
     * Achado de auditoria (2026-07-29): a médica digitava o nome do paciente no
     * diálogo "Gerar relatório" e o valor era descartado — nunca chegava aqui.
     * Texto livre, não resolvido pra um `patientId` real: um match automático por
     * nome poderia ligar o relatório ao paciente errado (nome duplicado, typo), o
     * que é pior que não linkar nenhum.
     */
    val patientName: String = "",
    val status: ReportStatus = ReportStatus.DRAFT
)

data class FinancialSummary(
    val totalAppointments: Int = 0,
    val totalRevenue: Double = 0.0,
    val received: Double = 0.0,
    val pending: Double = 0.0,
    val typeBreakdown: Map<String, Double> = emptyMap()
)
