package com.bioacupunt.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.di.AppContainer
import com.bioacupunt.ui.theme.Primary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class ReportTemplate(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val aiPowered: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatoriosScreen(
    vm: com.bioacupunt.relatorios.presentation.RelatoriosViewModel = viewModel(factory = AppContainer.relatoriosViewModelFactory)
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Modelos", "Gerados", "Financeiro")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
        selectedTabIndex = selectedTab,
        modifier = Modifier.premiumShadow(Color.Transparent, MaterialTheme.shapes.extraLarge, 0.dp)
    ) {
            tabs.forEachIndexed { i, t ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(t) })
            }
        }
        when (selectedTab) {
            0 -> ReportTemplatesTab(onGenerate = { report ->
                vm.generate(report)
            })
            else -> {
                val reports = state.reports
                if (selectedTab == 1) {
                    GeneratedReportsTab(reports = reports)
                } else {
                    FinancialReportTab()
                }
            }
        }
    }
}

@Composable
private fun ReportTemplatesTab(onGenerate: (com.bioacupunt.relatorios.domain.model.Report) -> Unit) {
    val templates = listOf(
        ReportTemplate("evo", "Nota de Evolução", "Registro de cada sessão clínica com pontos, observações e plano", Icons.Default.EditNote, Primary, aiPowered = true),
        ReportTemplate("first", "Avaliação Inicial MTC", "Anamnese completa, diagnóstico energético e plano terapêutico", Icons.Default.AssignmentInd, Color(0xFF64B5F6), aiPowered = true),
        ReportTemplate("discharge", "Relatório de Alta", "Síntese do tratamento, resultados e orientações pós-alta", Icons.Default.TaskAlt, Color(0xFF81C784), aiPowered = true),
        ReportTemplate("monthly", "Relatório Mensal Clínico", "Resumo mensal de atendimentos e evolução dos casos", Icons.Default.CalendarViewMonth, Color(0xFFFFB300)),
        ReportTemplate("financial", "Relatório Financeiro", "Receitas, pagamentos pendentes e análise por período", Icons.Default.AccountBalance, Color(0xFF9575CD)),
        ReportTemplate("referral", "Encaminhamento Médico", "Carta de encaminhamento para outros profissionais", Icons.Default.LocalHospital, Color(0xFFFF8A65), aiPowered = true),
        ReportTemplate("consent", "Termo de Consentimento", "TCLE para acupuntura e técnicas de MTC", Icons.Default.Gavel, Color(0xFF78909C)),
        ReportTemplate("anamnese", "Ficha de Anamnese", "Formulário de histórico completo do paciente", Icons.Default.Description, Color(0xFF4CAF50))
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Selecione um modelo para gerar com IA ou preencher manualmente",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(templates) { tpl -> 
            ReportTemplateCard(tpl, onGenerate = onGenerate) 
        }
    }
}

@Composable
private fun ReportTemplateCard(tpl: ReportTemplate, onGenerate: (com.bioacupunt.relatorios.domain.model.Report) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(tpl.color.copy(alpha = 0.12f), androidx.compose.foundation.shape.RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(tpl.icon, null, tint = tpl.color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tpl.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    if (tpl.aiPowered) {
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                            color = Primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                "IA",
                                style = MaterialTheme.typography.labelSmall.copy(color = Primary),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(tpl.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outlineVariant)
        }
    }

    if (showDialog) {
        var patientName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            icon = { Icon(tpl.icon, null, tint = tpl.color) },
            title = { Text(tpl.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Paciente:", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = patientName, onValueChange = { patientName = it },
                        placeholder = { Text("Nome do paciente") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    if (tpl.aiPowered) {
                        Surface(
                            color = Primary.copy(alpha = 0.06f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, null, tint = Primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Este relatório será gerado com apoio de IA.", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val report = com.bioacupunt.relatorios.domain.model.Report(
                            type = tpl.id,
                            title = tpl.title,
                            generatedAt = java.time.Instant.now().toString(),
                            status = com.bioacupunt.relatorios.domain.model.ReportStatus.READY
                        )
                        onGenerate(report)
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tpl.color)
                ) { Text("Gerar") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun GeneratedReportsTab(reports: List<com.bioacupunt.relatorios.domain.model.Report>) {
    val today = java.time.LocalDate.now()
    val fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy", java.util.Locale("pt", "BR"))
    val items = if (reports.isEmpty()) {
        listOf(Triple("Nenhum relatório gerado.", today.toString(), Icons.Default.Info))
    } else {
        reports.map { r -> Triple(r.title, r.generatedAt.take(10), Icons.Default.EditNote) }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { (title, date, icon) ->
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = Primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                        Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row {
                        IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Download, null, tint = Primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinancialReportTab() {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Resumo — Junho 2026", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(12.dp))
                    FinRow("Total de consultas", "42")
                    FinRow("Valor total gerado", "R$ 6.480,00", Color(0xFF4CAF50))
                    FinRow("Valores recebidos", "R$ 4.800,00", Color(0xFF64B5F6))
                    FinRow("Valores pendentes", "R$ 1.680,00", Color(0xFFFF8A65))
                    FinRow("Ticket médio", "R$ 154,28", Primary)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    FinRow("Taxa de inadimplência", "25,9%", Color(0xFFFF8A65))
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Por Tipo de Atendimento", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "Acupuntura" to "R$ 2.850,00",
                        "1ª Consulta" to "R$ 1.750,00",
                        "Retornos" to "R$ 1.200,00",
                        "Moxibustão" to "R$ 480,00",
                        "Outros" to "R$ 200,00"
                    ).forEach { (t, v) -> FinRow(t, v) }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Exportar PDF")
                }
                Button(onClick = {}, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Compartilhar")
                }
            }
        }
    }
}

@Composable
private fun FinRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(color = color, fontWeight = FontWeight.SemiBold))
    }
}
