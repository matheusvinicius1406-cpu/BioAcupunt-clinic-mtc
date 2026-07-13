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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.premiumShadow
import com.bioacupunt.agenda.domain.model.Appointment
import com.bioacupunt.agenda.domain.model.AppointmentStatus
import com.bioacupunt.agenda.domain.model.AppointmentType
import com.bioacupunt.agenda.presentation.AgendaViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.di.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(viewModel: AgendaViewModel? = null) {
    val vm = viewModel ?: viewModel(factory = com.bioacupunt.di.AppContainer.agendaViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()
    val selectedDate = remember(state.selectedDate) { LocalDate.parse(state.selectedDate) }
    var showNewAppointment by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewAppointment = true },
                containerColor = Primary,
                modifier = Modifier.premiumShadow(shape = MaterialTheme.shapes.large, elevationDp = 18.dp)
            ) { Icon(Icons.Default.Add, null, tint = Color.White) }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            WeekStrip(selectedDate = selectedDate, onDateSelected = { vm.onDateSelected(it.toString()) })
            val dayAppointments = state.appointments
            if (dayAppointments.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EventAvailable, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(12.dp))
                        Text("Nenhuma consulta agendada", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { showNewAppointment = true }) { Text("Agendar agora") }
                    }
                }
            } else {
                val total = dayAppointments.size
                val confirmed = dayAppointments.count { it.status == AppointmentStatus.CONFIRMED.name }
                val revenue = dayAppointments.filter { it.paid }.sumOf { it.valueBrl }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("$total consultas", style = MaterialTheme.typography.labelMedium, color = Primary)
                    Text("$confirmed confirmadas", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64B5F6))
                    Text("R$ %.0f recebidos".format(revenue), style = MaterialTheme.typography.labelMedium, color = Color(0xFF81C784))
                }
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dayAppointments, key = { it.id }) { appt ->
                        AppointmentCard(appt, onStatusChange = { newStatus -> vm.onStatusChange(appt.id, newStatus) })
                    }
                }
            }
        }
    }

    if (showNewAppointment) {
        NewAppointmentDialog(
            onDismiss = { showNewAppointment = false },
            onSave = { patientId, patientName, time, value, type ->
                showNewAppointment = false
                vm.createAppointment(patientId, patientName, time, value, type)
            }
        )
    }
}

@Composable
private fun WeekStrip(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val weekStart = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() - 1)
    val days = (0..6).map { weekStart.plusDays(it.toLong()) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            days.forEach { date ->
                val isSelected = date == selectedDate
                val isToday = date == LocalDate.now()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Primary else Color.Transparent)
                        .clickable { onDateSelected(date) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("pt", "BR")).take(3),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isToday && !isSelected) Primary.copy(alpha = 0.15f) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal),
                            color = if (isSelected) Color.White else if (isToday) Primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        Divider()
    }
}

/** Status changes a doctor can make from a given state. Terminal states (cancelled,
 * completed, no-show) offer none — reopening one is a deliberate re-booking, not a
 * tap on a chip. */
private fun nextStatusOptions(current: AppointmentStatus): List<AppointmentStatus> = when (current) {
    AppointmentStatus.SCHEDULED -> listOf(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED)
    AppointmentStatus.CONFIRMED -> listOf(AppointmentStatus.IN_PROGRESS, AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW)
    AppointmentStatus.IN_PROGRESS -> listOf(AppointmentStatus.COMPLETED)
    else -> emptyList()
}

@Composable
private fun AppointmentCard(appt: Appointment, onStatusChange: (AppointmentStatus) -> Unit) {
    val status = AppointmentStatus.entries.find { it.name == appt.status } ?: AppointmentStatus.SCHEDULED
    val type = AppointmentType.entries.find { it.name == appt.type } ?: AppointmentType.ACUPUNCTURE
    val statusColor = Color(status.color)
    val options = nextStatusOptions(status)
    var menuExpanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.width(52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(appt.time, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Primary))
                Text("${appt.durationMin}min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(modifier = Modifier.padding(horizontal = 8.dp).width(4.dp).height(48.dp).clip(RoundedCornerShape(2.dp)).background(statusColor))
            Column(Modifier.weight(1f)) {
                Text(appt.patientName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(type.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Sessão ${appt.sessionNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Box {
                    SuggestionChip(
                        onClick = { if (options.isNotEmpty()) menuExpanded = true },
                        label = { Text(status.label, style = MaterialTheme.typography.labelSmall) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = statusColor.copy(alpha = 0.1f))
                    )
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    menuExpanded = false
                                    onStatusChange(option)
                                }
                            )
                        }
                    }
                }
                if (appt.valueBrl > 0) Text("R$ %.0f".format(appt.valueBrl), style = MaterialTheme.typography.labelSmall, color = if (appt.paid) Color(0xFF4CAF50) else Color(0xFFFF8A65))
            }
        }
    }
}

@Composable
private fun NewAppointmentDialog(onDismiss: () -> Unit, onSave: (Long, String, String, Double, AppointmentType) -> Unit) {
    var patientIdText by remember { mutableStateOf("") }
    var patientName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AppointmentType.ACUPUNCTURE) }
    var time by remember { mutableStateOf("09:00") }
    var value by remember { mutableStateOf("150") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Consulta", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = patientIdText, onValueChange = { patientIdText = it.filter { ch -> ch.isDigit() } }, label = { Text("ID Paciente") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = patientName, onValueChange = { patientName = it }, label = { Text("Paciente") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Horário (HH:mm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = value, onValueChange = { value = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Valor (R$)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Tipo:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AppointmentType.entries) { type ->
                        FilterChip(selected = selectedType == type, onClick = { selectedType = type }, label = { Text(type.label, style = MaterialTheme.typography.labelSmall) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val pid = patientIdText.toLongOrNull() ?: 0L
                val valueBrl = value.toDoubleOrNull() ?: 0.0
                onSave(pid, patientName, time, valueBrl, selectedType)
            }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
