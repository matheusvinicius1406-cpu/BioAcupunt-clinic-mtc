package com.bioacupunt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.bioacupunt.crm.domain.model.CrmPatient
import com.bioacupunt.crm.domain.model.PatientStage
import com.bioacupunt.crm.presentation.uiColor
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.TextMuted

@Composable
fun PatientsListTab(
    patients: List<CrmPatient>,
    searchQuery: String,
    selectedStage: String?,
    onSearch: (String) -> Unit,
    onStageFilter: (String?) -> Unit,
    onOpenProntuario: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onStatusChange: (Long, PatientStage) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraLarge)
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(20.dp))
            BasicTextFieldPlaceholder(searchQuery, onSearch, "Nome ou telefone…")
        }

        Spacer(Modifier.height(12.dp))

        // Status filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                StatusChip("Todos", selected = selectedStage.isNullOrBlank(), color = Primary) { onStageFilter(null) }
            }
            items(PatientStage.entries) { stage ->
                StatusChip(stage.label, selected = selectedStage == stage.name, color = stage.uiColor) {
                    onStageFilter(stage.name)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        if (patients.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(top = 40.dp), contentAlignment = Alignment.TopCenter) {
                Text("Nenhum paciente encontrado.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            return
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(patients, key = { it.id }) { p ->
                PatientCrmCard(
                    p,
                    onOpen = { onOpenProntuario(p.id) },
                    onDelete = { onDelete(p.id) },
                    onStatusChange = onStatusChange
                )
            }
        }
    }
}

@Composable
private fun BasicTextFieldPlaceholder(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.fillMaxWidth()
        )
        if (value.isBlank()) {
            Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
    }
}

@Composable
private fun StatusChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(if (selected) color else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (selected) color else MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) Color.White else color,
        )
    }
}

@Composable
private fun PatientCrmCard(
    p: CrmPatient,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (Long, PatientStage) -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var pickerStage by remember { mutableStateOf<PatientStage?>(null) }
    val stage = remember(p.stage) { PatientStage.entries.find { it.name == p.stage } ?: PatientStage.ACTIVE }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(stage.uiColor),
            contentAlignment = Alignment.Center
        ) {
            Text(initialsOf(p.name), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }
        Column(Modifier.weight(1f)) {
            Text(p.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            val secondLine = if (p.phone.isNotBlank()) "${stage.emoji} ${stage.label} · ${p.phone}" else "${stage.emoji} ${stage.label}"
            Text(secondLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(stage.uiColor.copy(alpha = 0.15f))
                    .clickable { pickerStage = stage }
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            ) {
                Text(stage.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = stage.uiColor)
            }
            TextButton(onClick = { confirmDelete = true }, contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Default.Delete, null, tint = TextMuted, modifier = Modifier.size(16.dp))
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
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pickerStage = null }) { Text("Fechar") } }
        )
    }
}

private fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}
