package com.bioacupunt.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.bioacupunt.ui.theme.Primary
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── Header banner ──────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF1B2F1A), Color(0xFF2D4E2C)))
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "$greeting, Dra. Camila 👋",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White, fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt","BR"))),
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.7f))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocalHospital, null, tint = Color.White)
                    }
                }
            }
        }

        // ── KPI cards ──────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashStatCard(Modifier.weight(1f), Icons.Default.Today,        "5",     "Hoje",         Primary)
                DashStatCard(Modifier.weight(1f), Icons.Default.Group,        "47",    "Ativos",       Color(0xFF64B5F6))
                DashStatCard(Modifier.weight(1f), Icons.Default.AttachMoney, "R$3.2k", "Mês",         Color(0xFF81C784))
            }
        }

        // ── IA Insights ────────────────────────────────────
        item { DashSectionTitle("Insights da IA 🤖", Icons.Default.AutoAwesome) }

        val insights = listOf(
            Triple(Icons.Default.NotificationImportant,
                "3 pacientes sem retorno (30+ dias)",
                "Ana, João e Petra não reagendaram. Contato sugerido.",
                Color(0xFFFF8A65)),
            Triple(Icons.Default.EventAvailable,
                "2 horários livres na quinta",
                "Possibilidade de aceitar mais pacientes esta semana.",
                Primary),
            Triple(Icons.Default.Insights,
                "Padrão: Defic. de Yin frequente",
                "7 pacientes ativos com mesmo padrão. Considere grupo terapêutico.",
                Color(0xFF9575CD))
        )

        items(insights) { (icon, title, desc, color) ->
            DashInsightCard(icon, title, desc, color)
        }

        // ── Agenda de Hoje ─────────────────────────────────
        item {
            DashSectionTitle("Agenda de Hoje", Icons.Default.Schedule)
        }

        val today_appts = listOf(
            Triple("08:00", "Ana Lima",      "Retorno · Sessão 5"),
            Triple("09:30", "Carlos Souza",  "1ª Consulta"),
            Triple("11:00", "Maria Santos",  "Acupuntura · Sessão 3"),
            Triple("14:00", "João Ferreira", "Moxibustão · Sessão 8"),
            Triple("15:30", "Paula Costa",  "Acupuntura · Sessão 2")
        )

        items(today_appts) { (time, name, type) ->
            DashApptRow(time, name, type, onClick = onNavigateToAgenda)
        }

        item {
            TextButton(
                onClick = onNavigateToAgenda,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) { Text("Ver agenda completa →", color = Primary) }
        }

        // ── Ações Rápidas ──────────────────────────────────
        item { DashSectionTitle("Ações Rápidas", Icons.Default.Bolt) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashQuickAction(Modifier.weight(1f), Icons.Default.PersonAdd,  "Novo Paciente", Primary, onNavigateToCRM)
                DashQuickAction(Modifier.weight(1f), Icons.Default.Description, "Relatório",    Color(0xFF64B5F6), onNavigateToRelatorios)
                DashQuickAction(Modifier.weight(1f), Icons.Default.SmartToy,   "Assistente IA", Color(0xFF9575CD), onNavigateToAI)
            }
        }

        // ── Financeiro Resumo ──────────────────────────────
        item { DashSectionTitle("Financeiro do Mês", Icons.Default.BarChart) }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DashFinanceRow("Receita bruta",     "R$ 4.800,00", Color(0xFF4CAF50))
                    DashFinanceRow("Consultas pagas",   "R$ 3.200,00", Color(0xFF64B5F6))
                    DashFinanceRow("Pendente",          "R$ 1.600,00", Color(0xFFFF8A65))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DashFinanceRow("Taxa de ocupação",  "78%",         Primary, bold = true)
                }
            }
        }
    }
}

// ── Shared Composables ──────────────────────────────────────
@Composable
private fun DashStatCard(
    modifier: Modifier, icon: ImageVector,
    value: String, label: String, color: Color
) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(3.dp))
            Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = color))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DashSectionTitle(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(18.dp))
        Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun DashInsightCard(icon: ImageVector, title: String, detail: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.07f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.18f))
    ) {
        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp).padding(top = 2.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DashApptRow(time: String, name: String, type: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                time,
                style = MaterialTheme.typography.labelLarge.copy(color = Primary, fontWeight = FontWeight.Bold),
                modifier = Modifier.width(44.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                Text(type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun DashQuickAction(modifier: Modifier, icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(3.dp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(color = color), maxLines = 1)
        }
    }
}

@Composable
private fun DashFinanceRow(label: String, value: String, color: Color, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(
            color = color,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        ))
    }
}
