package com.bioacupunt.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.di.AppContainer
import com.bioacupunt.patient.domain.model.Patient
import com.bioacupunt.patient.presentation.ProntuarioViewModel

@Composable
fun ProntuarioScreen(
    onBack: (() -> Unit)? = null,
    vm: com.bioacupunt.prontuario.presentation.ProntuarioViewModel = viewModel(factory = AppContainer.prontuarioViewModelFactory),
    patientId: Long = 0L
) {
    vm.load(patientId)
    val state by vm.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") }
            Spacer(Modifier.width(4.dp))
            Text("Prontuário", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(12.dp))

        if (state.loading) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        else if (state.error != null) Text(text = state.error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = state.summary, onValueChange = { vm.updateHeader(summary = it) }, label = { Text("Resumo") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = state.mainComplaint, onValueChange = { vm.updateHeader(mainComplaint = it) }, label = { Text("Queixa principal") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = state.diagnosis, onValueChange = { vm.updateHeader(diagnosis = it) }, label = { Text("Diagnóstico") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = state.treatmentPlan, onValueChange = { vm.updateHeader(treatmentPlan = it) }, label = { Text("Plano") }, modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(12.dp))
        Text("Evoluções", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        if (state.entries.isEmpty()) Text("Sem registros.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(state.entries) { e ->
                Card(elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("${e.date} · ${e.type.label}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text("Dr. ${e.doctorName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(e.body, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
