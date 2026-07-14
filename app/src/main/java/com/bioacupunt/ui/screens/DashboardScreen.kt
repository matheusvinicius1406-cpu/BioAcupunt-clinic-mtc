package com.bioacupunt.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.ui.theme.Accent
import com.bioacupunt.ui.theme.PrimaryDark
import com.bioacupunt.ui.theme.SemanticError
import com.bioacupunt.ui.theme.SemanticInfo
import com.bioacupunt.ui.theme.SemanticSuccess
import com.bioacupunt.ui.theme.SemanticWarning
import com.bioacupunt.ui.theme.SemanticWarningBg
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.TextMuted
import com.bioacupunt.agenda.domain.model.AppointmentType
import com.bioacupunt.core.network.NetworkStatus
import com.bioacupunt.crm.presentation.uiColor
import com.bioacupunt.dashboard.presentation.DashInsightLevel
import com.bioacupunt.dashboard.presentation.DashboardViewModel
import com.bioacupunt.dashboard.presentation.KanbanColumn
import com.bioacupunt.dashboard.presentation.ReengagePatient
import com.bioacupunt.di.AppContainer
import com.bioacupunt.observability.SyncStatus
import com.bioacupunt.observability.SyncStatusMonitor
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class DashInsight(
    val icon: ImageVector,
    val title: String,
    val desc: String,
    val color: Color
)

private data class DashMetric(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val color: Color,
)

@Composable
fun DashboardScreen(
    onNavigateToAgenda: () -> Unit = {},
    onNavigateToCRM: () -> Unit = {},
    onNavigateToRelatorios: () -> Unit = {},
    onNavigateToAI: () -> Unit = {},
    viewModel: DashboardViewModel? = null
) {
    val vm = viewModel ?: viewModel(factory = AppContainer.dashboardViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val today = remember { LocalDate.now() }
    val hour  = remember { LocalTime.now().hour }
    val greeting = when (hour) {
        in 5..11  -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else      -> "Boa noite"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header banner ──────────────────────────────────
        item {
            val connectivityStatus by AppContainer.connectivityObserverHandler.status.collectAsState(NetworkStatus.UNKNOWN)
            val syncStatus by AppContainer.syncStatusManager.status.collectAsState(initial = SyncStatus.Idle)
            val statusText = SyncStatusMonitor.describe(syncStatus, connectivityStatus)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(Brush.verticalGradient(listOf(Primary, PrimaryDark)))
                    .padding(20.dp)
            ) {
                val greetingText = if (state.greetingName.isBlank()) {
                    "$greeting 👋"
                } else {
                    "$greeting, ${state.greetingName} 👋"
                }
                Text(
                    greetingText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White, fontWeight = FontWeight.ExtraBold
                    )
                )
                Text(
                    today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt","BR"))),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f))
                )
                if (statusText.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(statusText, style = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(alpha = 0.85f)))
                }
            }
        }

        // ── Metrics grid (2 cols) ───────────────────────────
        item {
            val metrics = listOf(
                DashMetric(Icons.Default.Today, "Hoje", "${state.todayCount}", Primary),
                DashMetric(Icons.Default.Group, "Ativos", "${state.activeCount}", SemanticInfo),
                DashMetric(Icons.Default.PeopleAlt, "Pacientes", "${state.totalPatients}", Accent),
                DashMetric(Icons.Default.AttachMoney, "Recebido/mês", brlCompact(state.monthReceivedBrl), SemanticSuccess),
                DashMetric(Icons.Default.HourglassBottom, "Pendente/mês", brlCompact(state.monthPendingBrl), SemanticWarning),
                DashMetric(Icons.Default.NotificationImportant, "Ausentes 30d+", "${state.overdueCount}", SemanticError),
                DashMetric(Icons.Default.EventBusy, "Sem retorno", "${state.noNextCount}", TextMuted),
            )
            MetricsGrid(metrics)
        }

        // ── Reengajamento ───────────────────────────────────
        if (state.reengage.isNotEmpty()) {
            item {
                ReengagementCard(
                    patients = state.reengage,
                    overdueCount = state.overdueCount,
                    onWhats = { phone -> openWhatsApp(context, phone) },
                    onCall = { phone -> dialPhone(context, phone) },
                )
            }
        }

        // ── Kanban Clínico ──────────────────────────────────
        item {
            KanbanClinico(state.kanban, onCardClick = vm::advanceStage)
        }

        // ── Acesso Rápido ───────────────────────────────────
        item {
            QuickAccessCard(
                onNovoPaciente = onNavigateToCRM,
                onRelatorio = onNavigateToRelatorios,
                onAssistenteIA = onNavigateToAI,
                onAgenda = onNavigateToAgenda,
            )
        }

        // ── Próximo atendimento ─────────────────────────────
        state.nextAppointment?.let { next ->
            item {
                NextAppointmentCard(
                    initials = initialsOf(next.patientName),
                    name = next.patientName,
                    time = next.time,
                    subtitle = apptSubtitle(next.type, next.sessionNumber),
                    onNavigateToAgenda = onNavigateToAgenda,
                )
            }
        }

        // ── Alertas & Sugestões (determinístico, sem IA) ───
        item { DashSectionTitle("Alertas & Sugestões", Icons.Default.NotificationImportant) }
        items(state.insights) { insight -> DashInsightCard(insight.toUi()) }

        // ── Agenda de Hoje ─────────────────────────────────
        item { DashSectionTitle("Agenda de Hoje", Icons.Default.Schedule) }

        if (state.todayAppointments.isEmpty()) {
            item {
                Text(
                    "Nenhuma consulta agendada para hoje.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(state.todayAppointments) { appt ->
                DashApptRow(
                    appt.time,
                    appt.patientName,
                    apptSubtitle(appt.type, appt.sessionNumber),
                    onClick = onNavigateToAgenda,
                )
            }
        }

        // ── Financeiro Resumo ──────────────────────────────
        item { DashSectionTitle("Financeiro do Mês", Icons.Default.BarChart) }
        item {
            DashCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    val received = state.monthReceivedBrl
                    val pending = state.monthPendingBrl
                    DashFinanceRow("Recebido no mês", brl(received), SemanticSuccess)
                    DashFinanceRow("Pendente", brl(pending), SemanticWarning)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DashFinanceRow("Total previsto", brl(received + pending), Primary, bold = true)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FooterBadge("✓ Selo de Excelência Clínica", solid = true)
                Spacer(Modifier.width(8.dp))
                FooterBadge("v2.1.0 · Supabase", solid = false)
            }
        }
    }
}

