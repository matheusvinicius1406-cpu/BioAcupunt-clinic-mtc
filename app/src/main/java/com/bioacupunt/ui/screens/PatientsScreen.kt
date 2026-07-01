package com.bioacupunt.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.di.AppContainer
import com.bioacupunt.patient.domain.model.Patient
import com.bioacupunt.patient.presentation.PatientsEvent
import com.bioacupunt.patient.presentation.PatientsViewModel

@Composable
fun PatientsScreen(vm: PatientsViewModel = viewModel(factory = AppContainer.patientsViewModelFactory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    var nameInput by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Adicionar Paciente")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Pacientes", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Text(
                    text = state.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
                state.patients.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum paciente cadastrado.\nToque + para adicionar.")
                }
                else -> PatientsList(patients = state.patients)
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Novo Paciente") },
                text = {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nome") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.onEvent(PatientsEvent.CreatePatient(name = nameInput))
                        nameInput = ""
                        showAddDialog = false
                    }) { Text("Salvar") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false; nameInput = "" }) { Text("Cancelar") }
                }
            )
        }
    }

    LaunchedEffect(Unit) { vm.onEvent(PatientsEvent.OnLoad) }
}

@Composable
private fun PatientsList(patients: List<Patient>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(patients, key = { it.id }) { p ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium)
                    p.document?.let { Text("CPF: $it", style = MaterialTheme.typography.bodySmall) }
                    Text("Status: ${p.status}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
