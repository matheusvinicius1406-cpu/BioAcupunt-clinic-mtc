package com.bioacupunt.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.bioacupunt.crm.domain.model.CrmPatient
import com.bioacupunt.crm.domain.model.PatientStage

@Composable
fun PipelineTab(
    stages: List<PatientStage>,
    patients: List<CrmPatient>,
    viewModel: com.bioacupunt.crm.presentation.CrmViewModel
) {
    val stageMap = remember(patients) { patients.groupBy { it.stage }.mapValues { it.value } }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(stages) { stage ->
            val stagePatients = stageMap[stage.name].orEmpty()
            val show = stagePatients.isNotEmpty() || stage == PatientStage.TREATMENT
            if (show) {
                PipelineColumn(stage, stagePatients) { targetStage ->
                    stagePatients.forEach { p ->
                        if (p.stage != targetStage.name) {
                            viewModel.updateStage(p.id, targetStage)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CrmReportsTab(summary: Map<String, Any>) {
    if (summary.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum dado de relatório disponível.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(summary.entries.toList()) { (key, value) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(key, style = MaterialTheme.typography.bodyMedium)
                    Text(value.toString(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

@Composable
private fun PipelineColumn(
    stage: PatientStage,
    patients: List<CrmPatient>,
    onMoveAll: (PatientStage) -> Unit
) {
    val stageColor = when (stage) {
        PatientStage.ACTIVE, PatientStage.TREATMENT -> Color(0xFF4CAF50)
        PatientStage.MAINTENANCE -> Color(0xFF64B5F6)
        PatientStage.INACTIVE -> Color(0xFFFF8A65)
        PatientStage.CHURNED -> Color(0xFFEF5350)
        PatientStage.LEAD -> Color(0xFF9575CD)
        PatientStage.FIRST_CONTACT -> Color(0xFF87B344)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = stageColor.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, stageColor.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stage.emoji, style = MaterialTheme.typography.titleMedium)
                Text(stage.label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.weight(1f))
                Badge(containerColor = stageColor) { Text("${patients.size}") }
            }
            Spacer(Modifier.height(8.dp))
            if (patients.isEmpty()) {
                Text("Nenhum paciente aqui.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                var showMenu by remember { mutableIntStateOf(-1) }
                patients.forEach { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(stageColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(p.name.first().toString(), style = MaterialTheme.typography.titleSmall.copy(color = stageColor))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                            Text("${p.totalSessions} sessões · R$ %.0f".format(p.totalRevenueBrl), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box {
                            IconButton(onClick = { showMenu = p.id.toInt() }) { Icon(Icons.Default.MoreVert, null) }
                            DropdownMenu(expanded = showMenu == p.id.toInt(), onDismissRequest = { showMenu = -1 }) {
                                PatientStage.entries.filter { it != stage }.forEach { target ->
                                    DropdownMenuItem(
                                        text = { Text("${target.emoji} Mover para ${target.label}") },
                                        onClick = { onMoveAll(target); showMenu = -1 }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
