package com.bioacupunt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.core.network.NetworkStatus
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

@Composable
fun DashboardScreen(
    onNavigateToAgenda: () -> Unit = {},
    onNavigateToCRM: () -> Unit = {},
    onNavigateToRelatorios: () -> Unit = {},
    onNavigateToAI: () -> Unit = {}
) {
    val today = remember { LocalDate.now() }
    val hour  = remember { LocalTime.now().hour }
    val greeting = when (hour) {
        in 5..11  -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else      -> "Boa noite"
    }

    val glassSurface = Color.White.copy(alpha = 0.10f)
    val glassStroke = Color.White.copy(alpha = 0.18f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Header banner ──────────────────────────────────
        item {
            val connectivityStatus by AppContainer.connectivityObserverHandler.status.collectAsState(NetworkStatus.UNKNOWN)
            val syncStatus by AppContainer.syncStatusManager.status.collectAsState(initial = SyncStatus.Idle)
            val statusText = SyncStatusMonitor.describe(syncStatus, connectivityStatus)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Primary.copy(alpha = 0.45f),
                                Primary.copy(alpha = 0.18f),
                                Color(0xFF042A0E)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Text(
                    "$greeting, Dra. Camila 👋",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White, fontWeight = FontWeight.ExtraBold
                    )
                )
                Text(
                    today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt","BR"))),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.75f))
                )
                if (statusText.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(statusText, style = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(alpha = 0.75f)))
                }
            }
        }

        // ── KPI cards ──────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashStatCard(Modifier.weight(1f), Icons.Default.Today, "5", "Hoje", Primary)
                DashStatCard(Modifier.weight(1f), Icons.Default.Group, "47", "Ativos", Color(0xFF64B5F6))
                DashStatCard(Modifier.weight(1f), Icons.Default.AttachMoney, "R$3.2k", "Mês", Color(0xFF81C784))
            }
        }

        // ── IA Insights ────────────────────────────────────
        item { DashSectionTitle("Insights da IA 🤖", Icons.Default.AutoAwesome) }

        val insights = listOf(
            DashInsight(Icons.Default.NotificationImportant, "3 pacientes sem retorno (30+ dias)", "Ana, João e Petra não reagendaram. Contato sugerido.", Color(0xFFFF8A65)),
            DashInsight(Icons.Default.EventAvailable, "2 horários livres na quinta", "Possibilidade de aceitar mais pacientes esta semana.", Primary),
            DashInsight(Icons.Default.Insights, "Padrão: Defic. de Yin frequente", "7 pacientes ativos com mesmo padrão. Considere grupo terapêutico.", Color(0xFF9575CD))
        )

        items(insights) { insight -> DashInsightCard(insight, glassSurface, glassStroke) }

        // ── Agenda de Hoje ─────────────────────────────────
        item { DashSectionTitle("Agenda de Hoje", Icons.Default.Schedule) }

        val today_appts = listOf(
            Triple("08:00", "Ana Lima", "Retorno · Sessão 5"),
            Triple("09:30", "Carlos Souza", "1ª Consulta"),
            Triple("11:00", "Maria Santos", "Acupuntura · Sessão 3"),
            Triple("14:00", "João Ferreira", "Moxibustão · Sessão 8"),
            Triple("15:30", "Paula Costa", "Acupuntura · Sessão 2")
        )

        items(today_appts) { (time, name, type) ->
            DashApptRow(time, name, type, onClick = onNavigateToAgenda, glassSurface, glassStroke)
        }

        item {
            ShadowButton(
                onClick = onNavigateToAgenda,
                modifier = Modifier.fillMaxWidth(),
                label = "Ver agenda completa",
                trailing = { Text("→", color = Color.White.copy(alpha = 0.9f)) }
            )
        }

        // ── Ações Rápidas ──────────────────────────────────
        item { DashSectionTitle("Ações Rápidas", Icons.Default.Bolt) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashQuickAction(Modifier.weight(1f), Icons.Default.PersonAdd, "Novo Paciente", Primary, onNavigateToCRM, glassSurface, glassStroke)
                DashQuickAction(Modifier.weight(1f), Icons.Default.Description, "Relatório", Color(0xFF64B5F6), onNavigateToRelatorios, glassSurface, glassStroke)
                DashQuickAction(Modifier.weight(1f), Icons.Default.SmartToy, "Assistente IA", Color(0xFF9575CD), onNavigateToAI, glassSurface, glassStroke)
            }
        }

        // ── Financeiro Resumo ──────────────────────────────
        item { DashSectionTitle("Financeiro do Mês", Icons.Default.BarChart) }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(12.dp, shape = MaterialTheme.shapes.large, spotColor = Color.Black.copy(alpha = 0.08f))
                    .clip(MaterialTheme.shapes.large)
                    .background(glassSurface)
                    .border(1.dp, glassStroke, MaterialTheme.shapes.large)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DashFinanceRow("Receita bruta", "R$ 4.800,00", Color(0xFF4CAF50))
                    DashFinanceRow("Consultas pagas", "R$ 3.200,00", Color(0xFF64B5F6))
                    DashFinanceRow("Pendente", "R$ 1.600,00", Color(0xFFFF8A65))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DashFinanceRow("Taxa de ocupação", "78%", Primary, bold = true)
                }
            }
        }
    }
}

