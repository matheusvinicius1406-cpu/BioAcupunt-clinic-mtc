package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.MockData

@Composable
fun DashboardScreen() {
    val totalPatients = MockData.patients.size
    val activeTreatments = MockData.appointments.count { it.status == "scheduled" }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bento Main Activity Card (Large) - Col-span-2
                StatCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "TOTAL ATIVOS CRM",
                    value = totalPatients.toString(),
                    icon = Icons.Outlined.People,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    isLarge = true
                )

                // Bento 2 small squares - Col-span-1 each
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        title = "EM TRATAMENTO",
                        value = activeTreatments.toString(),
                        icon = Icons.Outlined.MedicalServices,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                    StatCard(
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        title = "ALERTAS",
                        value = "0",
                        icon = Icons.Outlined.Warning,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                // Bento Wide card - Col-span-2
                StatCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "HISTÓRICO MTC",
                    value = MockData.appointments.size.toString(),
                    icon = Icons.Outlined.Timeline,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    borderColor = MaterialTheme.colorScheme.surfaceVariant,
                    isHorizontal = true
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Alertas de Reengajamento (0)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Pacientes inativos há mais de 30 dias.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { /*TODO*/ },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Text("Ver Vagas")
                    }
                }
            }
        }

        item {
            Text("Kanban Clínico", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Monitore o percurso terapêutico desde a triagem até a alta.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KanbanColumn(modifier = Modifier.weight(1f), title = "Pré-Consulta", count = 0)
                KanbanColumn(modifier = Modifier.weight(1f), title = "Avaliação", count = 1)
                KanbanColumn(modifier = Modifier.weight(1f), title = "Em Tratamento", count = activeTreatments)
                KanbanColumn(modifier = Modifier.weight(1f), title = "Retorno", count = 0)
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color? = null,
    isLarge: Boolean = false,
    isHorizontal: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        border = borderColor?.let { BorderStroke(1.dp, it) }
    ) {
        if (isHorizontal) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(56.dp).background(contentColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(28.dp))
                }
                Column {
                    Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = contentColor.copy(alpha = 0.7f))
                    Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier.size(48.dp).background(contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
                    }
                    if (isLarge) {
                        Text("ATUALIZADO AGORA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = contentColor.copy(alpha = 0.7f))
                    }
                }
                Spacer(modifier = if (isLarge) Modifier.height(32.dp) else Modifier.height(16.dp))
                Column {
                    Text(value, style = if (isLarge) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = contentColor.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
fun KanbanColumn(modifier: Modifier = Modifier, title: String, count: Int) {
    Card(
        modifier = modifier.height(300.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(count.toString(), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Vazio.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}
