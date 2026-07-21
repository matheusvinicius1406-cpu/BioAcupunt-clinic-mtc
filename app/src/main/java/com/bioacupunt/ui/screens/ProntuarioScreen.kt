package com.bioacupunt.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.di.AppContainer
import com.bioacupunt.crm.domain.model.CrmPatient
import com.bioacupunt.prontuario.domain.model.*
import com.bioacupunt.prontuario.presentation.ExameViewModel
import com.bioacupunt.prontuario.presentation.ProntuarioViewModel
import com.bioacupunt.prontuario.presentation.SupremoViewModel
import com.bioacupunt.ui.components.ClinicalSafetyPanel
import com.bioacupunt.ui.design.AxisSelector
import com.bioacupunt.ui.design.CompletenessBar
import com.bioacupunt.ui.design.SectionHeader
import com.bioacupunt.ui.design.SelectableChip
import com.bioacupunt.ui.design.SupremoCard
import com.bioacupunt.ui.theme.Accent
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.SemanticError
import com.bioacupunt.ui.theme.SemanticSuccess
import com.bioacupunt.ui.theme.SemanticWarning
import com.bioacupunt.ui.theme.TextMuted
import kotlinx.coroutines.launch

private enum class ProntTab(val label: String) {
    RESUMO("Resumo"), ANAMNESE("Anamnese"), PLANO("Plano"),
    EXAMES("Exames"), EVOLUCAO("Evolução"), DOCUMENTOS("Documentos"),
}