@Composable
private fun glassCardModifier(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    surface: Color = Color.White.copy(alpha = 0.10f),
    stroke: Color = Color.White.copy(alpha = 0.18f),
    elevationDp: Dp = 10.dp,
    containerColor: Color? = null,
    contentColor: Color? = null,
    disabledContainerColor: Color? = null,
    contentPadding: PaddingValues? = null,
) = modifier
    .shadow(elevationDp, shape = shape, spotColor = Color.Black.copy(alpha = 0.06f))
    .clip(shape)
    .background(containerColor ?: surface)
    .border(1.dp, stroke, shape)

@Composable
private fun DashStatCard(
    modifier: Modifier, icon: ImageVector,
    value: String, label: String, color: Color
) {
    Box(modifier = glassCardModifier(modifier)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = color))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DashSectionTitle(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = Primary.copy(alpha = 0.9f), modifier = Modifier.size(18.dp))
        Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun DashInsightCard(
    insight: DashInsight,
    surface: Color,
    stroke: Color
) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp)) {
        Box(modifier = glassCardModifier(Modifier.fillMaxWidth(), surface = surface.copy(alpha = 0.55f), stroke = stroke.copy(alpha = 0.45f))) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(insight.icon, null, tint = insight.color, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                Column {
                    Text(insight.title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                    Text(insight.desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DashApptRow(
    time: String, name: String, type: String, onClick: () -> Unit,
    surface: Color, stroke: Color
) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
        Box(modifier = glassCardModifier(Modifier.fillMaxWidth(), surface = surface.copy(alpha = 0.62f)).clip(MaterialTheme.shapes.medium)) {
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
                Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.35f))
            }
        }
    }
}

@Composable
private fun DashQuickAction(
    modifier: Modifier, icon: ImageVector, label: String, color: Color, onClick: () -> Unit,
    surface: Color, stroke: Color
) {
    Box(
        modifier = modifier
            .shadow(10.dp, shape = MaterialTheme.shapes.large, spotColor = Color.Black.copy(alpha = 0.05f))
            .clip(MaterialTheme.shapes.large)
            .background(surface.copy(alpha = 0.55f))
            .border(1.dp, stroke.copy(alpha = 0.45f), MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color.copy(alpha = 0.95f), modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(color = color.copy(alpha = 0.95f)), maxLines = 1)
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
private fun ShadowButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .shadow(10.dp, shape = MaterialTheme.shapes.large, spotColor = Primary.copy(alpha = 0.32f))
            .clip(MaterialTheme.shapes.large)
            .background(Primary.copy(alpha = 0.82f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontWeight = FontWeight.Bold)
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}