/** Card chrome shared by every Dashboard section: white surface, soft shadow, 1px border. */
@Composable
private fun DashCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, shape = shape, spotColor = Color.Black.copy(alpha = 0.08f))
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
    ) {
        content()
    }
}

@Composable
private fun MetricsGrid(metrics: List<DashMetric>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metrics.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { m ->
                    DashCard(modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.large) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(m.icon, null, tint = m.color, modifier = Modifier.size(16.dp))
                                Text(
                                    m.label.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                m.value,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ReengagementCard(
    patients: List<ReengagePatient>,
    overdueCount: Int,
    onWhats: (String) -> Unit,
    onCall: (String) -> Unit,
) {
    DashCard(shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.NotificationImportant, null, tint = SemanticWarning, modifier = Modifier.size(20.dp))
                    Text("Reengajamento", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                }
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(SemanticWarningBg)
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                ) {
                    Text(
                        "$overdueCount ausentes 30d+",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SemanticWarning,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                patients.forEach { r ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(34.dp).clip(CircleShape).background(TextMuted),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(r.initials, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(r.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(r.lastVisitLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RoundIconButton(Icons.Default.Chat) { onWhats(r.phone) }
                        RoundIconButton(Icons.Default.Call) { onCall(r.phone) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun KanbanClinico(columns: List<KanbanColumn>, onCardClick: (Long) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Kanban Clínico", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Text("toque no card p/ avançar", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            columns.forEach { col -> KanbanColumnCard(col, onCardClick) }
        }
    }
}

@Composable
private fun KanbanColumnCard(col: KanbanColumn, onCardClick: (Long) -> Unit) {
    val stageColor = col.stage.uiColor
    Box(
        modifier = Modifier
            .width(200.dp)
            .shadow(8.dp, shape = MaterialTheme.shapes.large, spotColor = Color.Black.copy(alpha = 0.06f))
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(stageColor))
                Text(col.stage.label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier.clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.background).padding(horizontal = 8.dp, vertical = 1.dp)
                ) {
                    Text("${col.count}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextMuted)
                }
            }
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                col.cards.forEach { c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.background)
                            .clickable { onCardClick(c.patientId) }
                            .padding(start = 8.dp, top = 9.dp, bottom = 9.dp, end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).background(stageColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(c.initials, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(c.name, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(c.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    onNovoPaciente: () -> Unit,
    onRelatorio: () -> Unit,
    onAssistenteIA: () -> Unit,
    onAgenda: () -> Unit,
) {
    DashCard(shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Acesso rápido", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAction(Modifier.weight(1f), Icons.Default.PersonAdd, "Novo\nPaciente", onNovoPaciente)
                QuickAction(Modifier.weight(1f), Icons.Default.Description, "Relatório", onRelatorio)
                QuickAction(Modifier.weight(1f), Icons.Default.SmartToy, "Assistente\nIA", onAssistenteIA)
                QuickAction(Modifier.weight(1f), Icons.Default.CalendarMonth, "Agenda", onAgenda)
            }
        }
    }
}

@Composable
private fun QuickAction(modifier: Modifier, icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(24.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, lineHeight = 12.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun NextAppointmentCard(
    initials: String,
    name: String,
    time: String,
    subtitle: String,
    onNavigateToAgenda: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape = MaterialTheme.shapes.extraLarge, spotColor = Primary.copy(alpha = 0.28f))
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(Primary, PrimaryDark)))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "PRÓXIMO ATENDIMENTO",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.85f)
            )
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable(onClick = onNavigateToAgenda)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Agenda ›", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column {
                Text("$name · $time", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f)))
            }
        }
    }
}

@Composable
private fun DashSectionTitle(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(18.dp))
        Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun DashInsightCard(insight: DashInsight) {
    DashCard {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(insight.icon, null, tint = insight.color, modifier = Modifier.size(18.dp).padding(top = 2.dp))
            Column {
                Text(insight.title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                Text(insight.desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DashApptRow(time: String, name: String, type: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape = MaterialTheme.shapes.medium, spotColor = Color.Black.copy(alpha = 0.05f))
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                time,
                style = MaterialTheme.typography.labelLarge.copy(color = Primary, fontWeight = FontWeight.Bold),
                modifier = Modifier.width(44.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                Text(type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextMuted)
        }
    }
}

@Composable
private fun DashFinanceRow(label: String, value: String, color: Color, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(
            color = color,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        ))
    }
}

@Composable
private fun FooterBadge(text: String, solid: Boolean) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(if (solid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(if (solid) 0.dp else 1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraLarge)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (solid) Primary else TextMuted,
        )
    }
}

/** Maps a domain insight (level + text) to the UI card struct (icon + colour). */
private fun com.bioacupunt.dashboard.presentation.DashInsight.toUi(): DashInsight {
    val (icon, color) = when (level) {
        DashInsightLevel.ALERT -> Icons.Default.NotificationImportant to SemanticError
        DashInsightLevel.INFO -> Icons.Default.EventAvailable to Primary
        DashInsightLevel.POSITIVE -> Icons.Default.CheckCircle to SemanticSuccess
    }
    return DashInsight(icon, title, desc, color)
}

private fun apptSubtitle(type: String, sessionNumber: Int): String {
    val label = runCatching { AppointmentType.valueOf(type).label }.getOrDefault(type)
    return if (sessionNumber > 0) "$label · Sessão $sessionNumber" else label
}

private fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

private fun dialPhone(context: android.content.Context, phone: String) {
    if (phone.isBlank()) return
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
}

private fun openWhatsApp(context: android.content.Context, phone: String) {
    if (phone.isBlank()) return
    val digits = phone.filter { it.isDigit() }
    val withCountryCode = if (digits.startsWith("55")) digits else "55$digits"
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$withCountryCode")))
}

private fun brl(value: Double): String =
    java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)

private fun brlCompact(value: Double): String = when {
    value >= 1_000_000 -> "R$%.1fM".format(Locale("pt", "BR"), value / 1_000_000)
    value >= 1_000 -> "R$%.1fk".format(Locale("pt", "BR"), value / 1_000)
    else -> "R$%.0f".format(Locale("pt", "BR"), value)
}
