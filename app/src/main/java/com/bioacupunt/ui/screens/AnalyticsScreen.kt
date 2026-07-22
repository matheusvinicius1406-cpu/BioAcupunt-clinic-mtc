package com.bioacupunt.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.bioacupunt.ui.theme.Primary

@Composable
fun AnalyticsScreen(onBack: (() -> Unit)? = null) {
    var selectedPeriod by remember { mutableIntStateOf(1) } // 0=7d, 1=30d, 2=90d
    val periods = listOf("7 dias", "30 dias", "90 dias")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        if (onBack != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                    Text("Analytics", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
        // Period selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                periods.forEachIndexed { i, p ->
                    FilterChip(
                        selected = selectedPeriod == i,
                        onClick = { selectedPeriod = i },
                        label = { Text(p) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // KPI cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(Modifier.weight(1f), "Receita", "R$ 6.4k", "+12%", true, Color(0xFF4CAF50))
                KpiCard(Modifier.weight(1f), "Consultas", "42", "+8%", true, Color(0xFF64B5F6))
                KpiCard(Modifier.weight(1f), "Novos", "7", "-2%", false, Color(0xFFFF8A65))
            }
        }

        item { Spacer(Modifier.height(8.dp)) }

        // Revenue bar chart
        item {
            AnalyticsCard("Receita Mensal (R$)", Icons.Default.BarChart) {
                SimpleBarChart(
                    data = listOf(
                        "Jan" to 3200f, "Fev" to 2800f, "Mar" to 4100f,
                        "Abr" to 3700f, "Mai" to 4500f, "Jun" to 3900f
                    ),
                    maxValue = 5000f,
                    color = Primary
                )
            }
        }

        // Sessions per type
        item {
            AnalyticsCard("Sessões por Tipo", Icons.Default.PieChart) {
                val types = listOf(
                    "Acupuntura" to 45f to Primary,
                    "Retorno" to 25f to Color(0xFF64B5F6),
                    "1ª Consulta" to 15f to Color(0xFF81C784),
                    "Moxibustão" to 10f to Color(0xFFFFB300),
                    "Outros" to 5f to Color(0xFFFF8A65)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { (pair, color) ->
                        val (label, pct) = pair
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
                            Spacer(Modifier.width(8.dp))
                            Text(label, modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(
                                progress = { pct / 100f },
                                modifier = Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)),
                                color = color
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${pct.toInt()}%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        // Top syndromes
        item {
            AnalyticsCard("Top Síndromes Diagnosticadas", Icons.Default.Analytics) {
                val syndromes = listOf(
                    "Estagnação de Qi do Fígado" to 18,
                    "Deficiência de Yin do Rim" to 12,
                    "Deficiência de Qi do Baço" to 10,
                    "Hiperatividade de Yang do Fígado" to 8,
                    "Deficiência de Sangue" to 6
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    syndromes.forEachIndexed { i, (name, count) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${i+1}º", style = MaterialTheme.typography.labelLarge.copy(color = Primary, fontWeight = FontWeight.Bold), modifier = Modifier.width(24.dp))
                            Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            Badge(containerColor = Primary.copy(alpha = 0.2f)) {
                                Text("$count", style = MaterialTheme.typography.labelSmall.copy(color = Primary))
                            }
                        }
                    }
                }
            }
        }

        // Patient retention funnel
        item {
            AnalyticsCard("Funil de Retenção", Icons.Default.FilterAlt) {
                val funnel = listOf(
                    "1ª Consulta" to 1f to Color(0xFF64B5F6),
                    "Retornou" to 0.78f to Primary,
                    "5+ sessões" to 0.55f to Color(0xFF81C784),
                    "10+ sessões" to 0.32f to Color(0xFFFFB300),
                    "Fidelizado" to 0.18f to Color(0xFF4CAF50)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    funnel.forEach { (pair, color) ->
                        val (label, pct) = pair
                        Column {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(label, style = MaterialTheme.typography.bodySmall)
                                Text("${(pct * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = color))
                            }
                            Spacer(Modifier.height(2.dp))
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier.fillMaxWidth(pct).height(14.dp).clip(RoundedCornerShape(7.dp)),
                                color = color
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(modifier: Modifier, title: String, value: String, change: String, positive: Boolean, color: Color) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = color))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (positive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    null, tint = if (positive) Color(0xFF4CAF50) else Color(0xFFEF5350), modifier = Modifier.size(14.dp)
                )
                Text(change, style = MaterialTheme.typography.labelSmall.copy(color = if (positive) Color(0xFF4CAF50) else Color(0xFFEF5350)))
            }
        }
    }
}

@Composable
private fun AnalyticsCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = Primary, modifier = Modifier.size(18.dp))
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SimpleBarChart(data: List<Pair<String, Float>>, maxValue: Float, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { (label, value) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Text("${(value/1000).toInt()}k", style = MaterialTheme.typography.labelSmall.copy(color = color))
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .fillMaxHeight(value / maxValue)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(color.copy(alpha = 0.8f))
                )
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
