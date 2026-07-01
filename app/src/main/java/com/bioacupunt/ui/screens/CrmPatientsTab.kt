package com.bioacupunt.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.bioacupunt.crm.domain.model.CrmPatient
import com.bioacupunt.crm.domain.model.PatientStage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatientsListTab(
    patients: List<CrmPatient>,
    searchQuery: String,
    onSearch: (String) -> Unit,
    onOpenProntuario: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onStatusChange: (Long, PatientStage) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearch,
            placeholder = { Text("Buscar paciente…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedButton(
            onClick = onOpenProntuario,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Icon(Icons.Default.Description, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Ver Prontuários")
        }
        Spacer(Modifier.height(8.dp))
        if (patients.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum paciente encontrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(patients, key = { it.id }) { p ->
                PatientCrmCard(
                    p,
                    onDelete = { onDelete(p.id) },
                    onStatusChange = onStatusChange
                )
            }
        }
    }
}

@Composable
private fun PatientCrmCard(
    p: CrmPatient,
    onDelete: () -> Unit,
    onStatusChange: (Long, PatientStage) -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var pickerStage by remember { mutableStateOf<PatientStage?>(null) }
    val stage = remember(p.stage) { PatientStage.entries.find { it.name == p.stage } ?: PatientStage.ACTIVE }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(p.name.first().toString(), style = MaterialTheme.typography.titleMedium.copy(color = Primary))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Text("${stage.emoji} ${stage.label}", style = MaterialTheme.typography.bodySmall)
                if (p.phone.isNotBlank()) Text(p.phone, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${p.totalSessions}x", style = MaterialTheme.typography.titleSmall.copy(color = Primary, fontWeight = FontWeight.Bold))
                Text("sessões", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("R$ %.0f".format(p.totalRevenueBrl), style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { pickerStage = stage }) { Text("Etapa") }
                    TextButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, null) }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remover paciente?") },
            text = { Text("Essa ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); confirmDelete = false }) { Text("Remover", color = Color(0xFFEF5350)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
        )
    }

    if (pickerStage != null) {
        AlertDialog(
            onDismissRequest = { pickerStage = null },
            title = { Text("Alterar etapa") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PatientStage.entries.forEach { candidate ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onStatusChange(p.id, candidate); pickerStage = null }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(candidate.emoji, modifier = Modifier.width(28.dp))
                            Text(candidate.label, modifier = Modifier.weight(1f))
                            if (candidate == stage) Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50))
                        }
                    }
                }
            },
            dismissButton = { TextButton(onClick = { pickerStage = null }) { Text("Fechar") } }
        )
    }
}
