package com.example.ui.screens.atendimento

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.data.MtcClinicalEngine
import com.example.data.MtcDiagnosisHypothesis
import com.example.data.local.MtcProntuaryEntity
import com.example.data.local.PatientEntity
import com.example.ui.theme.*
import com.example.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AtendimentoScreen(
    viewModel: MainViewModel,
    onNavigate: (Screen) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val patients by viewModel.patients.collectAsState()
    val selectedPatientId by viewModel.selectedPatientId.collectAsState()
    val activeProntuary by viewModel.activeProntuary.collectAsState()

    // If no patient selected, select first one automatically
    LaunchedEffect(patients) {
        if (selectedPatientId.isEmpty() && patients.isNotEmpty()) {
            viewModel.selectPatient(patients.first().id)
        }
    }

    val selectedPatient = patients.find { it.id == selectedPatientId }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Anamnese", "Sintomas & Shen", "Semiologia MTC", "Raciocínio & Diagnóstico", "Evolução & Anexos", "IA Clínica")

    with(sharedTransitionScope) {
        Column(
            modifier = Modifier
                .sharedBounds(
                    rememberSharedContentState(key = "atendimento_container"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
                .fillMaxSize()
                .background(DarkBlue)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Patient Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Prontuário MTC Supremo",
                        color = TextPrimary,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Raciocínio Clínico e Diagnóstico Energético Integrado",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Dropdown Patient Selector
                var expanded by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedPatient?.name ?: "Selecionar...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(SwissWhite)
                    ) {
                        patients.forEach { pat ->
                            DropdownMenuItem(
                                text = { Text(pat.name, color = TextPrimary) },
                                onClick = {
                                    viewModel.selectPatient(pat.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (selectedPatient == null) {
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.People, contentDescription = null, tint = Gold, modifier = Modifier.size(48.dp))
                            Text("Nenhum paciente cadastrado no sistema.", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { onNavigate(Screen.Pacientes) },
                                colors = ButtonDefaults.buttonColors(containerColor = Gold)
                            ) {
                                Text("Ir para Pacientes", color = Color.White)
                            }
                        }
                    }
                }
            } else {
                // Patient Summary Ribbon
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(selectedPatient.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "Sexo: ${selectedPatient.sex} | Profissão: ${selectedPatient.profession}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(SwissGreenLight, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = selectedPatient.status.uppercase(),
                                color = Gold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Tab Selector
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Gold,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Gold
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 11.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                            selectedContentColor = Gold,
                            unselectedContentColor = TextSecondary
                        )
                    }
                }

                // Active Prontuary State
                val prontuary = activeProntuary ?: MtcProntuaryEntity(selectedPatient.id)

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTab) {
                        0 -> TabAnamnese(prontuary, onSave = { viewModel.saveProntuary(it) })
                        1 -> TabSintomasShen(prontuary, onSave = { viewModel.saveProntuary(it) })
                        2 -> TabSemiologiaMtc(prontuary, onSave = { viewModel.saveProntuary(it) })
                        3 -> TabRaciocinioDiagnostico(prontuary, onSave = { viewModel.saveProntuary(it) })
                        4 -> TabEvolucaoAnexos(selectedPatient, viewModel)
                        5 -> TabIaClinica(prontuary, viewModel)
                    }
                }
            }
        }
    }
}

// ======================== TABS IMPLEMENTATION ========================

