package com.bioacupunt.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.di.AppContainer
import com.bioacupunt.ui.theme.CatAmber
import com.bioacupunt.ui.theme.CatBlue
import com.bioacupunt.ui.theme.CatBlueGrey
import com.bioacupunt.ui.theme.CatGreen
import com.bioacupunt.ui.theme.CatOrange
import com.bioacupunt.ui.theme.CatPurple
import com.bioacupunt.ui.design.SupremoCard
import com.bioacupunt.ui.design.SupremoClickableCard
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.statusColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class ReportTemplate(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    /** Templates clínicos exigem nome de paciente; financeiro/mensal/consentimento, não. */
    val needsPatient: Boolean = false
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
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            tabs.forEachIndexed { i, t ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(t) })
            }
        }
        when (selectedTab) {
            0 -> ReportTemplatesTab(
                onGenerate = { type, title, patientName ->
                    vm.generate(type, title, patientName)
                },
                generating = state.generating,
                generateError = state.generateError,
                onDismissError = vm::clearGenerateError,
            )
            else -> {
                val reports = state.reports
                if (selectedTab == 1) {
                    GeneratedReportsTab(reports = reports, lastGenerated = state.lastGenerated, onDismissLast = vm::clearLastGenerated)
                } else {
                    FinancialReportTab()
                }
            }
        }
    }
}

