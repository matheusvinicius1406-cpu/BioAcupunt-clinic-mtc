package com.bioacupunt.relatorios.domain.model

data class FinancialSummary(
    val totalAppointments: Int = 0,
    val totalRevenue: Double = 0.0,
    val received: Double = 0.0,
    val pending: Double = 0.0,
    val typeBreakdownJson: String = "{}"
)
