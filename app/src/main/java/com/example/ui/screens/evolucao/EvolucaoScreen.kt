package com.example.ui.screens.evolucao

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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.TrendingDown
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
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EvolutionRecord(
    val id: String,
    val sessionNumber: Int,
    val date: String,
    val patientName: String,
    val subjective: String,
    val objective: String,
    val assessment: String,
    val plan: String,
    val evaBefore: Int,
    val evaAfter: Int
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun EvolucaoScreen(
    viewModel: MainViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val patients by viewModel.patients.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // Dynamic, interactive list of SOAP records
    val evolutions = remember {
        mutableStateListOf(
            EvolutionRecord(
                id = "ev1",
                sessionNumber = 3,
                date = "23/06/2026",
                patientName = "Maria Souza Silva",
                subjective = "Paciente relata melhora de 80% na cefaleia frontal constante e redução da ansiedade.",
                objective = "Língua: Corpo rosado com saburra fina branca. Pulso: Mais moderado, perda do caráter tenso (em corda).",
                assessment = "Estagnação de Qi do Fígado em resolução progressiva. Baço tonificado.",
                plan = "Manter pontos sistêmicos E36, IG4, F3. Adicionar Yintang para estímulo de ondas cerebrais calmas.",
                evaBefore = 8,
                evaAfter = 2
            ),
            EvolutionRecord(
                id = "ev2",
                sessionNumber = 2,
                date = "16/06/2026",
                patientName = "Maria Souza Silva",
                subjective = "Queixa-se de distensão abdominal leve e cansaço ao final do expediente.",
                objective = "Língua: Bordo ligeiramente avermelhado com marcas de dentes. Pulso: Tenso (Xian).",
                assessment = "Estagnação de Qi do Fígado com leve deficiência do Qi do Baço.",
                plan = "Agulhar F3 (Taichong) para circular o Qi, e BP6 (Sanyinjiao) + E36 (Zusanli) para tonificação.",
                evaBefore = 8,
                evaAfter = 5
            ),
            EvolutionRecord(
                id = "ev3",
                sessionNumber = 1,
                date = "09/06/2026",
                patientName = "João Alencar Ribeiro",
                subjective = "Paciente relata dor lombar aguda que irradia para o membro inferior direito.",
                objective = "Língua: Saburra branca e espessa na base. Pulso: Profundo e lento.",
                assessment = "Obstrução de canais colaterais (estase) por frio/umidade na região lombar.",
                plan = "Moxabustão local em pontos B23 (Shenshu) e B40 (Weizhong). Agulhamento sistêmico R3 (Taixi).",
                evaBefore = 9,
                evaAfter = 4
            )
        )
    }

    if (showAddDialog) {
        var selectedPatientName by remember { mutableStateOf(patients.firstOrNull()?.name ?: "Paciente Avulso") }
        var subjective by remember { mutableStateOf("") }
        var objective by remember { mutableStateOf("") }
        var assessment by remember { mutableStateOf("") }
        var plan by remember { mutableStateOf("") }
        var evaBeforeStr by remember { mutableStateOf("8") }
        var evaAfterStr by remember { mutableStateOf("3") }
        var errorMessage by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nova Evolução Clínica (SOAP)", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (errorMessage.isNotEmpty()) {
                        Text(errorMessage, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    }

                    Text("Selecione o Paciente:", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    if (patients.isEmpty()) {
                        OutlinedTextField(
                            value = selectedPatientName,
                            onValueChange = { selectedPatientName = it },
                            label = { Text("Nome do Paciente", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        ) {
                            LazyColumn {
                                items(patients) { pat ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedPatientName = pat.name }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedPatientName == pat.name,
                                            onClick = { selectedPatientName = pat.name }
                                        )
                                        Text(pat.name, color = TextPrimary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = subjective,
                        onValueChange = { subjective = it },
                        label = { Text("S (Subjetivo) - Queixas, relatos do paciente", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = objective,
                        onValueChange = { objective = it },
                        label = { Text("O (Objetivo) - Língua, Pulso, Sinais físicos", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = assessment,
                        onValueChange = { assessment = it },
                        label = { Text("A (Avaliação) - Padrão MTC, Síndromes", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = plan,
                        onValueChange = { plan = it },
                        label = { Text("P (Plano) - Pontos agulhados, condutas", color = TextSecondary) },
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
                            value = evaBeforeStr,
                            onValueChange = { evaBeforeStr = it },
                            label = { Text("EVA Antes", color = TextSecondary) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = evaAfterStr,
                            onValueChange = { evaAfterStr = it },
                            label = { Text("EVA Depois", color = TextSecondary) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val before = evaBeforeStr.toIntOrNull() ?: 8
                        val after = evaAfterStr.toIntOrNull() ?: 3
                        if (subjective.isBlank() || objective.isBlank() || assessment.isBlank() || plan.isBlank()) {
                            errorMessage = "Todos os campos do SOAP são obrigatórios!"
                        } else {
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
                            val lastSession = evolutions.filter { it.patientName == selectedPatientName }.maxByOrNull { it.sessionNumber }?.sessionNumber ?: 0
                            val record = EvolutionRecord(
                                id = "ev_" + System.currentTimeMillis(),
                                sessionNumber = lastSession + 1,
                                date = dateFormat.format(Date()),
                                patientName = selectedPatientName,
                                subjective = subjective,
                                objective = objective,
                                assessment = assessment,
                                plan = plan,
                                evaBefore = before,
                                evaAfter = after
                            )
                            evolutions.add(0, record)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) {
                    Text("Salvar", color = Color.White)
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
                    rememberSharedContentState(key = "evolucao_container"),
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
                        "Evolução Clínica",
                        color = TextPrimary,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Registro cronológico estruturado por sessão (Método SOAP)",
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
                    Text("Evoluir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(evolutions, key = { it.id }) { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, BorderColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(SwissGreenLight, RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.HistoryEdu,
                                            contentDescription = null,
                                            tint = Gold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = record.patientName,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Sessão #${record.sessionNumber} • ${record.date}",
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Outlined.TrendingDown, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "EVA: ${record.evaBefore} ➔ ${record.evaAfter}",
                                            color = Color(0xFF2E7D32),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(
                                        onClick = { evolutions.remove(record) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Excluir evolução", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = BorderColor)

                            // SOAP Breakdown
                            SoapRow(label = "S (Subjetivo)", text = record.subjective)
                            Spacer(modifier = Modifier.height(4.dp))
                            SoapRow(label = "O (Objetivo)", text = record.objective)
                            Spacer(modifier = Modifier.height(4.dp))
                            SoapRow(label = "A (Diagnóstico/Aval.)", text = record.assessment)
                            Spacer(modifier = Modifier.height(4.dp))
                            SoapRow(label = "P (Conduta/Plano)", text = record.plan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SoapRow(label: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "$label:",
            color = Gold,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
