package com.bioacupunt.financeiro.domain.model

enum class TransactionType(val name: String) {
    PAYMENT("PAGAMENTO"),
    REFUND("REEMBOLSO"),
    ADJUSTMENT("AJUSTE")
}

enum class TransactionStatus(val name: String) {
    PAID("PAGO"),
    PENDING("PENDENTE"),
    CANCELLED("CANCELADO"),
    REFUNDED("REEMBOLSADO")
}

data class Transacao(
    val id: Long,
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