/**
 * PRONTUÁRIO — tela única, 6 abas, seguindo o mockup de referência.
 *
 * O veredito de segurança (R1 — motor determinístico, sem LLM) fica **acima das
 * abas**, sempre visível, em vez de trancado numa aba "Segurança" que dá pra pular.
 * Isso cumpre a mesma regra de "aviso antes do plano" do CLAUDE.md de um jeito mais
 * forte que uma aba: não tem como não ver.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProntuarioScreen(
    onBack: (() -> Unit)? = null,
    onOpenEvolucao: (Long) -> Unit = {},
    onOpenAtendimento: () -> Unit = {},
    vm: ProntuarioViewModel = viewModel(factory = AppContainer.prontuarioViewModelFactory),
    patientId: Long = 0L
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showPatientPicker by remember { mutableStateOf(patientId <= 0L) }
    var showSessionDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<ProntuarioEntry?>(null) }
    var confirmDelete by remember { mutableStateOf<ProntuarioEntry?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Reads `crm_patients` — the patient registry every clinical foreign key
    // points at — NOT the legacy `patients` table.
    //
    // This screen used to list the legacy table while the chart it saves is keyed
    // on the CRM one. The two tables both autoincrement from 1, so the ids
    // *collide without matching*: picking the legacy patient #1 saved a chart
    // under CRM patient #1, a different person. The foreign key was satisfied, so
    // nothing failed and nothing warned — the chart simply filed itself under the
    // wrong patient's name. Silent, and the worst possible outcome here.
    var allPatients by remember { mutableStateOf<List<CrmPatient>>(emptyList()) }
    LaunchedEffect(Unit) {
        scope.launch { AppContainer.crmPatientRepository.observeAll().collect { allPatients = it } }
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
                title = { Text("Prontuário") },
                navigationIcon = {
                    if (onBack != null && state.patientId > 0L) {
                        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") }
                    }
                },
            )
        }
    ) { padding ->
        if (showPatientPicker || state.patientId <= 0L) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
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
                                        if (p.phone.isNotBlank()) Text(p.phone, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            return@Scaffold
        }

        val patientName = remember(allPatients, state.patientId) {
            allPatients.firstOrNull { it.id == state.patientId }?.name ?: "Paciente"
        }
        val supremoVm: SupremoViewModel = viewModel(
            key = "supremo-${state.patientId}",
            factory = AppContainer.supremoViewModelFactory(state.patientId),
        )
        val exameVm: ExameViewModel = viewModel(
            key = "exame-${state.patientId}",
            factory = AppContainer.exameViewModelFactory(state.patientId),
        )
        val supremoState by supremoVm.state.collectAsStateWithLifecycle()

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            state.error?.let { Text(it, color = SemanticError, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall) }

            // ── Patient header ──────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(Primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initialsOfPatient(patientName), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Column(Modifier.weight(1f)) {
                    Text(patientName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text(
                        "Prontuário estruturado · ${(supremoState.completeness * 100).toInt()}% completo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onOpenAtendimento)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        "Atender",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                }
            }

            // ── Safety alerts — always visible, before any tab content ──
            val alertFindings = supremoState.verdict.findings
            if (alertFindings.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    alertFindings.forEach { finding ->
                        val tint = when (finding.severity) {
                            com.bioacupunt.prontuario.domain.safety.Severity.FORBIDDEN -> SemanticError
                            com.bioacupunt.prontuario.domain.safety.Severity.CAUTION -> SemanticWarning
                            com.bioacupunt.prontuario.domain.safety.Severity.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Row(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraLarge)
                                .background(tint.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(Icons.Default.Warning, null, tint = tint, modifier = Modifier.size(15.dp))
                            Text(finding.title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = tint)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 16.dp) {
                ProntTab.entries.forEachIndexed { index, item ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(item.label) })
                }
            }

            when (ProntTab.entries[tab]) {
                ProntTab.RESUMO -> ResumoTab(state, onUpdate = vm::updateHeader, onOpenAnamnese = { tab = ProntTab.ANAMNESE.ordinal }, onOpenEvolucao = { onOpenEvolucao(state.patientId) })
                ProntTab.ANAMNESE -> AnamneseTab(supremoVm)
                ProntTab.PLANO -> PlanoTab(state, supremoState, onUpdate = vm::updateHeader)
                ProntTab.EXAMES -> ExamesTab(exameVm)
                ProntTab.EVOLUCAO -> EvolucaoTab(
                    entries = state.entries,
                    onAdd = { editingEntry = null; showSessionDialog = true },
                    onEdit = { e -> editingEntry = e; showSessionDialog = true },
                    onDelete = { e -> confirmDelete = e },
                )
                ProntTab.DOCUMENTOS -> DocumentosTab(exameVm)
            }
        }
    }

    if (showSessionDialog) {
        SessionFormDialog(
            entry = editingEntry,
            onDismiss = { showSessionDialog = false; editingEntry = null },
            onSave = { type, body ->
                showSessionDialog = false
                val id = editingEntry?.id
                editingEntry = null
                if (id == null) vm.addSession(body, type) else vm.updateEntry(id, type, body)
            }
        )
    }

    confirmDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Remover registro?") },
            text = { Text("Essa ação não pode ser desfeita.") },
            confirmButton = { TextButton(onClick = { vm.deleteSession(entry.id); confirmDelete = null }) { Text("Remover", color = SemanticError) } },
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

private fun initialsOfPatient(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

// ── RESUMO ──────────────────────────────────────────────────────────────

@Composable
private fun ResumoTab(
    state: com.bioacupunt.prontuario.presentation.ProntuarioUiState,
    onUpdate: (String?, String?, String?, String?) -> Unit,
    onOpenAnamnese: () -> Unit,
    onOpenEvolucao: () -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth().clickable(onClick = onOpenEvolucao), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Evolução Clínica", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = Primary))
                Icon(Icons.Default.ChevronRight, null, tint = Primary)
            }
        }
        item {
            SupremoCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Diagnóstico MTC atual", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    TextButton(onClick = onOpenAnamnese) { Text("Revisar →") }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.diagnosis,
                    onValueChange = { onUpdate(null, null, it, null) },
                    label = { Text("Diagnóstico") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            SupremoCard {
                Text("Resumo", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.summary,
                    onValueChange = { onUpdate(it, null, null, null) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.mainComplaint,
                    onValueChange = { onUpdate(null, it, null, null) },
                    label = { Text("Queixa principal") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ── ANAMNESE (Ba Gang / Zang Fu / Língua / Pulso) ──────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnamneseTab(viewModel: SupremoViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SupremoCard {
                SectionHeader(title = "Completude do prontuário")
                Spacer(Modifier.height(10.dp))
                CompletenessBar(progress = state.completeness)
            }
        }

        item {
            SupremoCard {
                SectionHeader(title = "Ba Gang — Oito Princípios", subtitle = "Quatro eixos. Toque de novo para desmarcar.")
                Spacer(Modifier.height(16.dp))
                val bg = state.draft.baGang
                AxisSelector("Yin / Yang", listOf(BaGangPolarity.YIN to "Yin", BaGangPolarity.YANG to "Yang"), bg.polarity, BaGangPolarity.UNSET, onSelect = { viewModel.updateBaGang(bg.copy(polarity = it)) })
                Spacer(Modifier.height(16.dp))
                AxisSelector("Exterior / Interior", listOf(BaGangDepth.EXTERIOR to "Exterior", BaGangDepth.INTERIOR to "Interior"), bg.depth, BaGangDepth.UNSET, onSelect = { viewModel.updateBaGang(bg.copy(depth = it)) })
                Spacer(Modifier.height(16.dp))
                AxisSelector("Frio / Calor", listOf(BaGangTemperature.COLD to "Frio", BaGangTemperature.HEAT to "Calor"), bg.temperature, BaGangTemperature.UNSET, onSelect = { viewModel.updateBaGang(bg.copy(temperature = it)) })
                Spacer(Modifier.height(16.dp))
                AxisSelector("Deficiência / Excesso", listOf(BaGangStrength.DEFICIENCY to "Xu", BaGangStrength.EXCESS to "Shi"), bg.strength, BaGangStrength.UNSET, onSelect = { viewModel.updateBaGang(bg.copy(strength = it)) })
            }
        }

        item {
            SupremoCard {
                SectionHeader(title = "Zang Fu", subtitle = "Órgãos implicados no padrão.")
                Spacer(Modifier.height(14.dp))
                val selected = state.draft.patterns.map { it.organ }.toSet()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Organ.entries.forEach { organ ->
                        SelectableChip(organ.label, organ in selected, { viewModel.togglePattern(ZangFuPattern(organ = organ)) })
                    }
                }
            }
        }

        item {
            SupremoCard {
                SectionHeader(title = "5 Elementos (Wu Xing)", subtitle = "Derivado dos órgãos selecionados em Zang Fu.")
                Spacer(Modifier.height(12.dp))
                val activeElements = state.draft.elements
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Element.entries.forEach { el ->
                        val active = el in activeElements
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(if (active) Primary else MaterialTheme.colorScheme.background)
                                    .border(1.dp, if (active) Primary else MaterialTheme.colorScheme.outline, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(el.label.take(1), color = if (active) Color.White else TextMuted, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(el.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            SupremoCard {
                SectionHeader(title = "Língua")
                Spacer(Modifier.height(14.dp))
                val tongue = state.draft.tongue
                Text("COR DO CORPO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TongueBodyColor.entries.filter { it != TongueBodyColor.UNSET }.forEach { color ->
                        SelectableChip(color.label, tongue.bodyColor == color, {
                            viewModel.updateTongue(tongue.copy(bodyColor = if (tongue.bodyColor == color) TongueBodyColor.UNSET else color))
                        })
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("SABURRA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TongueCoatingColor.entries.filter { it != TongueCoatingColor.UNSET }.forEach { color ->
                        SelectableChip(color.label, tongue.coatingColor == color, {
                            viewModel.updateTongue(tongue.copy(coatingColor = if (tongue.coatingColor == color) TongueCoatingColor.UNSET else color))
                        })
                    }
                }
            }
        }

        item {
            SupremoCard {
                SectionHeader(title = "Pulso", subtitle = "Cun / Guan / Chi, em três profundidades, nos dois punhos.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.draft.pulse.rateBpm?.toString().orEmpty(),
                    onValueChange = { viewModel.updatePulseRate(it.toIntOrNull()) },
                    label = { Text("Frequência (bpm)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Wrist.entries.forEach { wrist ->
            PulsePosition.entries.forEach { position ->
                item {
                    PulseCard(wrist, position, state.draft.pulse.readings) { depth, quality ->
                        val current = state.draft.pulse.at(wrist, position, depth)
                        val qualities = current?.qualities.orEmpty()
                        val next = if (quality in qualities) qualities - quality else qualities + quality
                        viewModel.setPulseReading(PulseReading(wrist, position, depth, next))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PulseCard(wrist: Wrist, position: PulsePosition, readings: List<PulseReading>, onToggle: (PulseDepth, PulseQuality) -> Unit) {
    SupremoCard {
        SectionHeader(title = "${position.label} · punho ${wrist.label.lowercase()}")
        PulseDepth.entries.forEach { depth ->
            Spacer(Modifier.height(14.dp))
            Text(depth.label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            val selected = readings.firstOrNull { it.wrist == wrist && it.position == position && it.depth == depth }?.qualities.orEmpty()
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PulseQuality.entries.forEach { quality ->
                    SelectableChip(quality.label, quality in selected, { onToggle(depth, quality) })
                }
            }
        }
    }
}

// ── PLANO ───────────────────────────────────────────────────────────────

@Composable
private fun PlanoTab(
    state: com.bioacupunt.prontuario.presentation.ProntuarioUiState,
    supremoState: com.bioacupunt.prontuario.presentation.SupremoUiState,
    onUpdate: (String?, String?, String?, String?) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            SupremoCard {
                SectionHeader(
                    title = "Triagem de segurança",
                    subtitle = "Verificação determinística. Executada a cada alteração do prontuário.",
                )
                Spacer(Modifier.height(14.dp))
                ClinicalSafetyPanel(
                    verdict = supremoState.verdict,
                    onOverride = { /* auditado no ViewModel: próxima iteração — ver SupremoViewModel */ },
                )
            }
        }
        item {
            SupremoCard {
                Text("Plano terapêutico", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.treatmentPlan,
                    onValueChange = { onUpdate(null, null, null, it) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    placeholder = { Text("Objetivos, técnicas recomendadas e orientações ao paciente.") },
                )
            }
        }
    }
}

