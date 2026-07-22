package com.bioacupunt.financeiro.domain.model

enum class TransactionType(val label: String) {
    PAYMENT("PAGAMENTO"),
    REFUND("REEMBOLSO"),
    ADJUSTMENT("AJUSTE")
}

enum class TransactionStatus(val label: String) {
    PAID("PAGO"),
    PENDING("PENDENTE"),
    CANCELLED("CANCELADO"),
    REFUNDED("REEMBOLSADO")
}

data class Transacao(
    val id: Long,
    val tenantId: Long = 0L,
    val patientId: Long?,
    val appointmentId: Long?,
    val amountBrl: Double,
    val date: String,
    val type: String,
    val method: String,
    val category: String,
    val status: String,
    val notes: String,
    val createdAt: String
)
