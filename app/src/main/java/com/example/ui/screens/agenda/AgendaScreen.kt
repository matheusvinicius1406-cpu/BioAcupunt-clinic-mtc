package com.example.ui.screens.agenda

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import com.example.MainViewModel
import com.example.data.local.AppointmentEntity
import com.example.data.local.PatientEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AgendaScreen(
    viewModel: MainViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val appointments by viewModel.appointments.collectAsState()
    val patients by viewModel.patients.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val timeFormat = SimpleDateFormat("HH:mm", Locale("pt", "BR"))
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    if (showAddDialog) {
        var selectedPatientId by remember { mutableStateOf(patients.firstOrNull()?.id ?: "") }
        var type by remember { mutableStateOf("Acupuntura Sistêmica") }
        var durationStr by remember { mutableStateOf("50") }
        var timeOffsetHourStr by remember { mutableStateOf("2") } // In how many hours
        var notes by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Agendar Nova Consulta", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (errorMessage.isNotEmpty()) {
                        Text(errorMessage, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    }

                    Text("Selecione o Paciente:", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    if (patients.isEmpty()) {
                        Text("Por favor, cadastre um paciente primeiro no CRM.", color = Color.Red, fontSize = 12.sp)
                    } else {
                        // Display option list
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                patients.forEach { pat ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedPatientId = pat.id }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedPatientId == pat.id,
                                            onClick = { selectedPatientId = pat.id }
                                        )
                                        Text(pat.name, color = TextPrimary, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Tipo de Atendimento", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = durationStr,
                            onValueChange = { durationStr = it },
                            label = { Text("Duração (Min)", color = TextSecondary) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = timeOffsetHourStr,
                            onValueChange = { timeOffsetHourStr = it },
                            label = { Text("Em quantas horas", color = TextSecondary) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Conduta / Notas Iniciais", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val duration = durationStr.toIntOrNull() ?: 50
                        val offsetHours = timeOffsetHourStr.toDoubleOrNull() ?: 2.0
                        if (selectedPatientId.isEmpty()) {
                            errorMessage = "Selecione um paciente!"
                        } else {
                            val newAppt = AppointmentEntity(
                                id = "a_" + System.currentTimeMillis(),
                                patientId = selectedPatientId,
                                appointmentTime = System.currentTimeMillis() + (offsetHours * 60 * 60 * 1000).toLong(),
                                duration = duration,
                                status = "scheduled",
                                treatmentType = type,
                                notes = notes
                            )
                            viewModel.addAppointment(newAppt)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    enabled = patients.isNotEmpty()
                ) {
                    Text("Confirmar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SwissWhite
        )
    }

    with(sharedTransitionScope) {
        Column(
            modifier = Modifier
                .sharedBounds(
                    rememberSharedContentState(key = "agenda_container"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
                .fillMaxSize()
                .background(DarkBlue)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Agenda de Consultas",
                        color = TextPrimary,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Sessões agendadas e controle de retornos de pacientes",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agendar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            if (appointments.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Sem consultas agendadas.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(appointments) { appt ->
                        val patient = patients.find { it.id == appt.patientId }
                        val patientName = patient?.name ?: "Paciente Desconhecido"
                        val isScheduled = appt.status == "scheduled"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            border = BorderStroke(1.dp, BorderColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AccessTime,
                                            contentDescription = null,
                                            tint = Gold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = timeFormat.format(Date(appt.appointmentTime)),
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(SwissGreenLight, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${appt.duration} MIN",
                                                color = Gold,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isScheduled) SwissGreenLight else BorderColor,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    if (isScheduled) {
                                                        // Mark as completed
                                                        val updated = appt.copy(status = "completed")
                                                        viewModel.addAppointment(updated)
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (isScheduled) "CONFIRMADO" else "CONCLUÍDO",
                                                color = if (isScheduled) Gold else TextSecondary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteAppointment(appt.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Excluir Consulta",
                                                tint = Color.Red.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = patientName,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                Text(
                                    text = "Procedimento: ${appt.treatmentType}",
                                    color = Gold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )

                                if (appt.notes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Conduta Clín./Notas:",
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = appt.notes,
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
