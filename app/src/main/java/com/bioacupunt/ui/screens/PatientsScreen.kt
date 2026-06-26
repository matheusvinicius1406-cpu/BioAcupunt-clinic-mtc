package com.bioacupunt.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bioacupunt.patient.domain.model.Patient
import com.bioacupunt.patient.presentation.PatientsEvent
import com.bioacupunt.patient.presentation.PatientsUiState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bioacupunt.patient.presentation.PatientsViewModel

@Composable
fun PatientsScreen(vm: PatientsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            state.isLoading -> CircularProgressIndicator()
            state.error != null -> Text(text = state.error ?: "", color = MaterialTheme.colorScheme.error)
            state.patients.isEmpty() -> Text("Nenhum paciente encontrado.")
            else -> PatientsList(patients = state.patients)
        }

        var nameInput by remember { mutableStateOf("") }
        Spacer(modifier = Modifier.height(16.dp))
        TextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Nome do paciente") })
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = { vm.onEvent(PatientsEvent.CreatePatient(name = nameInput)); nameInput = "" }) {
            Text("Criar paciente")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = { vm.onEvent(PatientsEvent.OnLoad) }) {
            Text("Recarregar")
        }
    }

    LaunchedEffect(Unit) {
        vm.onEvent(PatientsEvent.OnLoad)
    }
}

@Composable
private fun PatientsList(patients: List<Patient>) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(patients) { p ->
            Text(
                text = "${p.name} (id ${p.id})",
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}
