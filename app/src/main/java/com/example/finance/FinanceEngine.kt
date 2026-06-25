package com.example.finance

import com.example.core.Money
import com.example.core.PatientId
import java.util.UUID

enum class PaymentStatus {
    PENDING, PAID, OVERDUE, REFUNDED, CANCELED
}

enum class TransactionCategory {
    CLINICAL_SESSION, HERBAL_PRODUCT, CLINIC_RENT, MARKETING, SALARY, TAX, OTHER
}

data class Invoice(
    val id: String = UUID.randomUUID().toString(),
    val patientId: PatientId,
    val amount: Money,
    val dueDate: Long,
    val status: PaymentStatus,
    val issuedAt: Long = System.currentTimeMillis()
)

data class FinancialTransaction(
    val id: String = UUID.randomUUID().toString(),
    val amount: Money,
    val type: String, // "RECEIPT" (receita), "EXPENSE" (despesa)
    val category: TransactionCategory,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ProfessionalCommission(
    val id: String = UUID.randomUUID().toString(),
    val professionalId: String,
    val invoiceId: String,
    val rate: Double, // e.g. 0.40 for 40%
    val amount: Money,
    val status: String // "PENDING", "PAID"
)

// Finance Aggregate Root
data class FinanceAggregate(
    val invoices: List<Invoice> = emptyList(),
    val transactions: List<FinancialTransaction> = emptyList(),
    val commissions: List<ProfessionalCommission> = emptyList()
) {
    // 1. Receitas
    fun getTotalReceipts(): Money {
        return transactions
            .filter { it.type == "RECEIPT" }
            .fold(Money(0.0)) { acc, t -> acc + t.amount }
    }

    // 2. Despesas
    fun getTotalExpenses(): Money {
        return transactions
            .filter { it.type == "EXPENSE" }
            .fold(Money(0.0)) { acc, t -> acc + t.amount }
    }

    // 3. Fluxo de Caixa (Net Balance)
    fun getNetCashFlow(): Money {
        val receipts = getTotalReceipts()
        val expenses = getTotalExpenses()
        return Money(receipts.amount - expenses.amount, receipts.currency)
    }

    // 4. Commissions Engine
    fun calculateAndAccrueCommission(
        professionalId: String,
        invoice: Invoice,
        rate: Double
    ): FinanceAggregate {
        require(invoice.status == PaymentStatus.PAID) { "Commissions can only be accrued for paid invoices" }
        val commissionAmount = Money(invoice.amount.amount * rate, invoice.amount.currency)
        val commission = ProfessionalCommission(
            professionalId = professionalId,
            invoiceId = invoice.id,
            rate = rate,
            amount = commissionAmount,
            status = "PENDING"
        )
        return this.copy(commissions = commissions + commission)
    }

    // 5. Forecast & KPIs
    fun getFinancialForecast(growthRate: Double): Money {
        // Simple linear forecast based on current receipts
        val current = getTotalReceipts().amount
        val forecastAmount = current * (1.0 + growthRate)
        return Money(forecastAmount, "BRL")
    }

    // 6. DRE (Demonstrativo do Resultado do Exercício)
    fun getDREStatement(): DREStatement {
        val totalRevenue = getTotalReceipts().amount
        val cogs = commissions.fold(0.0) { acc, c -> acc + c.amount.amount } // Cost of Goods/Services (Commissions)
        val grossProfit = totalRevenue - cogs
        val operatingExpenses = transactions
            .filter { it.type == "EXPENSE" && it.category != TransactionCategory.SALARY }
            .sumOf { it.amount.amount }
        val netOperatingIncome = grossProfit - operatingExpenses
        
        return DREStatement(
            grossRevenue = totalRevenue,
            commissionsCost = cogs,
            grossProfit = grossProfit,
            operatingExpenses = operatingExpenses,
            netOperatingIncome = netOperatingIncome
        )
    }
}

data class DREStatement(
    val grossRevenue: Double,
    val commissionsCost: Double,
    val grossProfit: Double,
    val operatingExpenses: Double,
    val netOperatingIncome: Double
)