@Composable
private fun ReportTemplatesTab(
    onGenerate: (String, String, String) -> Unit,
    generating: Boolean,
    generateError: String?,
    onDismissError: () -> Unit,
) {
    val templates = listOf(
        ReportTemplate("evo", "Nota de Evolução", "Registro da sessão clínica a partir do prontuário", Icons.Default.EditNote, Primary, needsPatient = true),
        ReportTemplate("first", "Avaliação Inicial MTC", "Anamnese completa, diagnóstico energético e plano terapêutico", Icons.Default.AssignmentInd, CatBlue, needsPatient = true),
        ReportTemplate("discharge", "Relatório de Alta", "Síntese do tratamento, resultados e orientações pós-alta", Icons.Default.TaskAlt, CatGreen, needsPatient = true),
        ReportTemplate("monthly", "Relatório Mensal Clínico", "Resumo mensal de atendimentos e evolução dos casos", Icons.Default.CalendarViewMonth, CatAmber),
        ReportTemplate("financial", "Relatório Financeiro", "Receitas, pagamentos pendentes e análise por período", Icons.Default.AccountBalance, CatPurple),
        ReportTemplate("referral", "Encaminhamento Médico", "Carta de encaminhamento para outros profissionais", Icons.Default.LocalHospital, CatOrange, needsPatient = true),
        ReportTemplate("consent", "Termo de Consentimento", "TCLE para acupuntura e técnicas de MTC", Icons.Default.Gavel, CatBlueGrey),
        ReportTemplate("anamnese", "Ficha de Anamnese", "Formulário de histórico completo do paciente", Icons.Default.Description, CatGreen, needsPatient = true)
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Selecione um modelo — o corpo é preenchido com os dados reais do prontuário do paciente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(templates) { tpl ->
            ReportTemplateCard(tpl, generating = generating, onGenerate = onGenerate)
        }
        if (generateError != null) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Text(
                            generateError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onDismissError) { Text("OK") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportTemplateCard(
    tpl: ReportTemplate,
    generating: Boolean,
    onGenerate: (String, String, String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    SupremoClickableCard(
        onClick = { if (!generating) showDialog = true },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                Text(tpl.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(tpl.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outlineVariant)
        }
    }

    if (showDialog) {
        var patientName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!generating) showDialog = false },
            icon = { Icon(tpl.icon, null, tint = tpl.color) },
            title = { Text(tpl.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (tpl.needsPatient) {
                        Text("Paciente:", style = MaterialTheme.typography.labelMedium)
                        OutlinedTextField(
                            value = patientName, onValueChange = { patientName = it },
                            placeholder = { Text("Nome do paciente") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            enabled = !generating,
                        )
                    }
                    Surface(
                        color = tpl.color.copy(alpha = 0.06f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, null, tint = tpl.color, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (tpl.needsPatient)
                                    "O relatório é preenchido com o prontuário real do paciente."
                                else
                                    "Gerado automaticamente com os dados da clínica.",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    if (generating) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Gerando relatório...", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onGenerate(tpl.id, tpl.title, patientName.trim())
                        showDialog = false
                    },
                    enabled = !generating && (!tpl.needsPatient || patientName.isNotBlank()),
                    colors = ButtonDefaults.buttonColors(containerColor = tpl.color)
                ) {
                    if (generating) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Gerar")
                }
            },
            dismissButton = { TextButton(onClick = { if (!generating) showDialog = false }, enabled = !generating) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun GeneratedReportsTab(
    reports: List<com.bioacupunt.relatorios.domain.model.Report>,
    lastGenerated: com.bioacupunt.relatorios.domain.model.Report?,
    onDismissLast: () -> Unit,
) {
    val fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy", java.util.Locale.Builder().setLanguage("pt").setRegion("BR").build())
    val context = androidx.compose.ui.platform.LocalContext.current

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lastGenerated?.let { fresh ->
            item(key = "fresh") {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.06f))) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = Primary, modifier = Modifier.size(18.dp))
                            Text("Gerado: ${fresh.title}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = onDismissLast) { Text("OK") }
                        }
                        Text(
                            fresh.body.ifBlank { "—" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (reports.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = Primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Nenhum relatório salvo ainda.", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                    }
                }
            }
        } else {
            items(reports, key = { it.id }) { r ->
                val date = runCatching {
                    java.time.OffsetDateTime.parse(r.generatedAt).toLocalDate().format(fmt)
                }.getOrDefault(r.generatedAt.take(10))
                ReportRow(r, date, context)
            }
        }
    }
}

@Composable
private fun ReportRow(
    r: com.bioacupunt.relatorios.domain.model.Report,
    date: String,
    context: android.content.Context,
) {
    var expanded by remember { mutableStateOf(false) }
    SupremoClickableCard(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EditNote, null, tint = Primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                    Text(
                        if (r.patientName.isNotBlank()) "${r.patientName} · $date" else date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = {
                        val text = buildString {
                            append(r.title)
                            append("\n\n")
                            append(r.body.ifBlank { "Sem conteúdo." })
                        }
                        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, r.title)
                            putExtra(android.content.Intent.EXTRA_TEXT, text)
                        }
                        runCatching {
                            context.startActivity(android.content.Intent.createChooser(send, "Compartilhar relatório"))
                        }
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Default.Share, "Compartilhar", tint = Primary, modifier = Modifier.size(18.dp))
                }
            }
            if (expanded && r.body.isNotBlank()) {
                HorizontalDivider()
                Text(
                    r.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun FinancialReportTab(
    vm: com.bioacupunt.financeiro.presentation.FinanceiroViewModel = viewModel(factory = AppContainer.financeiroViewModelFactory),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val brl = { v: Double -> java.text.NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("pt").setRegion("BR").build()).format(v) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                FinStatCard(Modifier.weight(1f), "Recebido no mês", brl(state.monthReceivedBrl))
                FinStatCard(Modifier.weight(1f), "Pendente no mês", brl(state.monthPendingBrl))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                FinStatCard(Modifier.weight(1f), "Consultas pagas", "${state.paidCount}")
                FinStatCard(Modifier.weight(1f), "Ticket médio", brl(state.ticketMedioBrl))
            }
        }
        item {
            SupremoCard {
                Text("Lançamentos recentes", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(8.dp))
                if (state.recentTransactions.isEmpty()) {
                    Text("Sem lançamentos ainda.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.recentTransactions.forEach { t ->
                        val isPaid = t.status == com.bioacupunt.financeiro.domain.model.TransactionStatus.PAID.name
                        val color = if (isPaid) statusColors().success else statusColors().warning
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(if (isPaid) Icons.Default.CheckCircle else Icons.Default.Schedule, null, tint = color, modifier = Modifier.size(18.dp))
                                Column {
                                    Text(t.category, style = MaterialTheme.typography.bodySmall)
                                    Text(t.method, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(brl(t.amountBrl), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = color))
                        }
                    }
                }
            }
        }
        item {
            SupremoCard {
                Text("Receita por procedimento", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(10.dp))
                if (state.revenueByCategory.isEmpty()) {
                    Text("Sem receita registrada neste mês.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.revenueByCategory.forEach { r ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(r.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(brl(r.amountBrl), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                            }
                            Spacer(Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                Box(modifier = Modifier.fillMaxWidth(r.fraction.coerceIn(0f, 1f)).height(6.dp).clip(RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(listOf(Primary, MaterialTheme.colorScheme.secondary))))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinStatCard(modifier: Modifier, label: String, value: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    }
}
