package com.bioacupunt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.crm.domain.model.CrmPatient
import com.bioacupunt.crm.domain.model.PatientStage
import com.bioacupunt.crm.presentation.CrmViewModel
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.premiumShadow
import kotlinx.coroutines.launch

@Composable
fun CrmScreen(
    onNavigateToProntuario: (Long) -> Unit = {},
    viewModel: CrmViewModel? = null
) {
    val vm = viewModel ?: viewModel(factory = com.bioacupunt.di.AppContainer.crmViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showNewPatient by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val tabs = listOf("Pacientes", "Pipeline", "Relatórios")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Pacientes",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(Primary)
                    .clickable { showNewPatient = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text("Novo", color = Color.White, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            modifier = Modifier.premiumShadow(shape = MaterialTheme.shapes.extraLarge, color = Color.Transparent, elevationDp = 0.dp)
        ) {
            tabs.forEachIndexed { i, t ->
                Tab(
                    selected = selectedTab == i,
                    onClick = { selectedTab = i },
                    text = { Text(t) }
                )
            }
        }

        when (selectedTab) {
            0 -> PatientsListTab(
                patients = state.filteredPatients,
                searchQuery = searchQuery,
                selectedStage = state.selectedStageName,
                onSearch = { query ->
                    searchQuery = query
                    vm.onQueryChanged(query)
                },
                onStageFilter = { stageName -> vm.onStageSelected(stageName) },
                onOpenProntuario = onNavigateToProntuario,
                onDelete = { id -> vm.deletePatient(id) },
                onStatusChange = { id, stage -> vm.updateStage(id, stage) }
            )
            1 -> PipelineTab(
                patients = state.filteredPatients,
                stages = state.stages,
                viewModel = vm
            )
            2 -> CrmReportsTab(summary = state.reportSummary)
        }
    }

    if (showNewPatient) {
        PatientFormDialog(
            patient = null,
            onDismiss = { showNewPatient = false },
            onSave = { name, phone, email, birthDate, notes ->
                vm.createPatient(name, phone, email, birthDate, notes)
                showNewPatient = false
            }
        )
    }

    LaunchedEffect(state.error) {
        val msg = state.error
        if (!msg.isNullOrBlank()) {
            if (context is android.app.Activity) {
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
            vm.clearError()
        }
    }
}

@Composable
private fun PatientFormDialog(
    patient: CrmPatient?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(patient?.name.orEmpty()) }
    var phone by remember { mutableStateOf(patient?.phone.orEmpty()) }
    var email by remember { mutableStateOf(patient?.email.orEmpty()) }
    var birthDate by remember { mutableStateOf(patient?.birthDate.orEmpty()) }
    var notes by remember { mutableStateOf(patient?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (patient == null) "Novo Paciente" else "Editar Paciente", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telefone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = birthDate, onValueChange = { birthDate = it }, label = { Text("Nascimento") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onSave(name.trim(), phone.trim(), email.trim(), birthDate, notes)
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