@Composable
fun TabAnamnese(prontuary: MtcProntuaryEntity, onSave: (MtcProntuaryEntity) -> Unit) {
    var queixa by remember(prontuary) { mutableStateOf(prontuary.queixaPrincipal) }
    var hist by remember(prontuary) { mutableStateOf(prontuary.historico) }
    var medicamentos by remember(prontuary) { mutableStateOf(prontuary.medicamentos) }
    var estiloVida by remember(prontuary) { mutableStateOf(prontuary.estiloVida) }
    var intensidade by remember(prontuary) { mutableStateOf(prontuary.sintomaIntensidade) }
    var frequencia by remember(prontuary) { mutableStateOf(prontuary.sintomaFrequencia) }
    var duracao by remember(prontuary) { mutableStateOf(prontuary.sintomaDuracao) }
    var gatilhos by remember(prontuary) { mutableStateOf(prontuary.sintomaGatilhos) }
    var padraoTemporal by remember(prontuary) { mutableStateOf(prontuary.sintomaPadraoTemporal) }
    var impactoFuncional by remember(prontuary) { mutableStateOf(prontuary.sintomaImpactoFuncional) }

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Gold,
        unfocusedBorderColor = BorderColor,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Notes, contentDescription = null, tint = Gold)
                    Text("Identificação & Queixas Clínicas", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                OutlinedTextField(
                    value = queixa,
                    onValueChange = {
                        queixa = it
                        onSave(prontuary.copy(queixaPrincipal = it))
                    },
                    label = { Text("Queixa Principal (QP) / Motivo da Consulta", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    colors = tfColors
                )

                OutlinedTextField(
                    value = hist,
                    onValueChange = {
                        hist = it
                        onSave(prontuary.copy(historico = it))
                    },
                    label = { Text("Histórico da Doença Atual (HDA) / Antecedentes", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    colors = tfColors
                )
            }
        }

        // Section: Structured Anamnese
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Gold)
                    Text("Anamnese Estruturada (Não Textual)", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Text("Avaliação multidimensional do sintoma principal:", color = TextSecondary, fontSize = 11.sp)

                // Intensity selector 0..10
                Column {
                    Text("Intensidade do Sintoma ($intensidade / 10):", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (0..10).forEach { num ->
                            val isSelected = intensidade == num
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(
                                        if (isSelected) Gold else SwissWhite,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(1.dp, if (isSelected) Gold else BorderColor, RoundedCornerShape(6.dp))
                                    .clickable {
                                        intensidade = num
                                        onSave(prontuary.copy(sintomaIntensidade = num))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = num.toString(),
                                    color = if (isSelected) Color.White else TextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                SelectionRow(
                    label = "Frequência:",
                    options = listOf("Esporádica", "Semanal", "Diária", "Contínua"),
                    selectedOption = frequencia,
                    onSelected = {
                        frequencia = it
                        onSave(prontuary.copy(sintomaFrequencia = it))
                    }
                )

                SelectionRow(
                    label = "Duração do Quadro:",
                    options = listOf("Dias", "Semanas", "Meses", "Anos"),
                    selectedOption = duracao,
                    onSelected = {
                        duracao = it
                        onSave(prontuary.copy(sintomaDuracao = it))
                    }
                )

                OutlinedTextField(
                    value = gatilhos,
                    onValueChange = {
                        gatilhos = it
                        onSave(prontuary.copy(sintomaGatilhos = it))
                    },
                    label = { Text("Gatilhos (ex: Frio, Estresse, Esforço Físico)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = tfColors
                )

                SelectionRow(
                    label = "Padrão Temporal / Piora:",
                    options = listOf("Sem padrão", "Manhã", "Tarde", "Noite", "Piora Frio", "Piora Calor"),
                    selectedOption = padraoTemporal,
                    onSelected = {
                        padraoTemporal = it
                        onSave(prontuary.copy(sintomaPadraoTemporal = it))
                    }
                )

                SelectionRow(
                    label = "Impacto Funcional nas Atividades:",
                    options = listOf("Nenhum", "Leve", "Moderado", "Grave"),
                    selectedOption = impactoFuncional,
                    onSelected = {
                        impactoFuncional = it
                        onSave(prontuary.copy(sintomaImpactoFuncional = it))
                    }
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Healing, contentDescription = null, tint = Gold)
                    Text("Medicamentos, Terapias & Estilo de Vida", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                OutlinedTextField(
                    value = medicamentos,
                    onValueChange = {
                        medicamentos = it
                        onSave(prontuary.copy(medicamentos = it))
                    },
                    label = { Text("Medicamentos e Terapias em Uso Ativo", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = tfColors
                )

                OutlinedTextField(
                    value = estiloVida,
                    onValueChange = {
                        estiloVida = it
                        onSave(prontuary.copy(estiloVida = it))
                    },
                    label = { Text("Estilo de Vida, Alimentação e Hábitos Geras", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = tfColors
                )
            }
        }

        AutosaveBadge()
    }
}

@Composable
fun TabSintomasShen(prontuary: MtcProntuaryEntity, onSave: (MtcProntuaryEntity) -> Unit) {
    var sFisicos by remember(prontuary) { mutableStateOf(prontuary.sintomasFisicos) }
    var sEmocionais by remember(prontuary) { mutableStateOf(prontuary.sintomasEmocionais) }
    var sono by remember(prontuary) { mutableStateOf(prontuary.sono) }
    var energia by remember(prontuary) { mutableStateOf(prontuary.energiaVital) }
    var digestao by remember(prontuary) { mutableStateOf(prontuary.digestao) }
    
    var dorLoc by remember(prontuary) { mutableStateOf(prontuary.dorLocalizacao) }
    var dorNat by remember(prontuary) { mutableStateOf(prontuary.dorNatureza) }

    var shenBrief by remember(prontuary) { mutableStateOf(prontuary.shen) }
    var ansiedade by remember(prontuary) { mutableStateOf(prontuary.shenAnsiedade) }
    var agitacao by remember(prontuary) { mutableStateOf(prontuary.shenAgitacao) }
    var depressao by remember(prontuary) { mutableStateOf(prontuary.shenDepressao) }
    var clareza by remember(prontuary) { mutableStateOf(prontuary.shenClarezaMental) }
    var estabilidade by remember(prontuary) { mutableStateOf(prontuary.shenEstabilidadeEmocional) }
    var vitalidade by remember(prontuary) { mutableStateOf(prontuary.shenVitalidadeEspiritual) }
    var irritabilidade by remember(prontuary) { mutableStateOf(prontuary.shenIrritabilidade) }

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Gold,
        unfocusedBorderColor = BorderColor,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Anamnese Clínica - Sintomas Gerais", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                OutlinedTextField(
                    value = sFisicos,
                    onValueChange = {
                        sFisicos = it
                        onSave(prontuary.copy(sintomasFisicos = it))
                    },
                    label = { Text("Sintomas Físicos Clássicos (ex: dores, calafrios, transpiração)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = tfColors
                )

                OutlinedTextField(
                    value = sEmocionais,
                    onValueChange = {
                        sEmocionais = it
                        onSave(prontuary.copy(sintomasEmocionais = it))
                    },
                    label = { Text("Sintomas Emocionais / Mudanças de Humor (ex: estresse, raiva)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = tfColors
                )

                SelectionRow("Sono / Repouso:", listOf("Normal", "Insônia", "Agitado", "Suor Noturno"), sono) {
                    sono = it
                    onSave(prontuary.copy(sono = it))
                }

                SelectionRow("Energia Vital:", listOf("Normal", "Alta", "Baixa (Fadiga)", "Fadiga Extrema"), energia) {
                    energia = it
                    onSave(prontuary.copy(energiaVital = it))
                }

                SelectionRow("Digestão / Apetite:", listOf("Excelente", "Normal", "Lenta / Estufado", "Irregular"), digestao) {
                    digestao = it
                    onSave(prontuary.copy(digestao = it))
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Semiologia da Dor Física", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                OutlinedTextField(
                    value = dorLoc,
                    onValueChange = {
                        dorLoc = it
                        onSave(prontuary.copy(dorLocalizacao = it))
                    },
                    label = { Text("Localização da Dor (ex: Lombar, Joelho Esquerdo, Ombros)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = tfColors
                )

                OutlinedTextField(
                    value = dorNat,
                    onValueChange = {
                        dorNat = it
                        onSave(prontuary.copy(dorNatureza = it))
                    },
                    label = { Text("Natureza da Dor (ex: Peso, Facada, Queimação, Fixa, Móvel)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = tfColors
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Avaliação do Shen (Estado Mental & Emocional)", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                OutlinedTextField(
                    value = shenBrief,
                    onValueChange = {
                        shenBrief = it
                        onSave(prontuary.copy(shen = it))
                    },
                    label = { Text("Presença de Espírito / Brilho Ocular", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = tfColors
                )

                SelectionRow("Estabilidade Emocional:", listOf("Estável", "Instável", "Muito Instável"), estabilidade) {
                    estabilidade = it
                    onSave(prontuary.copy(shenEstabilidadeEmocional = it))
                }

                SelectionRow("Vitalidade Espiritual:", listOf("Normal", "Diminuída", "Apagada"), vitalidade) {
                    vitalidade = it
                    onSave(prontuary.copy(shenVitalidadeEspiritual = it))
                }

                SelectionRow("Irritabilidade:", listOf("Ausente", "Leve", "Moderada", "Intensa"), irritabilidade) {
                    irritabilidade = it
                    onSave(prontuary.copy(shenIrritabilidade = it))
                }

                SelectionRow("Ansiedade:", listOf("Ausente", "Leve", "Moderada", "Severa"), ansiedade) {
                    ansiedade = it
                    onSave(prontuary.copy(shenAnsiedade = it))
                }

                SelectionRow("Agitação:", listOf("Ausente", "Leve", "Moderada", "Severa"), agitacao) {
                    agitacao = it
                    onSave(prontuary.copy(shenAgitacao = it))
                }

                SelectionRow("Depressão:", listOf("Ausente", "Leve", "Moderada", "Severa"), depressao) {
                    depressao = it
                    onSave(prontuary.copy(shenDepressao = it))
                }

                SelectionRow("Clareza Mental:", listOf("Normal", "Confusa / Lenta", "Excelente"), clareza) {
                    clareza = it
                    onSave(prontuary.copy(shenClarezaMental = it))
                }
            }
        }

        AutosaveBadge()
    }
}

@Composable
fun TabSemiologiaMtc(prontuary: MtcProntuaryEntity, onSave: (MtcProntuaryEntity) -> Unit) {
    var lCorpo by remember(prontuary) { mutableStateOf(prontuary.linguaCorpo) }
    var lSaburra by remember(prontuary) { mutableStateOf(prontuary.linguaSaburra) }
    var lFormato by remember(prontuary) { mutableStateOf(prontuary.linguaFormato) }
    var lFissuras by remember(prontuary) { mutableStateOf(prontuary.linguaFissuras) }
    var lMarcas by remember(prontuary) { mutableStateOf(prontuary.linguaMarcasDentarias) }
    var lUmidade by remember(prontuary) { mutableStateOf(prontuary.linguaUmidade) }
    var lRegioes by remember(prontuary) { mutableStateOf(prontuary.linguaRegioes) }
    var lSaburraDist by remember(prontuary) { mutableStateOf(prontuary.linguaSaburraDistribuicao) }
    var lBordas by remember(prontuary) { mutableStateOf(prontuary.linguaBordas) }
    var lEvolucao by remember(prontuary) { mutableStateOf(prontuary.linguaEvolucaoTempo) }

    var pQualidade by remember(prontuary) { mutableStateOf(prontuary.pulso) }
    var pProf by remember(prontuary) { mutableStateOf(prontuary.pulsoProfundidade) }
    var pForca by remember(prontuary) { mutableStateOf(prontuary.pulsoForca) }
    var pRitmo by remember(prontuary) { mutableStateOf(prontuary.pulsoRitmo) }
    var pLat by remember(prontuary) { mutableStateOf(prontuary.pulsoLateralidade) }
    var pVelocidade by remember(prontuary) { mutableStateOf(prontuary.pulsoVelocidade) }
    var pQualidadeEnerg by remember(prontuary) { mutableStateOf(prontuary.pulsoQualidadeEnergetica) }
    var pRespPressao by remember(prontuary) { mutableStateOf(prontuary.pulsoRespostaPressao) }

    var zSpleen by remember(prontuary) { mutableStateOf(prontuary.zangFuSpleen) }
    var zLiver by remember(prontuary) { mutableStateOf(prontuary.zangFuLiver) }
    var zKidney by remember(prontuary) { mutableStateOf(prontuary.zangFuKidney) }
    var zHeart by remember(prontuary) { mutableStateOf(prontuary.zangFuHeart) }
    var zLung by remember(prontuary) { mutableStateOf(prontuary.zangFuLung) }
    var zSpleenEst by remember(prontuary) { mutableStateOf(prontuary.zangFuSpleenEstado) }
    var zLiverEst by remember(prontuary) { mutableStateOf(prontuary.zangFuLiverEstado) }
    var zKidneyEst by remember(prontuary) { mutableStateOf(prontuary.zangFuKidneyEstado) }
    var zHeartEst by remember(prontuary) { mutableStateOf(prontuary.zangFuHeartEstado) }
    var zLungEst by remember(prontuary) { mutableStateOf(prontuary.zangFuLungEstado) }

    var cincoElem by remember(prontuary) { mutableStateOf(prontuary.cincoElementos) }
    var woodLvl by remember(prontuary) { mutableStateOf(prontuary.nivelMadeira) }
    var fireLvl by remember(prontuary) { mutableStateOf(prontuary.nivelFogo) }
    var earthLvl by remember(prontuary) { mutableStateOf(prontuary.nivelTerra) }
    var metalLvl by remember(prontuary) { mutableStateOf(prontuary.nivelMetal) }
    var waterLvl by remember(prontuary) { mutableStateOf(prontuary.nivelAgua) }

    var meridianos by remember(prontuary) { mutableStateOf(prontuary.meridianos) }
    var mExcesso by remember(prontuary) { mutableStateOf(prontuary.meridianosExcesso) }
    var mDeficiencia by remember(prontuary) { mutableStateOf(prontuary.meridianosDeficiencia) }
    var mDorTrajeto by remember(prontuary) { mutableStateOf(prontuary.meridianosDorTrajeto) }

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Gold,
        unfocusedBorderColor = BorderColor,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- TONGUE CARD ---
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = Gold)
                    Text("Língua (Inspeção Sistêmica Profunda)", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                OutlinedTextField(
                    value = lCorpo,
                    onValueChange = {
                        lCorpo = it
                        onSave(prontuary.copy(linguaCorpo = it))
                    },
                    label = { Text("Cor do Corpo (ex: Pálido, Vermelho, Roxo, Pálido-Rosado)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = tfColors
                )

                SelectionRow("Regiões Alteradas (Mapa):", listOf("Homogênea", "Ponta (Coração)", "Central (Baço)", "Laterais (Fígado)", "Raiz (Rim)"), lRegioes) {
                    lRegioes = it
                    onSave(prontuary.copy(linguaRegioes = it))
                }

                OutlinedTextField(
                    value = lSaburra,
                    onValueChange = {
                        lSaburra = it
                        onSave(prontuary.copy(linguaSaburra = it))
                    },
                    label = { Text("Aspecto da Saburra (ex: Fina Branca, Espessa Amarela, Sem Saburra)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = tfColors
                )

                SelectionRow("Distribuição da Saburra:", listOf("Geral", "Raiz", "Laterais", "Ausente"), lSaburraDist) {
                    lSaburraDist = it
                    onSave(prontuary.copy(linguaSaburraDistribuicao = it))
                }

                OutlinedTextField(
                    value = lFormato,
                    onValueChange = {
                        lFormato = it
                        onSave(prontuary.copy(linguaFormato = it))
                    },
                    label = { Text("Formato Geral (ex: Inchada, Fina, Tremula, Flácida)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = tfColors
                )

                SelectionRow("Fissuras:", listOf("Nenhuma", "Central", "Laterais", "Difusas"), lFissuras) {
                    lFissuras = it
                    onSave(prontuary.copy(linguaFissuras = it))
                }

                SelectionRow("Bordas:", listOf("Normais", "Vermelhas", "Denteadas", "Inchadas"), lBordas) {
                    lBordas = it
                    onSave(prontuary.copy(linguaBordas = it))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Marcas Dentárias Laterais:", color = TextPrimary, fontSize = 12.sp)
                    Switch(
                        checked = lMarcas,
                        onCheckedChange = {
                            lMarcas = it
                            onSave(prontuary.copy(linguaMarcasDentarias = it))
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = Gold)
                    )
                }

                SelectionRow("Umidade da Língua:", listOf("Normal", "Seca (Calor)", "Muito Úmida (Frio)", "Escorregadia"), lUmidade) {
                    lUmidade = it
                    onSave(prontuary.copy(linguaUmidade = it))
                }

                SelectionRow("Evolução Temporal da Língua:", listOf("Estável", "Melhorando", "Piorando", "Flutuando"), lEvolucao) {
                    lEvolucao = it
                    onSave(prontuary.copy(linguaEvolucaoTempo = it))
                }
            }
        }

        // --- PULSE CARD ---
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Gold)
                    Text("Pulso (Palpação Multidimensional)", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                SelectionRow("Velocidade (Frequência):", listOf("Normal", "Rápido (Shu)", "Lento (Chi)"), pVelocidade) {
                    pVelocidade = it
                    onSave(prontuary.copy(pulsoVelocidade = it))
                }

                SelectionRow("Profundidade:", listOf("Normal", "Superficial (Biao)", "Profundo (Li)"), pProf) {
                    pProf = it
                    onSave(prontuary.copy(pulsoProfundidade = it))
                }

                SelectionRow("Força do Pulso:", listOf("Normal", "Forte (Shi)", "Fraco (Xu)"), pForca) {
                    pForca = it
                    onSave(prontuary.copy(pulsoForca = it))
                }

                SelectionRow("Qualidade Energética (Clássica):", listOf("Moderado", "Corda (Xian)", "Fino (Xi)", "Deslizante (Hua)", "Rugoso (Se)"), pQualidadeEnerg) {
                    pQualidadeEnerg = it
                    onSave(prontuary.copy(pulsoQualidadeEnergetica = it))
                }

                SelectionRow("Resposta à Pressão:", listOf("Normal", "Mantém força", "Desaparece", "Aumenta força"), pRespPressao) {
                    pRespPressao = it
                    onSave(prontuary.copy(pulsoRespostaPressao = it))
                }

                SelectionRow("Ritmo do Batimento:", listOf("Regular", "Irregular (Alterado)"), pRitmo) {
                    pRitmo = it
                    onSave(prontuary.copy(pulsoRitmo = it))
                }

                SelectionRow("Simetria Bilateral:", listOf("Simétrico", "Diferença Esquerdo", "Diferença Direito"), pLat) {
                    pLat = it
                    onSave(prontuary.copy(pulsoLateralidade = it))
                }

                OutlinedTextField(
                    value = pQualidade,
                    onValueChange = {
                        pQualidade = it
                        onSave(prontuary.copy(pulso = it))
                    },
                    label = { Text("Anotações Adicionais do Pulso (Texto Livre)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = tfColors
                )
            }
        }

        // --- ZANG FU CARD ---
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Widgets, contentDescription = null, tint = Gold)
                    Text("Avaliação de Zang Fu (Órgãos Internos)", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Text("Selecione o estado fisiopatológico específico de cada órgão:", color = TextSecondary, fontSize = 11.sp)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Spleen
                    Column {
                        Text("Baço (Pi) — Estado Funcional:", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        SelectionRow("BP Estado:", listOf("Normal", "Deficiência Qi", "Deficiência Yang", "Umidade"), zSpleenEst) {
                            zSpleenEst = it
                            onSave(prontuary.copy(zangFuSpleenEstado = it, zangFuSpleen = it))
                        }
                    }

                    // Liver
                    Column {
                        Text("Fígado (Gan) — Estado Funcional:", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        SelectionRow("F Estado:", listOf("Normal", "Estagnação Qi", "Fogo Subindo", "Def. Sangue"), zLiverEst) {
                            zLiverEst = it
                            onSave(prontuary.copy(zangFuLiverEstado = it, zangFuLiver = it))
                        }
                    }

                    // Kidney
                    Column {
                        Text("Rim (Shen) — Estado Funcional:", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        SelectionRow("R Estado:", listOf("Normal", "Def. Yin", "Def. Yang", "Def. Jing"), zKidneyEst) {
                            zKidneyEst = it
                            onSave(prontuary.copy(zangFuKidneyEstado = it, zangFuKidney = it))
                        }
                    }

                    // Heart
                    Column {
                        Text("Coração (Xin) — Estado Funcional:", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        SelectionRow("C Estado:", listOf("Normal", "Def. Qi/Sangue", "Calor/Fogo", "Estase Sangue"), zHeartEst) {
                            zHeartEst = it
                            onSave(prontuary.copy(zangFuHeartEstado = it, zangFuHeart = it))
                        }
                    }

                    // Lung
                    Column {
                        Text("Pulmão (Fei) — Estado Funcional:", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        SelectionRow("P Estado:", listOf("Normal", "Deficiência Qi", "Secura", "Vento-Calor"), zLungEst) {
                            zLungEst = it
                            onSave(prontuary.copy(zangFuLungEstado = it, zangFuLung = it))
                        }
                    }
                }
            }
        }

        // --- FIVE ELEMENTS DYNAMIC CARD ---
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Gold)
                    Text("Cinco Elementos (Mapa Dinâmico de Energia)", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Text("Arraste os níveis de energia (0 - 100) para modelar o mapa do paciente:", color = TextSecondary, fontSize = 11.sp)

                // Wood
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Madeira (Fígado/VB):", color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("$woodLvl%", color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = woodLvl.toFloat(),
                        onValueChange = {
                            woodLvl = it.toInt()
                            onSave(prontuary.copy(nivelMadeira = it.toInt()))
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF2E7D32), activeTrackColor = Color(0xFF2E7D32))
                    )
                }

                // Fire
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Fogo (Coração/ID):", color = Color(0xFFC62828), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("$fireLvl%", color = Color(0xFFC62828), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = fireLvl.toFloat(),
                        onValueChange = {
                            fireLvl = it.toInt()
                            onSave(prontuary.copy(nivelFogo = it.toInt()))
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFC62828), activeTrackColor = Color(0xFFC62828))
                    )
                }

                // Earth
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Terra (Baço/Estômago):", color = Color(0xFF8D6E63), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("$earthLvl%", color = Color(0xFF8D6E63), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = earthLvl.toFloat(),
                        onValueChange = {
                            earthLvl = it.toInt()
                            onSave(prontuary.copy(nivelTerra = it.toInt()))
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF8D6E63), activeTrackColor = Color(0xFF8D6E63))
                    )
                }

                // Metal
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Metal (Pulmão/IG):", color = Color(0xFF78909C), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("$metalLvl%", color = Color(0xFF78909C), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = metalLvl.toFloat(),
                        onValueChange = {
                            metalLvl = it.toInt()
                            onSave(prontuary.copy(nivelMetal = it.toInt()))
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF78909C), activeTrackColor = Color(0xFF78909C))
                    )
                }

                // Water
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Água (Rim/Bexiga):", color = Color(0xFF1565C0), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("$waterLvl%", color = Color(0xFF1565C0), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = waterLvl.toFloat(),
                        onValueChange = {
                            waterLvl = it.toInt()
                            onSave(prontuary.copy(nivelAgua = it.toInt()))
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF1565C0), activeTrackColor = Color(0xFF1565C0))
                    )
                }

                Text("Elemento Constitucional Predominante:", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val elementos = listOf("Madeira", "Fogo", "Terra", "Metal", "Água")
                    elementos.forEach { elem ->
                        val isSelected = cincoElem == elem
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) Gold else SwissWhite,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(1.dp, if (isSelected) Gold else BorderColor, RoundedCornerShape(8.dp))
                                .clickable {
                                    cincoElem = elem
                                    onSave(prontuary.copy(cincoElementos = elem))
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = elem,
                                color = if (isSelected) Color.White else TextPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- MERIDIANS CARD ---
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.MenuOpen, contentDescription = null, tint = Gold)
                    Text("Fluxo nos Meridianos & Bloqueios", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                OutlinedTextField(
                    value = meridianos,
                    onValueChange = {
                        meridianos = it
                        onSave(prontuary.copy(meridianos = it))
                    },
                    label = { Text("Meridianos Principais Afetados (ex: BP, F, R, VC)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = tfColors
                )

                OutlinedTextField(
                    value = mExcesso,
                    onValueChange = {
                        mExcesso = it
                        onSave(prontuary.copy(meridianosExcesso = it))
                    },
                    label = { Text("Canais em Excesso de Fluxo (Plenitude)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = tfColors
                )

                OutlinedTextField(
                    value = mDeficiencia,
                    onValueChange = {
                        mDeficiencia = it
                        onSave(prontuary.copy(meridianosDeficiencia = it))
                    },
                    label = { Text("Canais em Deficiência de Fluxo (Vazio)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = tfColors
                )

                OutlinedTextField(
                    value = mDorTrajeto,
                    onValueChange = {
                        mDorTrajeto = it
                        onSave(prontuary.copy(meridianosDorTrajeto = it))
                    },
                    label = { Text("Dor ao Longo do Trajeto (ex: Canal da VB lateral da coxa)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = tfColors
                )
            }
        }

        AutosaveBadge()
    }
}

@Composable
fun TabRaciocinioDiagnostico(prontuary: MtcProntuaryEntity, onSave: (MtcProntuaryEntity) -> Unit) {
    var diagManual by remember(prontuary) { mutableStateOf(prontuary.diagnosticoEnergetico) }
    var sindromesManual by remember(prontuary) { mutableStateOf(prontuary.sindromesMtc) }
    var condutaManual by remember(prontuary) { mutableStateOf(prontuary.protocolos) }
    var confianca by remember(prontuary) { mutableStateOf(prontuary.diagnosticoConfianca) }

    var diagnosisGenerated by remember(prontuary) { mutableStateOf(prontuary.diagnosticoEnergetico.isNotBlank()) }
    var isGenerating by remember { mutableStateOf(false) }
    var currentStepText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Gold,
        unfocusedBorderColor = BorderColor,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )

    // Execute pattern correlation engine locally
    val hypothesis = MtcClinicalEngine.analyze(prontuary)
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isGenerating) {
            // High clinical immersion loading screen
            Card(
                colors = CardDefaults.cardColors(containerColor = SwissWhite),
                border = BorderStroke(2.dp, Gold),
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        "Motor de Raciocínio Clínico MTC (v3)",
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    CircularProgressIndicator(color = Gold, strokeWidth = 3.dp)
                    Text(
                        text = currentStepText,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else if (!diagnosisGenerated) {
            // Elegant placeholder card with semiology completeness checklist
            Card(
                colors = CardDefaults.cardColors(containerColor = SwissWhite),
                border = BorderStroke(2.dp, Gold),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Gold)
                        Text(
                            "Apoio à Decisão Clínica MTC (v3)",
                            color = Gold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Text(
                        text = "O assistente clínico inteligente cruzará todos os dados de anamnese, sintomas físicos e emocionais, língua, pulso, dominâncias dos Cinco Elementos e desequilíbrios Zang-Fu para sugerir uma hipótese diagnóstica altamente precisa e um plano terapêutico completo de bioacupuntura.",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Divider(color = BorderColor)

                    Text(
                        "Verificação de Dados Preenchidos para Cruzamento:",
                        color = Gold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val isAnamneseOk = prontuary.queixaPrincipal.isNotBlank() || prontuary.historico.isNotBlank()
                    val isSintomasOk = prontuary.sintomasFisicos.isNotBlank() || prontuary.sintomasEmocionais.isNotBlank()
                    val isLinguaOk = prontuary.linguaCorpo.isNotBlank() || prontuary.linguaSaburra.isNotBlank()
                    val isPulsoOk = prontuary.pulso.isNotBlank() || prontuary.pulsoVelocidade.isNotBlank()
                    val isCincoElemOk = prontuary.cincoElementos.isNotBlank()
                    val isMeridianosOk = prontuary.meridianos.isNotBlank()

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            Triple("Anamnese Estruturada", isAnamneseOk, "Queixa principal e histórico"),
                            Triple("Sintomas & Shen", isSintomasOk, "Sintomas físicos, emocionais e sono"),
                            Triple("Semiologia da Língua", isLinguaOk, "Corpo, saburra, marcas e umidade"),
                            Triple("Qualidades do Pulso", isPulsoOk, "Velocidade, força e profundidade"),
                            Triple("Cinco Elementos", isCincoElemOk, "Equilíbrio das 5 fases de energia"),
                            Triple("Fluxo nos Meridianos", isMeridianosOk, "Excessos, deficiências e dores")
                        ).forEach { (label, isOk, desc) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Circle,
                                        contentDescription = null,
                                        tint = if (isOk) Color(0xFF2E7D32) else TextSecondary.copy(alpha = 0.3f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(label, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                        Text(desc, color = TextSecondary, fontSize = 9.sp)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isOk) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isOk) "Pronto" else "Vazio",
                                        color = if (isOk) Color(0xFF2E7D32) else Color(0xFFE65100),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isGenerating = true
                                currentStepText = "Sincronizando queixas clínicas e histórico do paciente..."
                                kotlinx.coroutines.delay(450)
                                currentStepText = "Analisando semiologia da língua e distribuição de saburra..."
                                kotlinx.coroutines.delay(450)
                                currentStepText = "Correlacionando força, velocidade e profundidade do pulso..."
                                kotlinx.coroutines.delay(450)
                                currentStepText = "Calculando dominâncias nos Cinco Elementos (Zang-Fu)..."
                                kotlinx.coroutines.delay(450)
                                currentStepText = "Mapeando excessos, deficiências e dor no trajeto dos meridianos..."
                                kotlinx.coroutines.delay(450)
                                currentStepText = "Formulando hipóteses diagnósticas de suporte à decisão clínica..."
                                kotlinx.coroutines.delay(450)
                                currentStepText = "Definindo pontos de acupuntura com localização exata e estímulo..."
                                kotlinx.coroutines.delay(400)
                                isGenerating = false
                                diagnosisGenerated = true

                                val updated = prontuary.copy(
                                    diagnosticoEnergetico = hypothesis.primaryPattern,
                                    sindromesMtc = if (hypothesis.secondaryPattern.isNotBlank() && hypothesis.secondaryPattern != "Ausência de co-fatores expressivos") {
                                        "${hypothesis.primaryPattern} / ${hypothesis.secondaryPattern}"
                                    } else {
                                        hypothesis.primaryPattern
                                    },
                                    diagnosticoConfianca = hypothesis.confidence
                                )
                                onSave(updated)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "GERAR DIAGNÓSTICO & CONDUTA MTC",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            // Clinical Pattern Engine Result Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SwissWhite),
                border = BorderStroke(2.dp, Gold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = Gold)
                            Text("Análise de Padrões MTC (Automático)", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    when (hypothesis.confidence) {
                                        "Alta" -> Color(0xFFE8F5E9)
                                        "Média" -> Color(0xFFFFF3E0)
                                        else -> Color(0xFFFFEBEE)
                                    },
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Confiança: ${hypothesis.confidence}",
                                color = when (hypothesis.confidence) {
                                            "Alta" -> Color(0xFF2E7D32)
                                            "Média" -> Color(0xFFE65100)
                                            else -> Color(0xFFC62828)
                                        },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Divider(color = BorderColor)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PADRÃO PRINCIPAL SUGERIDO:", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(hypothesis.primaryPattern, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("FATOR CO-ADJUVANTE:", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(hypothesis.secondaryPattern, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Column {
                        Text("Justificativa Fisiológica MTC:", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(hypothesis.explanation, color = TextPrimary, fontSize = 11.sp, lineHeight = 16.sp)
                    }

                    if (hypothesis.matchedSymptoms.isNotEmpty()) {
                        Column {
                            Text("Sinais Correlacionados Identificados:", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                hypothesis.matchedSymptoms.take(4).forEach { sym ->
                                    Box(
                                        modifier = Modifier
                                            .background(SwissGreenLight, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(sym, color = Gold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Card: Raciocínio Clínico de Apoio à Decisão (Causes, Relations, Goals)
            Card(colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Gold)
                        Text("Raciocínio Clínico de Apoio à Decisão MTC", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Causas Prováveis / Gatilhos do Padrão:", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        hypothesis.probableCauses.forEach { cause ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(4.dp).background(Gold, RoundedCornerShape(2.dp)))
                                Text(cause, color = TextPrimary, fontSize = 11.sp)
                            }
                        }

                        Divider(color = BorderColor.copy(alpha = 0.5f))

                        Text("Relação Órgão-Sintoma Identificada:", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        hypothesis.organSymptomRelations.forEach { relation ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(4.dp).background(Gold, RoundedCornerShape(2.dp)))
                                Text(relation, color = TextPrimary, fontSize = 11.sp)
                            }
                        }

                        Divider(color = BorderColor.copy(alpha = 0.5f))

                        Text("Objetivos Terapêuticos do Plano de Conduta:", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        hypothesis.therapeuticGoals.forEach { goal ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(4.dp).background(Gold, RoundedCornerShape(2.dp)))
                                Text(goal, color = TextPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Differential Diagnosis Card
            Card(colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Gold)
                        Text("Diferenciação de Síndromes Clássicas", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text("Consulte as diferenças essenciais para validar sua hipótese clínica:", color = TextSecondary, fontSize = 11.sp)

                    hypothesis.differentiationTips.forEach { tip ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("•", color = Gold, fontWeight = FontWeight.Bold)
                                Text(tip.patternName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("(${tip.keySymptom})", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(tip.comparison, color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            }

            // Suggested Therapies Card (Click to import)
            Card(colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Conduta Terapêutica Sugerida", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Toque nos botões para copiar diretamente para a sua conduta final editável.", color = TextSecondary, fontSize = 11.sp)

                    // Acupuncture point suggestions
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Acupuntura Sistêmica Recomendada:", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        hypothesis.acupuncturePoints.forEach { pt ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SwissWhite, RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text("${pt.code} (${pt.name}) — ${pt.action}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(pt.rationale, color = TextSecondary, fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("📍 Localização: ${pt.location}", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text("💉 Inserção: ${pt.insertion}", color = TextSecondary, fontSize = 10.sp)
                            }
                        }

                        Button(
                            onClick = {
                                val importedText = hypothesis.acupuncturePoints.joinToString("\n") { pt ->
                                    "- ${pt.code} (${pt.name}): ${pt.action}\n  * Localização: ${pt.location}\n  * Estímulo: ${pt.insertion}\n  * Justificativa: ${pt.rationale}"
                                }
                                condutaManual = if (condutaManual.isBlank()) importedText else "$condutaManual\n\n$importedText"
                                onSave(prontuary.copy(protocolos = condutaManual))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SwissGreenLight),
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("IMPORTAR PONTOS PARA A CONDUTA", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(color = BorderColor)

                    // Auriculoterapia, Ventosa and Tuina
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Auriculoterapia Recomendada:", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(hypothesis.auriculotherapy.joinToString(" • "), color = TextPrimary, fontSize = 11.sp)
                        Button(
                            onClick = {
                                val aurText = "Auriculoterapia sugerida: ${hypothesis.auriculotherapy.joinToString(", ")}"
                                condutaManual = if (condutaManual.isBlank()) aurText else "$condutaManual\n\n$aurText"
                                onSave(prontuary.copy(protocolos = condutaManual))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SwissGreenLight),
                            modifier = Modifier.fillMaxWidth().height(28.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("IMPORTAR AURICULO", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(color = BorderColor)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Ventosaterapia Recomendada:", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(hypothesis.ventosaterapia, color = TextPrimary, fontSize = 11.sp)
                        Button(
                            onClick = {
                                val ventText = "Ventosaterapia sugerida: ${hypothesis.ventosaterapia}"
                                condutaManual = if (condutaManual.isBlank()) ventText else "$condutaManual\n\n$ventText"
                                onSave(prontuary.copy(protocolos = condutaManual))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SwissGreenLight),
                            modifier = Modifier.fillMaxWidth().height(28.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("IMPORTAR VENTOSA", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(color = BorderColor)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Tuina Recomendado:", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(hypothesis.tuina, color = TextPrimary, fontSize = 11.sp)
                        Button(
                            onClick = {
                                val tuinaText = "Tuina sugerido: ${hypothesis.tuina}"
                                condutaManual = if (condutaManual.isBlank()) tuinaText else "$condutaManual\n\n$tuinaText"
                                onSave(prontuary.copy(protocolos = condutaManual))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SwissGreenLight),
                            modifier = Modifier.fillMaxWidth().height(28.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("IMPORTAR TUINA", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- SISTEMA DE SEGURANÇA E ALERTAS CLÍNICOS ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                border = BorderStroke(2.dp, Color(0xFFE65100)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = "Aviso de Segurança", tint = Color(0xFFE65100))
                        Text("ALERTAS CLÍNICOS E SEGURANÇA (v3)", color = Color(0xFFE65100), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(
                        text = "Contraindicações do Plano de Tratamento:\n${hypothesis.contraindications}",
                        color = Color(0xFF5D4037),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    if (prontuary.sintomaIntensidade >= 8) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFEBEE), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠ ALERTA DE INTENSIDADE SEVERA: Sintoma relatado com intensidade ${prontuary.sintomaIntensidade}/10. Monitore sinais de sofrimento sistêmico agudo.",
                                color = Color(0xFFC62828),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // --- BIBLIOTECA DE INTEGRAÇÃO & CANAL DE ESTUDO MTC ---
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, Gold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Book, contentDescription = "Estudo Clínico", tint = Gold)
                        Text("Biblioteca de Integração & Estudo Ativo MTC", color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Text(
                        text = "Gerações automáticas baseadas no padrão clínico ativo para enriquecer seu repertório teórico-prático:",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    if (hypothesis.studyTopics.isNotEmpty()) {
                        Text("📚 Tópicos de Estudo Personalizados:", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        hypothesis.studyTopics.forEach { topic ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SwissWhite, RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(topic.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(topic.description, color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
                            }
                        }
                    }

                    if (hypothesis.automaticFlashcards.isNotEmpty()) {
                        Text("🃏 Flashcards de Memorização Rápida (Toque para Revelar):", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            hypothesis.automaticFlashcards.forEach { card ->
                                var showAnswer by remember { mutableStateOf(false) }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (showAnswer) Color(0xFFE8F5E9) else Color(0xFFECEFF1)),
                                    border = BorderStroke(1.dp, if (showAnswer) Color(0xFF81C784) else Color(0xFFB0BEC5)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showAnswer = !showAnswer }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp).fillMaxWidth().heightIn(min = 60.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (showAnswer) "VERSO (RESPOSTA):" else "FRENTE (PERGUNTA):",
                                            color = if (showAnswer) Color(0xFF2E7D32) else Color(0xFF546E7A),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (showAnswer) card.back else card.front,
                                            color = TextPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (hypothesis.similarCases.isNotEmpty()) {
                        Text("👥 Casos Clínicos Similares da Base (Estudo de Caso):", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        hypothesis.similarCases.forEach { simCase ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SwissWhite, RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(simCase.patientAgeGender, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("Caso de Sucesso", color = Color(0xFF2E7D32), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("Queixa: ${simCase.complaint}", color = TextSecondary, fontSize = 10.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                Text("Conduta: ${simCase.treatmentSuccess}", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // Manual Professional Overwrites
            Card(colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Laudo Clínico & Registro do Profissional", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(
                        value = diagManual,
                        onValueChange = {
                            diagManual = it
                            onSave(prontuary.copy(diagnosticoEnergetico = it))
                        },
                        label = { Text("Diagnóstico Energético Definitivo (ex: Estagnação de Qi do Fígado)", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        colors = tfColors
                    )

                    OutlinedTextField(
                        value = sindromesManual,
                        onValueChange = {
                            sindromesManual = it
                            onSave(prontuary.copy(sindromesMtc = it))
                        },
                        label = { Text("Síndromes MTC Diagnosticadas", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        colors = tfColors
                    )

                    OutlinedTextField(
                        value = condutaManual,
                        onValueChange = {
                            condutaManual = it
                            onSave(prontuary.copy(protocolos = it))
                        },
                        label = { Text("Prescrição de Pontos & Conduta Clínica Aplicada", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        colors = tfColors
                    )

                    SelectionRow("Sua Confiança Diagnóstica:", listOf("Baixa", "Média", "Alta"), confianca) {
                        confianca = it
                        onSave(prontuary.copy(diagnosticoConfianca = it))
                    }

                    Button(
                        onClick = {
                            onSave(prontuary.copy(
                                diagnosticoEnergetico = diagManual,
                                sindromesMtc = sindromesManual,
                                protocolos = condutaManual,
                                diagnosticoConfianca = confianca
                            ))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SALVAR PRONTUÁRIO MTC COMPLETO", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- RELATÓRIO CLÍNICO CONSOLIDADO PARA A DRA. CAMILA (v3) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = SwissWhite),
                border = BorderStroke(2.dp, Gold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Assignment, contentDescription = null, tint = Gold)
                        Text("Relatório Final Consolidado (Dra. Camila)", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text(
                        text = "Documento clínico final contendo todo o histórico estruturado do paciente, semiologia e prescrição final recomendada pelo motor v3:",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    // Generating report string
                    val reportText = """
                        ==================================================
                          PRONTUÁRIO CLÍNICO MTC - RELATÓRIO CONSOLIDADO
                        ==================================================
                        Dra. Camila - Acupuntura & Bioacupuntura MTC
                        
                        [1] IDENTIFICAÇÃO E QUEIXAS CLÍNICAS
                        - Queixa Principal: ${prontuary.queixaPrincipal}
                        - Histórico: ${prontuary.historico}
                        
                        [2] ANAMNESE ESTRUTURADA (v3)
                        - Intensidade: ${prontuary.sintomaIntensidade}/10
                        - Frequência: ${prontuary.sintomaFrequencia}
                        - Duração: ${prontuary.sintomaDuracao}
                        - Gatilhos: ${prontuary.sintomaGatilhos}
                        - Padrão Temporal: ${prontuary.sintomaPadraoTemporal}
                        - Impacto Funcional: ${prontuary.sintomaImpactoFuncional}
                        - Medicamentos: ${prontuary.medicamentos}
                        - Estilo de Vida: ${prontuary.estiloVida}
                        
                        [3] SEMIOLOGIA MTC
                        - Língua: Corpo ${prontuary.linguaCorpo}, Saburra ${prontuary.linguaSaburra}, Formato ${prontuary.linguaFormato}, Umidade ${prontuary.linguaUmidade}, Regiões: ${prontuary.linguaRegioes}
                        - Pulso: Qualidade ${prontuary.pulso}, Velocidade ${prontuary.pulsoVelocidade}, Profundidade ${prontuary.pulsoProfundidade}, Força ${prontuary.pulsoForca}, Qualidade Energética ${prontuary.pulsoQualidadeEnergetica}
                        - Cinco Elementos: Predominância ${prontuary.cincoElementos} (Madeira: ${prontuary.nivelMadeira}%, Fogo: ${prontuary.nivelFogo}%, Terra: ${prontuary.nivelTerra}%, Metal: ${prontuary.nivelMetal}%, Água: ${prontuary.nivelAgua}%)
                        - Meridianos Afetados: ${prontuary.meridianos} (Excesso: ${prontuary.meridianosExcesso}, Deficiência: ${prontuary.meridianosDeficiencia}, Dor: ${prontuary.meridianosDorTrajeto})
                        
                        [4] DIAGNÓSTICO ENERGÉTICO (MOTOR CLÍNICO v3)
                        - Padrão Principal Sugerido: ${hypothesis.primaryPattern}
                        - Padrão Secundário Sugerido: ${hypothesis.secondaryPattern}
                        - Confiança Diagnóstica do Motor: ${hypothesis.confidence}
                        - Justificativa Fisiológica: ${hypothesis.explanation}
                        - Diagnóstico Definitivo (Profissional): ${diagManual}
                        - Síndromes Clínicas MTC: ${sindromesManual}
                        
                        [5] PLANO TERAPÊUTICO INTEGRADO PROPOSTO
                        - Objetivos Terapêuticos:
                        ${hypothesis.therapeuticGoals.joinToString("\n") { "  • $it" }}
                        - Pontos de Acupuntura Sistêmica:
                        ${hypothesis.acupuncturePoints.joinToString("\n") { "  • ${it.code} (${it.name}): ${it.action}\n    - Localização: ${it.location}\n    - Estímulo: ${it.insertion}" }}
                        - Auriculoterapia: ${hypothesis.auriculotherapy.joinToString(", ")}
                        - Ventosaterapia: ${hypothesis.ventosaterapia}
                        - Tuina: ${hypothesis.tuina}
                        
                        [6] ALERTAS CLÍNICOS E SEGURANÇA
                        - Contraindicações e Cuidados: ${hypothesis.contraindications}
                        
                        ==================================================
                        Relatório gerado automaticamente pelo Sistema de Apoio à Decisão MTC v3.
                    """.trimIndent()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(SwissWhite, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(10.dp)
                    ) {
                        Text(
                            text = reportText,
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }

                    Button(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(reportText))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("COPIAR RELATÓRIO COMPLETO", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        AutosaveBadge()
    }
}

@Composable
fun TabEvolucaoAnexos(patient: PatientEntity, viewModel: MainViewModel) {
    val appointments by viewModel.appointments.collectAsState()
    val patientSess = appointments.filter { it.patientId == patient.id }

    var attachmentTitle by remember { mutableStateOf("") }
    val mockAttachments = remember {
        mutableStateListOf(
            "Anamnese_Fisica_Frente.jpg" to "23/06/2026",
            "Exame_Sangue_Geral.pdf" to "16/06/2026"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Evolution timeline
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Histórico Evolutivo Clínico (Linha do Tempo)", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                if (patientSess.isEmpty()) {
                    Text("Nenhuma sessão registrada para este paciente ainda.", color = TextSecondary, fontSize = 12.sp)
                } else {
                    patientSess.sortedByDescending { it.appointmentTime }.forEachIndexed { idx, appt ->
                        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date(appt.appointmentTime))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SwissWhite, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.HistoryEdu, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Consulta #${patientSess.size - idx} — ${appt.treatmentType}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(dateStr, color = TextSecondary, fontSize = 11.sp)
                                    if (appt.notes.isNotEmpty()) {
                                        Text(appt.notes, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .background(if (appt.status == "completed") SwissGreenLight else BorderColor, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(appt.status.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (appt.status == "completed") Gold else TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // Image attachments / clinical archives
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Anexos de Exames & Fotos de Língua", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                mockAttachments.forEach { (name, date) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SwissWhite, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.AttachFile, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
                            Column {
                                textNameWithExtension(name)
                                Text("Anexado em: $date", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                        IconButton(
                            onClick = { mockAttachments.remove(name to date) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = attachmentTitle,
                        onValueChange = { attachmentTitle = it },
                        label = { Text("Nome do Documento ou Foto", color = TextSecondary) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Button(
                        onClick = {
                            if (attachmentTitle.isNotBlank()) {
                                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
                                mockAttachments.add(attachmentTitle to sdf.format(Date()))
                                attachmentTitle = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold)
                    ) {
                        Text("Anexar", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun TabIaClinica(prontuary: MtcProntuaryEntity, viewModel: MainViewModel) {
    val geminiResponse by viewModel.geminiResponse.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, BorderColor)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = Gold)
                    Text("IA Assistente & Gerador de Relatório MTC", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Text(
                    "Esta inteligência artificial analisa simultaneamente todos os campos clínicos deste prontuário avançado, recomendando correlações sinérgicas de acupuntura, auriculoterapia, ventosaterapia e gerando um relatório completo para a Dra. Camila.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Button(
                    onClick = {
                        val fullPrompt = """
                            Você é o Assistente Clínico da Clínica BioAcupunt. 
                            Analise o seguinte Prontuário MTC de forma extremamente aprofundada:
                            - Queixa Principal: ${prontuary.queixaPrincipal}
                            - Histórico de Doença (HDA): ${prontuary.historico}
                            - Sintomas Físicos: ${prontuary.sintomasFisicos}
                            - Sintomas Emocionais: ${prontuary.sintomasEmocionais}
                            - Sono: ${prontuary.sono} | Energia: ${prontuary.energiaVital} | Digestão: ${prontuary.digestao}
                            - Dor Física: Localização (${prontuary.dorLocalizacao}), Natureza (${prontuary.dorNatureza})
                            - Medicamentos em uso: ${prontuary.medicamentos} | Estilo de Vida: ${prontuary.estiloVida}
                            - Semiotécnica Lingual: Corpo (${prontuary.linguaCorpo}), Saburra (${prontuary.linguaSaburra}), Formato (${prontuary.linguaFormato}), Fissuras (${prontuary.linguaFissuras}), Marcas Dentárias (${prontuary.linguaMarcasDentarias}), Umidade (${prontuary.linguaUmidade})
                            - Semiotécnica do Pulso: Qualidade (${prontuary.pulso}), Profundidade (${prontuary.pulsoProfundidade}), Força (${prontuary.pulsoForca}), Ritmo (${prontuary.pulsoRitmo}), Lateralidade (${prontuary.pulsoLateralidade})
                            - Estado do Shen: ${prontuary.shen} | Ansiedade: ${prontuary.shenAnsiedade} | Agitação: ${prontuary.shenAgitacao}
                            - Zang Fu (Spleen, Liver, Kidney, Heart, Lung): ${prontuary.zangFuSpleen}, ${prontuary.zangFuLiver}, ${prontuary.zangFuKidney}, ${prontuary.zangFuHeart}, ${prontuary.zangFuLung}
                            - Elemento Ativo: ${prontuary.cincoElementos} | Meridianos: ${prontuary.meridianos}

                            Forneça um laudo assistivo estruturado contendo:
                            1. SÍNDROME MTC PRINCIPAL E SECUNDÁRIA JUSTIFICADA PELOS SINAIS.
                            2. PRINCÍPIO TERAPÊUTICO MTC (ex: Harmonizar o Fígado, Aquecer o Yang, etc.).
                            3. PRESCRIÇÃO SINÉRGICA DE 5 PONTOS DE ACUPUNTURA COM SUAS DEVIDAS JUSTIFICATIVAS CLÁSSICAS.
                            4. RECOMENDAÇÃO DE AURICULOTERAPIA, VENTOSATERAPIA E DIETOTERAPIA CHINESA.
                            5. RELATÓRIO OPERACIONAL FINAL DE SUPORTE À DRA. CAMILA.
                        """.trimIndent()
                        viewModel.consultarGemini(fullPrompt)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Psychology, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("GERAR RELATÓRIO INTEGRADO COM IA", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (geminiResponse.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SwissWhite),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                            Text("Relatório Gerado com Sucesso:", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        IconButton(
                            onClick = { viewModel.consultarGemini("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Recarregar", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }

                    Text(
                        text = geminiResponse,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// ======================== SHARED HELPER COMPOSABLES ========================

@Composable
fun SelectionRow(
    label: String,
    options: List<String>,
    selectedOption: String,
    onSelected: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { opt ->
                val isSelected = selectedOption == opt
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) Gold else SwissWhite,
                            RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, if (isSelected) Gold else BorderColor, RoundedCornerShape(8.dp))
                        .clickable { onSelected(opt) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt,
                        color = if (isSelected) Color.White else TextPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AutosaveBadge() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CloudQueue,
            contentDescription = null,
            tint = Color(0xFF2E7D32),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Autosave ativo — Prontuário persistido no Room local.",
            color = Color(0xFF2E7D32),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun textNameWithExtension(name: String) {
    Text(name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
}
