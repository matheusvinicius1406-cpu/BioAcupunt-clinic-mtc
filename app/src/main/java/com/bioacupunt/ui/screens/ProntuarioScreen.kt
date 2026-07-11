package com.bioacupunt.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.di.AppContainer
import com.bioacupunt.patient.domain.model.Patient
import com.bioacupunt.patient.domain.usecase.GetPatients
import com.bioacupunt.prontuario.domain.model.ProntuarioEntry
import com.bioacupunt.prontuario.domain.model.ProntuarioEntryType
import com.bioacupunt.prontuario.presentation.ProntuarioViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProntuarioScreen(
    onBack: (() -> Unit)? = null,
    vm: ProntuarioViewModel = viewModel(factory = AppContainer.prontuarioViewModelFactory),
    patientId: Long = 0L
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showPatientPicker by remember { mutableStateOf(patientId <= 0L) }
    var showSessionDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<ProntuarioEntry?>(null) }
    var confirmDelete by remember { mutableStateOf<ProntuarioEntry?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var allPatients by remember { mutableStateOf<List<Patient>>(emptyList()) }
    LaunchedEffect(Unit) {
        scope.launch { AppContainer.getPatients().collect { allPatients = it } }
    }

    LaunchedEffect(patientId) {
        if (patientId > 0L) {
            vm.load(patientId)
            showPatientPicker = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.patientId > 0L) "Prontuário" else "Prontuário") },
                navigationIcon = {
                    if (onBack != null && state.patientId > 0L) {
                        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") }
                    }
                },
                actions = {
                    if (state.patientId > 0L) {
                        IconButton(onClick = { editingEntry = null; showSessionDialog = true }) { Icon(Icons.Default.Add, contentDescription = null) }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            if (showPatientPicker || state.patientId <= 0L) {
                Text("Selecione o paciente", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                if (allPatients.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nenhum paciente disponível.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                        items(allPatients, key = { it.id }) { p ->
                            Card(modifier = Modifier.fillMaxWidth().clickable {
                                vm.load(p.id)
                                showPatientPicker = false
                            }) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                        if (p.document.orEmpty().isNotBlank()) Text(p.document.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            } else {
                if (state.loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (state.error != null) {
                    Text(text = state.error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = state.summary, onValueChange = { vm.updateHeader(summary = it) }, label = { Text("Resumo") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = state.mainComplaint, onValueChange = { vm.updateHeader(mainComplaint = it) }, label = { Text("Queixa principal") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = state.diagnosis, onValueChange = { vm.updateHeader(diagnosis = it) }, label = { Text("Diagnóstico") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = state.treatmentPlan, onValueChange = { vm.updateHeader(treatmentPlan = it) }, label = { Text("Plano") }, modifier = Modifier.fillMaxWidth())
                }

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Evoluções", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(8.dp))
                    AssistChip(onClick = { editingEntry = null; showSessionDialog = true }, label = { Text("Nova") }, leadingIcon = { Icon(Icons.Default.Add, null) })
                    Spacer(Modifier.width(6.dp))
                    Text("${state.entries.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))

                if (state.entries.isEmpty()) {
                    Text("Sem registros.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        items(state.entries, key = { it.id }) { e ->
                            Card(elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            val typeColor = when (e.type) {
                                                ProntuarioEntryType.ANAMNESE -> Color(0xFF90CAF9)
                                                ProntuarioEntryType.EXAM -> Color(0xFFFFF176)
                                                ProntuarioEntryType.OBSERVATION -> Color(0xFFFFCC80)
                                                else -> Color(0xFF81C784)
                                            }
                                            Text("${e.type.label} · ${e.date}", style = MaterialTheme.typography.labelMedium, color = typeColor)
                                            if (e.doctorName.isNotBlank()) Text("Dr. ${e.doctorName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(onClick = { editingEntry = e; showSessionDialog = true }) { Icon(Icons.Default.Edit, null) }
                                            IconButton(onClick = { confirmDelete = e }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFEF5350)) }
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(e.body, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSessionDialog) {
        SessionFormDialog(
            entry = editingEntry,
            onDismiss = {
                showSessionDialog = false
                editingEntry = null
            },
            onSave = { type, body ->
                showSessionDialog = false
                editingEntry = null
                val id = editingEntry?.id
                if (id == null) {
                    vm.addSession(body, type)
                } else {
                    vm.updateEntry(id, type, body)
                }
            }
        )
    }

    confirmDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Remover registro?") },
            text = { Text("Essa ação não pode ser desfeita.") },
            confirmButton = { TextButton(onClick = { vm.deleteSession(entry.id); confirmDelete = null }) { Text("Remover", color = Color(0xFFEF5350)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancelar") } }
        )
    }

    LaunchedEffect(state.error) {
        state.error?.let { msg ->
            if (msg.isNotBlank()) {
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                vm.clearError()
            }
        }
    }
}

@Composable
private fun SessionFormDialog(
    entry: ProntuarioEntry?,
    onDismiss: () -> Unit,
    onSave: (ProntuarioEntryType, String) -> Unit
) {
    var type by remember { mutableStateOf(entry?.type ?: ProntuarioEntryType.EVOLUTION) }
    var body by remember { mutableStateOf(entry?.body ?: "") }
    var doctor by remember { mutableStateOf(entry?.doctorName ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "Nova Sessão" else "Editar Sessão", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tipo:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProntuarioEntryType.entries.forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                OutlinedTextField(value = doctor, onValueChange = { doctor = it }, label = { Text("Responsável") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Evolução") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (body.isNotBlank()) onSave(type, "$doctor\n\n$body")
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
