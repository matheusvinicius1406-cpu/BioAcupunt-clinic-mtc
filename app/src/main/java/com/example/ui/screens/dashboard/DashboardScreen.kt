package com.example.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import com.example.MainViewModel
import com.example.data.MockData
import com.example.data.local.PatientEntity
import com.example.data.local.AppointmentEntity
import com.example.data.local.FinanceEntity
import com.example.ui.components.MetricCard
import com.example.ui.theme.*
import com.example.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigate: (Screen) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val patients by viewModel.patients.collectAsState()
    val appointments by viewModel.appointments.collectAsState()
    val finances by viewModel.finances.collectAsState()
    val clinicaConfig by viewModel.clinicaConfig.collectAsState()

    // Calculate real metrics
    val totalPatients = patients.size
    val scheduledToday = appointments.filter { it.status == "scheduled" }
    val pendingCount = patients.count { it.status == "NEW" }
    
    // Financial metrics
    val totalReceiptsToday = finances.filter { it.type == "receita" }.sumOf { it.amount }
    val totalExpensesToday = finances.filter { it.type == "despesa" }.sumOf { it.amount }
    val dailyBalance = totalReceiptsToday - totalExpensesToday

    // Interactive Clinical Tasks state
    val tasks = remember {
        mutableStateListOf(
            "Revisar prontuário MTC do próximo paciente" to true,
            "Higienizar ventosas clínicas e agulhas sistêmicas" to false,
            "Enviar follow-up de retorno para Maria Souza" to false,
            "Ajustar estoque de moxa e agulhas 0.25x30" to false
        )
    }

    with(sharedTransitionScope) {
        Column(
            modifier = Modifier
                .sharedBounds(
                    rememberSharedContentState(key = "dashboard_container"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
                .fillMaxSize()
                .background(DarkBlue)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Clinic Header & Welcome
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Central Clínica Operacional",
                        color = TextPrimary,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Olá, ${clinicaConfig.profissionalNome} | Hub unificado de atendimento",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Box(
                    modifier = Modifier
                        .background(SwissGreenLight, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "MTC SYSTEM ACTIVE",
                        color = Gold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Executive Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard("Prontuários", "$totalPatients", Gold, Modifier.weight(1f))
                MetricCard("Agendados", "${scheduledToday.size}", Color(0xFF2E7D32), Modifier.weight(1f))
                MetricCard("Pendentes", "$pendingCount", Color(0xFFC62828), Modifier.weight(1f))
                MetricCard("Saldo Diário", "R$ ${"%.2f".format(dailyBalance)}", Color(0xFF1565C0), Modifier.weight(1.2f))
            }

            // IA Clinical Alerts & Smart Insights
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Alertas de IA Clínica & Insights Operacionais",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Dynamic Insights
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("•", color = Gold, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Insight MTC: Ponto E36 (Zusanli) e IG4 (Hegu) recomendados para pacientes com sintomas de deficiência de Qi sob forte estresse profissional.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        if (patients.any { it.status == "STABLE" }) {
                            val stablePat = patients.first { it.status == "STABLE" }
                            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("•", color = Gold, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "CRM Preventivo: ${stablePat.name} está com status estável. Enviar mensagem de follow-up para manutenção preventiva mensal.",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Integrated Agenda do Dia
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Agenda do Dia Integrada",
                            color = Gold,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { onNavigate(Screen.Agenda) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Ver agenda cheia", tint = Gold)
                        }
                    }

                    if (scheduledToday.isEmpty()) {
                        Text(
                            "Nenhuma sessão agendada para hoje.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        scheduledToday.forEach { appt ->
                            val patName = patients.find { it.id == appt.patientId }?.name ?: "Paciente Desconhecido"
                            val timeStr = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(appt.appointmentTime))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SwissWhite, RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("$timeStr - $patName", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(appt.treatmentType, color = TextSecondary, fontSize = 11.sp)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            viewModel.selectPatient(appt.patientId)
                                            onNavigate(Screen.Atendimento)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SwissGreenLight),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Prontuário", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.addAppointment(appt.copy(status = "completed"))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Gold),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Concluir", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Patients Flows (Treatment & Pending & Follow-up CRM)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Pacientes Ativos & Fluxo Clínico",
                        color = Gold,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    patients.forEach { pat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SwissWhite, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pat.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Contato: ${pat.phone}", color = TextSecondary, fontSize = 11.sp)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(SwissGreenLight, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(pat.status.uppercase(), fontSize = 8.sp, color = Gold, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.selectPatient(pat.id)
                                        onNavigate(Screen.Atendimento)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Abrir Prontuário", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Daily Financial Flow & Transactions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fluxo Financeiro Diário",
                            color = Gold,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { onNavigate(Screen.Financeiro) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Ver financeiro", tint = Gold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Recebido hoje:", color = TextSecondary, fontSize = 12.sp)
                        Text("R$ ${"%.2f".format(totalReceiptsToday)}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Pago hoje:", color = TextSecondary, fontSize = 12.sp)
                        Text("R$ ${"%.2f".format(totalExpensesToday)}", color = Color(0xFFC62828), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Clinical Tasks Checklist
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Tarefas Clínicas Diárias",
                        color = Gold,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    tasks.forEachIndexed { index, (task, isChecked) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tasks[index] = task to !isChecked
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { tasks[index] = task to it },
                                colors = CheckboxDefaults.colors(checkedColor = Gold)
                            )
                            Text(
                                text = task,
                                color = if (isChecked) TextSecondary else TextPrimary,
                                fontSize = 12.sp,
                                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }
                    }
                }
            }
        }
    }
}