// ── EXAMES ──────────────────────────────────────────────────────────────

@Composable
private fun ExamesTab(vm: ExameViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var addVital by remember { mutableStateOf(false) }
    var addExam by remember { mutableStateOf(false) }
    var addMed by remember { mutableStateOf(false) }
    var addAllergy by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            SupremoCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Sinais vitais", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    TextButton(onClick = { addVital = true }) { Text("+ Adicionar") }
                }
                Spacer(Modifier.height(10.dp))
                if (state.vitals.isEmpty()) {
                    Text("Sem registros.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                } else {
                    state.vitals.forEach { v ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(v.label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(v.value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                            IconButton(onClick = { vm.deleteVital(v.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
        item {
            SupremoCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Exames laboratoriais", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    TextButton(onClick = { addExam = true }) { Text("+ Adicionar") }
                }
                Spacer(Modifier.height(6.dp))
                if (state.exams.isEmpty()) {
                    Text("Sem registros.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                } else {
                    state.exams.forEach { e ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(e.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                Text(e.date, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                            val (bg, fg) = examTagColors(e.resultTag)
                            Box(modifier = Modifier.clip(MaterialTheme.shapes.extraLarge).background(bg).padding(horizontal = 10.dp, vertical = 3.dp)) {
                                Text(e.resultTag.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = fg)
                            }
                            IconButton(onClick = { vm.deleteExam(e.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
        item {
            SupremoCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Medicamentos", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    TextButton(onClick = { addMed = true }) { Text("+ Adicionar") }
                }
                Spacer(Modifier.height(6.dp))
                if (state.medications.isEmpty()) {
                    Text("Sem registros.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                } else {
                    state.medications.forEach { m ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Medication, null, tint = Primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(m.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                if (m.info.isNotBlank()) Text(m.info, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                            IconButton(onClick = { vm.deleteMedication(m.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("ALERGIAS", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.allergies.forEach { a ->
                        Row(
                            modifier = Modifier.clip(MaterialTheme.shapes.extraLarge).background(SemanticError.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp).clickable { vm.deleteAllergy(a.id) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(a.description, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = SemanticError)
                        }
                    }
                    AssistChip(onClick = { addAllergy = true }, label = { Text("+ Alergia") })
                }
            }
        }
    }

    if (addVital) SimpleTwoFieldDialog("Novo sinal vital", "Rótulo (ex: Pressão)", "Valor (ex: 120/80 mmHg)", { addVital = false }) { l, v -> vm.addVital(l, v); addVital = false }
    if (addExam) ExamDialog(onDismiss = { addExam = false }) { name, date, tag -> vm.addExam(name, date, tag); addExam = false }
    if (addMed) SimpleTwoFieldDialog("Novo medicamento", "Nome", "Dosagem / frequência", { addMed = false }) { n, i -> vm.addMedication(n, i); addMed = false }
    if (addAllergy) SimpleOneFieldDialog("Nova alergia", "Descrição", { addAllergy = false }) { d -> vm.addAllergy(d); addAllergy = false }
}

private fun examTagColors(tag: ExamResultTag): Pair<Color, Color> = when (tag) {
    ExamResultTag.NORMAL -> SemanticSuccess.copy(alpha = 0.14f) to SemanticSuccess
    ExamResultTag.ALTERED -> SemanticError.copy(alpha = 0.14f) to SemanticError
    ExamResultTag.PENDING -> SemanticWarning.copy(alpha = 0.14f) to SemanticWarning
}

// ── EVOLUÇÃO ────────────────────────────────────────────────────────────

@Composable
private fun EvolucaoTab(
    entries: List<ProntuarioEntry>,
    onAdd: () -> Unit,
    onEdit: (ProntuarioEntry) -> Unit,
    onDelete: (ProntuarioEntry) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Linha do tempo · Evoluções", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
                TextButton(onClick = onAdd) { Text("+ Nova") }
            }
        }
        if (entries.isEmpty()) {
            item { Text("Sem registros.", color = TextMuted, style = MaterialTheme.typography.bodySmall) }
        } else {
            items(entries, key = { it.id }) { e ->
                SupremoCard {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${e.type.label} · ${e.date.take(10)}", style = MaterialTheme.typography.labelMedium, color = Primary)
                            if (e.doctorName.isNotBlank()) Text("Dr. ${e.doctorName}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { onEdit(e) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) }
                            IconButton(onClick = { onDelete(e) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, null, tint = SemanticError, modifier = Modifier.size(18.dp)) }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(e.body, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SessionFormDialog(entry: ProntuarioEntry?, onDismiss: () -> Unit, onSave: (ProntuarioEntryType, String) -> Unit) {
    var type by remember { mutableStateOf(entry?.type ?: ProntuarioEntryType.EVOLUTION) }
    var body by remember { mutableStateOf(entry?.body ?: "") }
    var doctor by remember { mutableStateOf(entry?.doctorName ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "Nova Sessão" else "Editar Sessão", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProntuarioEntryType.entries.forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                OutlinedTextField(value = doctor, onValueChange = { doctor = it }, label = { Text("Responsável") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Evolução") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
            }
        },
        confirmButton = { TextButton(onClick = { if (body.isNotBlank()) onSave(type, "$doctor\n\n$body") }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ── DOCUMENTOS ──────────────────────────────────────────────────────────

@Composable
private fun DocumentosTab(vm: ExameViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            var name = uri.lastPathSegment ?: "documento"
            var size = 0L
            cursor?.use {
                val nameIdx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIdx = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (it.moveToFirst()) {
                    if (nameIdx >= 0) name = it.getString(nameIdx) ?: name
                    if (sizeIdx >= 0) size = it.getLong(sizeIdx)
                }
            }
            val mimeType = context.contentResolver.getType(uri) ?: ""
            vm.addDocument(name, uri.toString(), mimeType, size)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Documentos", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
                TextButton(onClick = { pickerLauncher.launch(arrayOf("*/*")) }) { Text("+ Adicionar") }
            }
        }
        if (state.documents.isEmpty()) {
            item { Text("Sem documentos anexados.", color = TextMuted, style = MaterialTheme.typography.bodySmall) }
        } else {
            items(state.documents, key = { it.id }) { d ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                        .clickable {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(d.uri)).apply {
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                })
                            }
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Description, null, tint = Primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), maxLines = 1)
                        Text(formatDocMeta(d.mimeType, d.sizeBytes), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    IconButton(onClick = { vm.deleteDocument(d.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

private fun formatDocMeta(mimeType: String, sizeBytes: Long): String {
    val kb = sizeBytes / 1024.0
    val sizeLabel = if (kb >= 1024) "%.1f MB".format(kb / 1024) else "%.0f KB".format(kb)
    return if (mimeType.isNotBlank()) "$mimeType · $sizeLabel" else sizeLabel
}

// ── Small add-item dialogs ────────────────────────────────────────────

@Composable
private fun SimpleOneFieldDialog(title: String, label: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text(label) }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { if (value.isNotBlank()) onSave(value.trim()) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun SimpleTwoFieldDialog(title: String, label1: String, label2: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var v1 by remember { mutableStateOf("") }
    var v2 by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = v1, onValueChange = { v1 = it }, label = { Text(label1) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = v2, onValueChange = { v2 = it }, label = { Text(label2) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { if (v1.isNotBlank()) onSave(v1.trim(), v2.trim()) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ExamDialog(onDismiss: () -> Unit, onSave: (String, String, ExamResultTag) -> Unit) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var tag by remember { mutableStateOf(ExamResultTag.PENDING) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo exame", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome do exame") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Data (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExamResultTag.entries.forEach { t ->
                        FilterChip(selected = tag == t, onClick = { tag = t }, label = { Text(t.label) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim(), date.trim(), tag) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
