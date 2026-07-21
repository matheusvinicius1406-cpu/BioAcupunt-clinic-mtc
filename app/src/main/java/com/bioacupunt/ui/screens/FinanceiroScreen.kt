package com.bioacupunt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.di.AppContainer
import com.bioacupunt.financeiro.domain.model.TransactionStatus
import java.util.Locale

/**
 * FINANCEIRO — tela própria, seguindo block_financeiro.html: grade 2x2 de
 * estatísticas, Lançamentos e Receita por procedimento. Tudo calculado ao vivo
 * das transações reais; sem números de exemplo. ("Pacotes de sessões" do mockup
 * fica de fora: não existe modelo de pacote no app ainda.)
 */
@Composable
fun FinanceiroScreen(
    vm: com.bioacupunt.financeiro.presentation.FinanceiroViewModel = viewModel(factory = AppContainer.financeiroViewModelFactory),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val brl = { v: Double -> java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(v) }
    val cs = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(cs.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Financeiro", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold))
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FinStat(Modifier.weight(1f), "Recebido/mês", brl(state.monthReceivedBrl))
                    FinStat(Modifier.weight(1f), "Pendente/mês", brl(state.monthPendingBrl))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FinStat(Modifier.weight(1f), "Consultas pagas", "${state.paidCount}")
                    FinStat(Modifier.weight(1f), "Ticket médio", brl(state.ticketMedioBrl))
                }
            }
        }

        item {
            FinCard {
                Text("Lançamentos", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(4.dp))
                if (state.recentTransactions.isEmpty()) {
                    Text("Sem lançamentos ainda.", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    state.recentTransactions.forEachIndexed { i, t ->
                        val isPaid = t.status == TransactionStatus.PAID.name
                        val color = if (isPaid) cs.primary else MaterialTheme.colorScheme.error
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(if (isPaid) Icons.Default.CheckCircle else Icons.Default.Schedule, null, tint = color, modifier = Modifier.size(18.dp))
                                Column {
                                    Text(t.category, style = MaterialTheme.typography.bodySmall)
                                    Text(t.method, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                                }
                            }
                            Text(brl(t.amountBrl), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = color)
                        }
                        if (i != state.recentTransactions.lastIndex) HorizontalDivider(color = cs.outline)
                    }
                }
            }
        }

        item {
            FinCard {
                Text("Receita por procedimento", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(10.dp))
                if (state.revenueByCategory.isEmpty()) {
                    Text("Sem receita registrada neste mês.", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                } else {
                    state.revenueByCategory.forEach { r ->
                        Column(modifier = Modifier.padding(bottom = 10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(r.category, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                                Text(brl(r.amountBrl), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                            }
                            Spacer(Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(20.dp)).background(cs.background)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(r.fraction.coerceIn(0f, 1f))
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Brush.horizontalGradient(listOf(cs.primary, cs.secondary)))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinStat(modifier: Modifier, label: String, value: String) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(cs.surface)
            .border(1.dp, cs.outline, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = cs.onSurface)
    }
}

@Composable
private fun FinCard(content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(cs.surface)
            .border(1.dp, cs.outline, RoundedCornerShape(22.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        content = content,
    )
}
