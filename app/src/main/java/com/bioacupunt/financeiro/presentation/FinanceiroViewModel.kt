package com.bioacupunt.financeiro.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.financeiro.domain.model.Transacao
import com.bioacupunt.financeiro.domain.model.TransactionStatus
import com.bioacupunt.financeiro.domain.usecase.ObserveTransactions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class RevenueByCategory(val category: String, val amountBrl: Double, val fraction: Float)

data class FinanceiroUiState(
    val monthReceivedBrl: Double = 0.0,
    val monthPendingBrl: Double = 0.0,
    val paidCount: Int = 0,
    val ticketMedioBrl: Double = 0.0,
    val recentTransactions: List<Transacao> = emptyList(),
    val revenueByCategory: List<RevenueByCategory> = emptyList(),
)

class FinanceiroViewModel(
    observeTransactions: ObserveTransactions,
    private val today: LocalDate = LocalDate.now(),
) : ViewModel() {

    private val _state = MutableStateFlow(FinanceiroUiState())
    val state: StateFlow<FinanceiroUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeTransactions() // tenantId será extraído do TenantManager quando necessário
                .catch { emit(emptyList()) }
                .collect { all ->
                    val monthKey = today.toString().take(7) // yyyy-MM
                    val thisMonth = all.filter { it.date.take(7) == monthKey }
                    val paid = thisMonth.filter { it.status == TransactionStatus.PAID.name }
                    val pending = thisMonth.filter { it.status == TransactionStatus.PENDING.name }
                    val received = paid.sumOf { it.amountBrl }
                    val pendingSum = pending.sumOf { it.amountBrl }
                    val byCategory = paid.groupBy { it.category }
                        .mapValues { (_, list) -> list.sumOf { it.amountBrl } }
                        .toList()
                        .sortedByDescending { it.second }
                    val total = byCategory.sumOf { it.second }.takeIf { it > 0 } ?: 1.0

                    _state.update {
                        it.copy(
                            monthReceivedBrl = received,
                            monthPendingBrl = pendingSum,
                            paidCount = paid.size,
                            ticketMedioBrl = if (paid.isNotEmpty()) received / paid.size else 0.0,
                            recentTransactions = all.take(10),
                            revenueByCategory = byCategory.map { (cat, amount) ->
                                RevenueByCategory(cat, amount, (amount / total).toFloat())
                            },
                        )
                    }
                }
        }
    }
}

class FinanceiroViewModelFactory(private val observeTransactions: ObserveTransactions) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return FinanceiroViewModel(observeTransactions) as T
    }
}
