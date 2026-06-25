package com.example.ui.screens.financeiro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import com.example.MainViewModel
import com.example.data.local.FinanceEntity
import com.example.ui.components.MetricCard
import com.example.ui.theme.*

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FinanceiroScreen(
    viewModel: MainViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val finances by viewModel.finances.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // Multi-session packages (represented dynamically as mutable state)
    var mariaPackageUsage by remember { mutableStateOf(1) }
    var joaoPackageUsage by remember { mutableStateOf(0) }

    val totalRevenue = finances.filter { it.type == "receita" }.sumOf { it.amount }
    val totalExpense = finances.filter { it.type == "despesa" }.sumOf { it.amount }
    val balance = totalRevenue - totalExpense

    if (showAddDialog) {
        var type by remember { mutableStateOf("receita") } // receita, despesa
        var description by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Sessão Individual") }
        var paymentMethod by remember { mutableStateOf("pix") }
        var errorMessage by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Registrar Novo Lançamento", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (errorMessage.isNotEmpty()) {
                        Text(errorMessage, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    }

                    Text("Tipo de Fluxo:", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { type = "receita" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "receita") Gold else BorderColor
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Receita", color = if (type == "receita") Color.White else TextPrimary)
                        }
                        Button(
                            onClick = { type = "despesa" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "despesa") Color(0xFFC62828) else BorderColor
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Despesa", color = if (type == "despesa") Color.White else TextPrimary)
                        }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrição / Título", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Valor (R$)", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Categoria", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Text("Método de Pagamento:", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val options = listOf("pix" to "PIX", "cartao_credito" to "Crédito", "dinheiro" to "Dinheiro")
                        options.forEach { (key, label) ->
                            FilterChip(
                                selected = paymentMethod == key,
                                onClick = { paymentMethod = key },
                                label = { Text(label, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (description.isBlank() || amount <= 0.0) {
                            errorMessage = "Preencha a descrição e um valor válido maior que zero!"
                        } else {
                            val newFinance = FinanceEntity(
                                id = "f_" + System.currentTimeMillis(),
                                type = type,
                                description = description,
                                amount = amount,
                                date = System.currentTimeMillis(),
                                category = category,
                                paymentMethod = paymentMethod
                            )
                            viewModel.addFinance(newFinance)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) {
                    Text("Confirmar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SwissWhite
        )
    }

    with(sharedTransitionScope) {
        Column(
            modifier = Modifier
                .sharedBounds(
                    rememberSharedContentState(key = "financeiro_container"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
                .fillMaxSize()
                .background(DarkBlue)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    "Gestão Financeira",
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Fluxo de caixa, pacotes de tratamento e livro de caixa integrado",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard("Faturamento (Receitas)", "R$ ${String.format("%.2f", totalRevenue)}", Gold)
                MetricCard("Despesas", "R$ ${String.format("%.2f", totalExpense)}", Color(0xFFC62828))
                MetricCard("Saldo Clínico", "R$ ${String.format("%.2f", balance)}", Color(0xFF2E7D32))
            }

            Text(
                "Pacotes Multissessões Ativos",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("PACOTE OURO - Maria Souza", color = Gold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Valor: R$ 1.200,00 | Consumo: $mariaPackageUsage/10", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { if (mariaPackageUsage < 10) mariaPackageUsage++ },
                            colors = ButtonDefaults.buttonColors(containerColor = Gold),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 12.dp)
                        ) {
                            Text("Registrar Presença", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("PLANO RELAX - João Santos", color = Gold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Valor: R$ 540,00 | Consumo: $joaoPackageUsage/6", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { if (joaoPackageUsage < 6) joaoPackageUsage++ },
                            colors = ButtonDefaults.buttonColors(containerColor = Gold),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 12.dp)
                        ) {
                            Text("Registrar Presença", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Livro de Caixa (Lançamentos)",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lançar Caixa", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(finances) { trans ->
                        val isReceita = trans.type == "receita"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SwissWhite, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (isReceita) SwissGreenLight else Color(0xFFFFEBEE),
                                            RoundedCornerShape(18.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isReceita) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown,
                                        contentDescription = null,
                                        tint = if (isReceita) Gold else Color(0xFFC62828),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = trans.description,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "${trans.category} • ${trans.paymentMethod.uppercase()}",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${if (isReceita) "+" else "-"} R$ ${String.format("%.2f", trans.amount)}",
                                    color = if (isReceita) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                IconButton(
                                    onClick = { viewModel.deleteFinance(trans.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Excluir lançamento",
                                        tint = Color.Red.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
