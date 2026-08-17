package com.bioacupunt.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import com.bioacupunt.agenda.presentation.AtendimentoUiState
import com.bioacupunt.agenda.presentation.AtendimentoViewModel
import com.bioacupunt.crm.domain.model.CrmPatient
import com.bioacupunt.prontuario.domain.model.*
import com.bioacupunt.pharma.presentation.PrescricaoViewModel
import com.bioacupunt.prontuario.presentation.EvolucaoViewModel
import com.bioacupunt.prontuario.presentation.ExameViewModel
import com.bioacupunt.prontuario.presentation.ProntuarioViewModel
import com.bioacupunt.prontuario.presentation.SupremoViewModel
import com.bioacupunt.ui.components.ClinicalSafetyPanel
import com.bioacupunt.ui.components.PharmaSafetyPanel
import com.bioacupunt.ui.components.SpellCheckedTextField
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
import com.bioacupunt.ui.theme.SemanticWarningBg
import com.bioacupunt.ui.theme.TextMuted
import kotlinx.coroutines.launch

private enum class ProntTab(val label: String) {
    RESUMO("Resumo"), ANAMNESE("Anamnese"), PLANO("Plano"),
    EXAMES("Exames"), PRESCRICAO("Prescrição"), EVOLUCAO("Evolução"), DOCUMENTOS("Documentos"),
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
    onOpenAtendimento: () -> Unit = {},
    onOpenInteligencia: (Long) -> Unit = {},
    vm: ProntuarioViewModel = viewModel(factory = AppContainer.prontuarioViewModelFactory),
    patientId: Long = 0L,
    /**
     * Presente ⇒ este Prontuário está sendo aberto a partir de uma consulta concreta da
     * Agenda ("Atender") — a tela entra em MODO ATENDIMENTO: banner com o tempo de
     * sessão e botão "Finalizar atendimento" (fecha a consulta + grava uma evolução).
     * Ausente ⇒ prontuário aberto pra consulta/edição normal (CRM → paciente), sem
     * nenhum desses dois. Antes disto existir, "Atendimento" era uma TELA separada
     * (`AtendimentoScreen`, removida 2026-08-04) editando o mesmo [MtcAssessment] com
     * uma UI diferente — unificado aqui por pedido explícito da médica.
     */
    appointmentId: Long? = null,
    onFinalizedAtendimento: () -> Unit = {},
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
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                    }
                },
                actions = {
                    if (state.patientId > 0L) {
                        IconButton(onClick = { onOpenInteligencia(state.patientId) }) {
                            Icon(Icons.Default.SmartToy, "Perguntar à IA sobre este caso")
                        }
                    }
                },
            )
        }
    ) { padding ->
        if (showPatientPicker || state.patientId <= 0L) {
            var patientQuery by remember { mutableStateOf("") }
            val filteredPatients = remember(allPatients, patientQuery) {
                if (patientQuery.isBlank()) {
                    allPatients
                } else {
                    val q = patientQuery.trim().lowercase()
                    allPatients.filter { it.name.lowercase().contains(q) || it.phone.contains(q) }
                }
            }
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                Text("Selecione o paciente", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = patientQuery,
                    onValueChange = { patientQuery = it },
                    placeholder = { Text("Buscar por nome ou telefone…") },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (patientQuery.isNotEmpty()) {
                            IconButton(onClick = { patientQuery = "" }) { Icon(Icons.Default.Close, "Limpar busca", modifier = Modifier.size(16.dp)) }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                if (allPatients.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nenhum paciente disponível.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (filteredPatients.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nenhum paciente corresponde à busca.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                        items(filteredPatients, key = { it.id }) { p ->
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
        val prescricaoVm: PrescricaoViewModel = viewModel(
            key = "prescricao-${state.patientId}",
            factory = AppContainer.prescricaoViewModelFactory(state.patientId),
        )
        val evolucaoVm: EvolucaoViewModel = viewModel(
            key = "evolucao-${state.patientId}",
            factory = AppContainer.evolucaoViewModelFactory(state.patientId),
        )
        val supremoState by supremoVm.state.collectAsStateWithLifecycle()
        val exameState by exameVm.state.collectAsStateWithLifecycle()
        val evolucaoState by evolucaoVm.state.collectAsStateWithLifecycle()

        // ── Modo atendimento (appointmentId != null) ──────────────
        val atendVm: AtendimentoViewModel? = appointmentId?.let { apptId ->
            viewModel(key = "atendimento-$apptId", factory = AppContainer.atendimentoViewModelFactory(apptId))
        }
        val atendState: AtendimentoUiState = if (atendVm != null) atendVm.state.collectAsStateWithLifecycle().value else AtendimentoUiState()
        var elapsedSeconds by remember { mutableIntStateOf(0) }
        if (appointmentId != null) {
            LaunchedEffect(appointmentId) {
                while (true) {
                    kotlinx.coroutines.delay(1000)
                    elapsedSeconds++
                }
            }
        }

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
                // BUG CRÍTICO corrigido (2026-07-29, achado em teste real de device):
                // ProntuarioScreen não tinha NENHUM jeito de persistir o rascunho da
                // avaliação (Ba Gang, língua, pulso, motivo, flags) — cada `edit{}` do
                // SupremoViewModel só atualiza `_state.draft` em memória. O único lugar
                // que chamava `supremoVm.save()` era o botão "Rascunho" de
                // AtendimentoScreen (fluxo via Agenda), nunca esta tela — o caminho que
                // a médica realmente usa pra abrir o prontuário de um paciente (CRM →
                // paciente → Prontuário). Resultado: preencher qualquer coisa aqui e
                // sair da tela perdia tudo, em silêncio.
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(if (supremoState.saving) MaterialTheme.colorScheme.outline else SemanticSuccess)
                        .clickable(enabled = !supremoState.saving) { supremoVm.save() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    if (supremoState.saving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text(
                            "Salvar",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Já estamos num atendimento (appointmentId != null): o botão "Atender" some,
                // porque o banner logo abaixo já cumpre esse papel ("Finalizar atendimento").
                if (appointmentId == null) Box(
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

            if (appointmentId != null) {
                AtendimentoBanner(
                    elapsedSeconds = elapsedSeconds,
                    finalizing = atendState.finalizing,
                    onFinalize = {
                        supremoVm.save()
                        val summary = buildString {
                            if (supremoState.draft.chiefComplaint.isNotBlank()) appendLine("Queixa: ${supremoState.draft.chiefComplaint}")
                            if (supremoState.draft.orientations.isNotBlank()) appendLine("Orientações: ${supremoState.draft.orientations}")
                        }.trim()
                        atendVm?.finalize(summary, onFinalizedAtendimento)
                    },
                )
                atendState.error?.let {
                    Text(it, color = SemanticError, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall)
                }
            }

            // Feedback de salvar — sucesso via Toast (some sozinho), erro fica visível
            // até a médica ver (nunca só no Log, mesma regra do resto do app).
            LaunchedEffect(supremoState.savedAt) {
                if (!supremoState.savedAt.isNullOrBlank()) {
                    android.widget.Toast.makeText(context, "Prontuário salvo.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            supremoState.error?.let {
                Text(
                    it,
                    color = SemanticError,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
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

            val onOverride = { reason: String ->
                val userId = runCatching {
                    com.bioacupunt.di.AppContainer.authRepository.getCurrentUser()?.id?.toString()
                        ?: com.bioacupunt.di.AppContainer.securePreferences.pinHash?.take(8) ?: "unknown"
                }.getOrDefault("unknown")
                supremoVm.overrideVeto(reason, userId)
            }
            when (ProntTab.entries[tab]) {
                ProntTab.RESUMO -> ResumoTab(
                    state, supremoState, onUpdate = vm::updateHeader,
                    onChiefComplaintChange = supremoVm::updateChiefComplaint,
                    onAcceptAggravating = supremoVm::acceptAggravatingSuggestion,
                    onAcceptRelieving = supremoVm::acceptRelievingSuggestion,
                    onAcceptReviewOfSystems = supremoVm::acceptReviewOfSystemsSuggestion,
                    onDismissSuggestion = supremoVm::dismissChiefComplaintSuggestion,
                    onOpenAnamnese = { tab = ProntTab.ANAMNESE.ordinal },
                    onOpenEvolucao = { tab = ProntTab.EVOLUCAO.ordinal },
                    onSetRegionEva = supremoVm::setRegionEva,
                    onToggleRelieving = supremoVm::toggleRelieving,
                    onToggleAggravating = supremoVm::toggleAggravating,
                )
                ProntTab.ANAMNESE -> AnamneseTab(supremoVm)
                ProntTab.PLANO -> PlanoTab(
                    supremoState, onOverride = onOverride,
                    onOrientationsChange = supremoVm::updateOrientations,
                    onToggleTechnique = supremoVm::toggleTechnique,
                    onSynthesize = {
                        supremoVm.synthesizeDiagnosis(
                            labSummary = buildLabSummary(exameState),
                            activeMedications = buildActiveMedicationsSummary(exameState),
                            allergySummary = buildAllergySummary(exameState),
                        )
                    },
                    onAcceptTcm = supremoVm::acceptTcmSynthesis,
                    onAcceptBiomedical = supremoVm::acceptBiomedicalSynthesis,
                    onAcceptTherapy = supremoVm::acceptTherapeuticSynthesis,
                    onDismissSynthesis = supremoVm::dismissSynthesis,
                )
                ProntTab.EXAMES -> ExamesTab(exameVm)
                ProntTab.PRESCRICAO -> PrescricaoTab(prescricaoVm)
                ProntTab.EVOLUCAO -> EvolucaoTab(
                    entries = state.entries,
                    history = evolucaoState.history,
                    evaFor = evolucaoVm::evaFor,
                    comparison = evolucaoVm.comparison(),
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

/** Formata sinais vitais + exames para o perfil que a Síntese Diagnóstica IA recebe. */
private fun buildLabSummary(exameState: com.bioacupunt.prontuario.presentation.ExameUiState): String = buildString {
    if (exameState.vitals.isNotEmpty()) {
        appendLine("Sinais vitais:")
        exameState.vitals.forEach { v ->
            val when_ = v.recordedAt.take(10).takeIf { it.isNotBlank() }
            appendLine("- ${v.label}: ${v.value}" + (when_?.let { " ($it)" } ?: ""))
        }
    }
    if (exameState.exams.isNotEmpty()) {
        if (isNotEmpty()) appendLine()
        appendLine("Exames laboratoriais:")
        exameState.exams.forEach { e ->
            val when_ = e.date.take(10).takeIf { it.isNotBlank() }
            appendLine(
                "- ${e.name}: ${e.resultTag.label}" +
                    (e.notes.takeIf { it.isNotBlank() }?.let { " — $it" } ?: "") +
                    (when_?.let { " ($it)" } ?: "")
            )
        }
    }
}.trim()

/** Só medicações ativas — inativas/descontinuadas não pesam na síntese. */
private fun buildActiveMedicationsSummary(exameState: com.bioacupunt.prontuario.presentation.ExameUiState): String =
    exameState.medications.filter { it.active }
        .joinToString("\n") { m -> "- ${m.name}" + (m.info.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: "") }

private fun buildAllergySummary(exameState: com.bioacupunt.prontuario.presentation.ExameUiState): String =
    exameState.allergies.joinToString("\n") { a -> "- ${a.description}" }

private fun initialsOfPatient(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

// ── RESUMO ──────────────────────────────────────────────────────────────

private val relievingOptions = listOf("Repouso", "Calor", "Movimento leve", "Pressão", "Alongamento")
private val aggravatingOptions = listOf("Frio", "Umidade", "Estresse", "Longo período sentado", "Esforço físico", "Noite")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResumoTab(
    state: com.bioacupunt.prontuario.presentation.ProntuarioUiState,
    supremoState: com.bioacupunt.prontuario.presentation.SupremoUiState,
    onUpdate: (String?, String?, String?, String?) -> Unit,
    onChiefComplaintChange: (String) -> Unit,
    onAcceptAggravating: (String) -> Unit,
    onAcceptRelieving: (String) -> Unit,
    onAcceptReviewOfSystems: (String) -> Unit,
    onDismissSuggestion: () -> Unit,
    onOpenAnamnese: () -> Unit,
    onOpenEvolucao: () -> Unit,
    onSetRegionEva: (com.bioacupunt.prontuario.domain.safety.BodyRegion, Int) -> Unit,
    onToggleRelieving: (String) -> Unit,
    onToggleAggravating: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth().clickable(onClick = onOpenEvolucao), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Evolução Clínica", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = Primary))
                Icon(Icons.Default.ChevronRight, null, tint = Primary)
            }
        }
        item {
            // "Motivo da Consulta" consolidado em MtcAssessment.chiefComplaint — o campo
            // que de fato conta pra completude e que a IA extrativa observa. O antigo
            // campo "Queixa principal" (tabela Prontuario/mainComplaint) não é mais
            // oferecido aqui pra não fragmentar em dois lugares — o dado antigo continua
            // no banco, só paramos de escrever nele por esta tela.
            SupremoCard {
                Text("Motivo da Consulta", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(10.dp))
                SpellCheckedTextField(
                    value = supremoState.draft.chiefComplaint,
                    onValueChange = onChiefComplaintChange,
                    placeholder = { Text("Descreva livremente o que a paciente trouxe hoje...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            }
        }
        supremoState.chiefComplaintSuggestion?.let { suggestion ->
            item {
                ChiefComplaintSuggestionCard(
                    suggestion = suggestion,
                    onAcceptAggravating = onAcceptAggravating,
                    onAcceptRelieving = onAcceptRelieving,
                    onAcceptReviewOfSystems = onAcceptReviewOfSystems,
                    onDismiss = onDismissSuggestion,
                )
            }
        }
        item {
            // Vinha só do wizard de Atendimento (que editava o mesmo MtcAssessment por
            // fora do Prontuário) — trazido pra cá na unificação dos dois fluxos numa
            // tela só. `bodyMarks`/EVA já existiam no domínio, só não tinham UI aqui.
            SupremoCard {
                SectionHeader(title = "Mapa corporal", subtitle = "Toque na região · toque de novo para ciclar a intensidade (EVA)")
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.bioacupunt.prontuario.domain.safety.BodyRegion.entries.forEach { region ->
                        val eva = supremoState.draft.bodyMarks.firstOrNull { it.region == region }?.intensity ?: 0
                        val label = if (eva > 0) "${region.label} · $eva" else region.label
                        SelectableChip(label, eva > 0, {
                            val next = when (eva) { 0 -> 3; 3 -> 6; 6 -> 9; else -> 0 }
                            onSetRegionEva(region, next)
                        })
                    }
                }
            }
        }
        item {
            SupremoCard {
                Text("O QUE MELHORA", style = MaterialTheme.typography.labelMedium, color = Primary)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    relievingOptions.forEach { f -> SelectableChip(f, f in supremoState.draft.relievingFactors, { onToggleRelieving(f) }) }
                }
                Spacer(Modifier.height(14.dp))
                Text("O QUE PIORA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    aggravatingOptions.forEach { f -> SelectableChip(f, f in supremoState.draft.aggravatingFactors, { onToggleAggravating(f) }) }
                }
            }
        }
        item {
            SupremoCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Diagnóstico MTC atual", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    TextButton(onClick = onOpenAnamnese) { Text("Revisar →") }
                }
                Spacer(Modifier.height(10.dp))
                SpellCheckedTextField(
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
                SpellCheckedTextField(
                    value = state.summary,
                    onValueChange = { onUpdate(it, null, null, null) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            }
        }
    }
}

/**
 * "Sugestão da IA — revise antes de usar." Cada chip é a mesma escrita que um toque
 * manual faria (`toggleAggravating`/`toggleRelieving`/`toggleReviewOfSystems` via
 * SupremoViewModel) — aceitar não é um caminho de gravação paralelo, só um atalho
 * pra não digitar de novo o que ela já escreveu no motivo da consulta.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChiefComplaintSuggestionCard(
    suggestion: com.bioacupunt.prontuario.domain.usecase.ChiefComplaintExtraction,
    onAcceptAggravating: (String) -> Unit,
    onAcceptRelieving: (String) -> Unit,
    onAcceptReviewOfSystems: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    SupremoCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Sugestão da IA", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = Primary))
                Text(
                    "Extraído do que você já escreveu acima — revise antes de usar.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Dispensar", tint = TextMuted) }
        }
        Spacer(Modifier.height(10.dp))
        if (suggestion.aggravating.isNotEmpty()) {
            Text("PIORA COM", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestion.aggravating.forEach { item -> SelectableChip(item, false, { onAcceptAggravating(item) }) }
            }
            Spacer(Modifier.height(10.dp))
        }
        if (suggestion.relieving.isNotEmpty()) {
            Text("MELHORA COM", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestion.relieving.forEach { item -> SelectableChip(item, false, { onAcceptRelieving(item) }) }
            }
            Spacer(Modifier.height(10.dp))
        }
        if (suggestion.reviewOfSystemsHits.isNotEmpty()) {
            Text("OUTROS SINTOMAS CITADOS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestion.reviewOfSystemsHits.forEach { item -> SelectableChip(item, false, { onAcceptReviewOfSystems(item) }) }
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
            // R1: o motor de segurança (ClinicalSafetyEngine) só enxerga o que estiver
            // aqui — `MtcAssessment.flags`. Sem esta seção não existe NENHUMA forma de
            // marcar gestação, marca-passo, anticoagulante etc. em lugar algum do app;
            // `SupremoViewModel.toggleFlag` ficava sem chamador, `flags` era sempre
            // vazio, e a triagem sempre devolvia "Sem contraindicações detectadas" —
            // não porque a paciente não tinha risco, mas porque nada jamais era
            // registrado. Isso não estava errado, estava faltando.
            SupremoCard {
                SectionHeader(
                    title = "Fatores de risco / contraindicações",
                    subtitle = "Alimenta a triagem de segurança determinística (aba Plano). Toque para marcar.",
                )
                Spacer(Modifier.height(14.dp))
                val flags = state.draft.flags
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClinicalFlag.entries.forEach { flag ->
                        SelectableChip(flag.label, flag in flags, { viewModel.toggleFlag(flag) })
                    }
                }
                // Semanas de gestação: alimenta a regra de trimestre do motor de
                // segurança (eletroacupuntura no 1º trimestre). Antes a regra existia
                // e era testada, mas nenhuma tela chamava updateGestationalWeeks —
                // o alerta era logicamente inalcançável. Visível quando a flag
                // Gestação está marcada.
                if (ClinicalFlag.PREGNANCY in flags) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Semanas de gestação (trimestre da gestante)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.draft.gestationalWeeks?.toString().orEmpty(),
                        onValueChange = { viewModel.updateGestationalWeeks(it.toIntOrNull()) },
                        label = { Text("Semanas") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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
                val selectedPatterns = state.draft.patterns
                val selected = selectedPatterns.map { it.organ }.toSet()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Organ.entries.forEach { organ ->
                        SelectableChip(organ.label, organ in selected, { viewModel.togglePattern(ZangFuPattern(organ = organ)) })
                    }
                }
                if (selectedPatterns.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    selectedPatterns.forEach { pattern ->
                        SpellCheckedTextField(
                            value = pattern.notes,
                            onValueChange = { viewModel.updatePatternNotes(pattern.organ, it) },
                            label = { Text("Notas — ${pattern.organ.label}") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        )
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
                Spacer(Modifier.height(14.dp))
                SpellCheckedTextField(
                    value = tongue.notes,
                    onValueChange = { viewModel.updateTongueNotes(it) },
                    label = { Text("Notas (forma, espessura, umidade, o que não coube nos chips)") },
                    modifier = Modifier.fillMaxWidth(),
                )
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
                Spacer(Modifier.height(10.dp))
                SpellCheckedTextField(
                    value = state.draft.pulse.notes,
                    onValueChange = { viewModel.updatePulseNotes(it) },
                    label = { Text("Notas gerais do pulso") },
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

/**
 * Colapsado por padrão — expandir revela o `FlowRow` de 28 [PulseQuality] por
 * profundidade. Antes, os 6 cartões (2 punhos × 3 posições) ficavam todos sempre
 * abertos ao mesmo tempo — até 504 alvos de toque simultâneos, o maior excesso de
 * checkbox do app. O dado por trás não muda, só a exposição visual: "não
 * registrado" continua explícito no resumo do cartão fechado (silêncio nunca é
 * ambíguo), e abrir/fechar não apaga nenhuma seleção já feita.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PulseCard(wrist: Wrist, position: PulsePosition, readings: List<PulseReading>, onToggle: (PulseDepth, PulseQuality) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val allSelected = PulseDepth.entries.flatMap { depth ->
        readings.firstOrNull { it.wrist == wrist && it.position == position && it.depth == depth }?.qualities.orEmpty()
    }
    SupremoCard {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${position.label} · punho ${wrist.label.lowercase()}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (allSelected.isEmpty()) "Não registrado" else allSelected.joinToString { it.label },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (allSelected.isEmpty()) TextMuted else Primary,
                )
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Recolher" else "Expandir",
                tint = TextMuted,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
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
    }
}

// ── PLANO ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanoTab(
    supremoState: com.bioacupunt.prontuario.presentation.SupremoUiState,
    onOverride: ((String) -> Unit)? = null,
    onOrientationsChange: (String) -> Unit = {},
    onToggleTechnique: (com.bioacupunt.prontuario.domain.safety.Technique) -> Unit = {},
    onSynthesize: () -> Unit = {},
    onAcceptTcm: () -> Unit = {},
    onAcceptBiomedical: () -> Unit = {},
    onAcceptTherapy: () -> Unit = {},
    onDismissSynthesis: () -> Unit = {},
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
                    onOverride = onOverride,
                )
            }
        }
        item {
            // Vinha só do wizard de Atendimento — trazido pra cá na unificação. Não é só
            // cosmético: é a técnica marcada aqui que o motor de segurança usa pra decidir
            // regras específicas de técnica (ex.: eletroacupuntura × marca-passo). Sem
            // este card em algum lugar do Prontuário, quem só usasse esta tela nunca
            // conseguiria marcar a técnica, e essas regras nunca disparariam de verdade.
            val securePrefs = com.bioacupunt.di.AppContainer.securePreferences
            val enabledNames = remember {
                securePrefs.enabledTechniquesCsv.split(",").filter { it.isNotBlank() }.toSet()
                    .ifEmpty { com.bioacupunt.prontuario.domain.safety.Technique.entries.map { it.name }.toSet() }
            }
            SupremoCard {
                Text("Técnicas selecionadas", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.bioacupunt.prontuario.domain.safety.Technique.entries.filter { it.name in enabledNames }.forEach { t ->
                        SelectableChip(t.label, t in supremoState.proposal.techniques, { onToggleTechnique(t) })
                    }
                }
            }
        }
        // ── SÍNTESE DIAGNÓSTICA IA ────────────────────────────────
        // Fica na aba Plano (não Resumo): é aqui que a médica decide o que vira
        // conduta. Vem depois da triagem de segurança, nunca antes — um plano
        // sugerido por IA passa pelo mesmo veto que qualquer outro.
        if (supremoState.clinicalSynthesis != null || supremoState.synthesizing || supremoState.synthesisError != null) {
            item {
                DiagnosticSynthesisResultCard(
                    synthesis = supremoState.clinicalSynthesis,
                    synthesizing = supremoState.synthesizing,
                    synthesisError = supremoState.synthesisError,
                    onAcceptTcm = onAcceptTcm,
                    onAcceptBiomedical = onAcceptBiomedical,
                    onAcceptTherapy = onAcceptTherapy,
                    onDismiss = onDismissSynthesis,
                )
            }
        } else {
            item {
                DiagnosticSynthesisTriggerCard(
                    completeness = supremoState.completeness,
                    onSynthesize = onSynthesize,
                )
            }
        }
        item {
            SupremoCard {
                Text("Plano terapêutico", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(10.dp))
                SpellCheckedTextField(
                    value = supremoState.draft.orientations,
                    onValueChange = onOrientationsChange,
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
    val context = LocalContext.current
    var addVital by remember { mutableStateOf(false) }
    var addExam by remember { mutableStateOf(false) }
    var addMed by remember { mutableStateOf(false) }
    var addAllergy by remember { mutableStateOf(false) }
    var examFilter by remember { mutableStateOf<ExamResultTag?>(null) }
    val filteredExams = remember(state.exams, examFilter) {
        state.exams.filter { examFilter == null || it.resultTag == examFilter }
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
                if (state.exams.size > 1) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = examFilter == null, onClick = { examFilter = null }, label = { Text("Todos") })
                        ExamResultTag.entries.forEach { t ->
                            FilterChip(selected = examFilter == t, onClick = { examFilter = if (examFilter == t) null else t }, label = { Text(t.label) })
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (state.exams.isEmpty()) {
                    Text("Sem registros.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                } else if (filteredExams.isEmpty()) {
                    Text("Nenhum exame corresponde ao filtro.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                } else {
                    filteredExams.forEach { e ->
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

// ── PRESCRIÇÃO (Smart Prescription) ──────────────────────────────────────

/**
 * Busca no catálogo Farmacologia, roda o [com.bioacupunt.pharma.domain.safety.PharmaSafetyEngine]
 * contra o perfil de risco desta paciente e só então libera salvar — mesmo contrato de
 * "veto alto e não dispensável" do resto do prontuário. Confirmar com FORBIDDEN pendente
 * é ignorado em silêncio pela ViewModel; só o override com justificativa grava.
 */
@Composable
private fun PrescricaoTab(vm: PrescricaoViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.error, state.savedMessage) {
        val msg = state.error ?: state.savedMessage
        if (!msg.isNullOrBlank()) {
            if (context is android.app.Activity) {
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
            vm.clearMessages()
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val selected = state.selected
        if (selected == null) {
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = vm::onQueryChanged,
                    label = { Text("Buscar medicamento pra prescrever") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            val selectedClasse = state.selectedClasse
            when {
                state.query.isNotBlank() -> items(state.results, key = { it.id }) { med ->
                    SupremoCard {
                        Column(Modifier.fillMaxWidth().clickable { vm.selectMedicamento(med) }) {
                            Text(med.nomeComercial, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                "${med.principiosAtivos.joinToString()} · ${med.classeTerapeutica}",
                                style = MaterialTheme.typography.bodySmall, color = TextMuted,
                            )
                        }
                    }
                }
                selectedClasse != null -> medicamentoClassResultItems(
                    classe = selectedClasse,
                    items = state.classResults,
                    onBack = vm::clearClasseSelection,
                    onSelect = vm::selectMedicamento,
                )
                else -> {
                    if (state.loadingClasses) item { CircularProgressIndicator(modifier = Modifier.padding(16.dp)) }
                    classeTerapeuticaGridItems(classes = state.classes, onSelect = vm::selectClasse)
                }
            }
        } else {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(selected.nomeComercial, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(selected.principiosAtivos.joinToString(), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    TextButton(onClick = vm::clearSelection) { Text("Trocar") }
                }
            }
            item {
                if (state.checkingVerdict) {
                    CircularProgressIndicator(modifier = Modifier.padding(12.dp))
                } else {
                    state.verdict?.let { verdict ->
                        PharmaSafetyPanel(verdict = verdict, onOverride = vm::overridePrescricaoVeto)
                    }
                }
            }
            item {
                SupremoCard {
                    Text("DADOS DA PRESCRIÇÃO", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(state.dose, vm::onDoseChanged, label = { Text("Dose") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(state.frequencia, vm::onFrequenciaChanged, label = { Text("Frequência") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(state.duracao, vm::onDuracaoChanged, label = { Text("Duração") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(state.via, vm::onViaChanged, label = { Text("Via de administração") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    SpellCheckedTextField(state.observacoes, vm::onObservacoesChanged, label = { Text("Observações") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    val blocked = state.verdict?.isBlocked == true
                    Button(
                        onClick = vm::confirmPrescricao,
                        enabled = !state.saving && !state.checkingVerdict && !blocked,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (blocked) "Bloqueado — justifique acima pra prosseguir" else "Confirmar prescrição") }
                }
            }
        }

        item {
            Text("MEDICAÇÕES ATIVAS", style = MaterialTheme.typography.labelMedium, color = TextMuted)
        }
        if (state.activePrescricoes.isEmpty()) {
            item { Text("Nenhuma prescrição ativa registrada por aqui.", style = MaterialTheme.typography.bodySmall, color = TextMuted) }
        } else {
            items(state.activePrescricoes, key = { it.id }) { p ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            p.medicamentoNomeLivre.ifBlank { p.medicamentoId ?: "Medicamento" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        )
                        Text("${p.dose} · ${p.frequencia}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        if (p.overrideReason.isNotBlank()) {
                            Text("Override: ${p.overrideReason}", style = MaterialTheme.typography.labelSmall, color = SemanticWarning)
                        }
                    }
                    IconButton(onClick = { vm.deactivate(p.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ── EVOLUÇÃO ────────────────────────────────────────────────────────────

@Composable
private fun EvolucaoTab(
    entries: List<ProntuarioEntry>,
    history: List<MtcAssessment>,
    evaFor: (MtcAssessment) -> Int?,
    comparison: com.bioacupunt.prontuario.presentation.EvolucaoComparison,
    onAdd: () -> Unit,
    onEdit: (ProntuarioEntry) -> Unit,
    onDelete: (ProntuarioEntry) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<ProntuarioEntryType?>(null) }
    val filteredEntries = remember(entries, query, typeFilter) {
        entries
            .filter { typeFilter == null || it.type == typeFilter }
            .filter { query.isBlank() || it.body.contains(query, ignoreCase = true) || it.doctorName.contains(query, ignoreCase = true) }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Gráfico de EVA + comparação automática primeira-vs-última sessão — antes só
        // existiam numa tela separada (EvolucaoScreen, removida 2026-08-04); trazidos
        // pra dentro desta aba pra não ter dois lugares diferentes mostrando o mesmo
        // histórico de jeitos diferentes.
        item { EvaTrendCard(history, evaFor = evaFor) }
        if (comparison.hasData) {
            item { ComparisonBanner(comparison) }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Linha do tempo · Evoluções", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
                TextButton(onClick = onAdd) { Text("+ Nova") }
            }
        }
        if (entries.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar no texto das evoluções…") },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(selected = typeFilter == null, onClick = { typeFilter = null }, label = { Text("Todas") })
                    ProntuarioEntryType.entries.forEach { t ->
                        FilterChip(selected = typeFilter == t, onClick = { typeFilter = if (typeFilter == t) null else t }, label = { Text(t.label) })
                    }
                }
            }
        }
        if (entries.isEmpty()) {
            item { Text("Sem registros.", color = TextMuted, style = MaterialTheme.typography.bodySmall) }
        } else if (filteredEntries.isEmpty()) {
            item { Text("Nenhuma evolução corresponde ao filtro.", color = TextMuted, style = MaterialTheme.typography.bodySmall) }
        } else {
            items(filteredEntries, key = { it.id }) { e ->
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

/** Portado de EvolucaoScreen.kt (removida) — mesmo cálculo, mesmo visual. */
@Composable
private fun EvaTrendCard(history: List<MtcAssessment>, evaFor: (MtcAssessment) -> Int?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
            .padding(18.dp),
    ) {
        Text("Gráfico de tendência · EVA", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(4.dp))
        val points = history.takeLast(8).mapNotNull { a -> evaFor(a)?.let { a to it } }
        if (points.isEmpty()) {
            Text("Sem sessões com dor registrada ainda.", style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.padding(top = 8.dp))
            return@Column
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(90.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            points.forEach { (assessment, eva) ->
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height((eva.coerceIn(0, 10) * 6 + 6).dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Accent, Primary))),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("$eva", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    Text(assessment.date.take(5), style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
                }
            }
        }
    }
}

/** Portado de EvolucaoScreen.kt (removida) — mesmo cálculo, mesmo visual. */
@Composable
private fun ComparisonBanner(comparison: com.bioacupunt.prontuario.presentation.EvolucaoComparison) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Primary, modifier = Modifier.size(20.dp))
        Column {
            Text("Comparação automática", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Primary)
            Spacer(Modifier.height(2.dp))
            val parts = buildList {
                if (comparison.tongueFrom != null && comparison.tongueTo != null) {
                    add("língua passou de ${comparison.tongueFrom.label.lowercase()} → ${comparison.tongueTo.label.lowercase()}")
                }
                if (comparison.evaFrom != null && comparison.evaTo != null) {
                    add("EVA de ${comparison.evaFrom} para ${comparison.evaTo}")
                }
            }
            Text(
                parts.joinToString("; ").ifBlank { "Ainda sem dados suficientes para comparar." }
                    .replaceFirstChar { it.uppercase() } + ".",
                style = MaterialTheme.typography.bodySmall,
            )
            if (comparison.evaFrom != null && comparison.evaTo != null) {
                Text(
                    if (comparison.evaImproved) "Melhora consistente." else if (comparison.evaTo!! > comparison.evaFrom!!) "Piora — revisar plano." else "Estável.",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (comparison.evaImproved) SemanticSuccess else if (comparison.evaTo!! > comparison.evaFrom!!) SemanticError else TextMuted,
                )
            }
        }
    }
}

/** Banner de sessão ativa — substitui o timer/"Finalizar atendimento" que antes só
 * existiam no wizard `AtendimentoScreen` (removido). */
@Composable
private fun AtendimentoBanner(elapsedSeconds: Int, finalizing: Boolean, onFinalize: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(SemanticWarningBg)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Timer, null, tint = SemanticWarning, modifier = Modifier.size(16.dp))
                Text(
                    "Atendimento em andamento · ${formatElapsed(elapsedSeconds)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = SemanticWarning,
                )
            }
            Button(onClick = onFinalize, enabled = !finalizing, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                if (finalizing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Finalizar atendimento")
                }
            }
        }
    }
}

private fun formatElapsed(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
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
                SpellCheckedTextField(value = body, onValueChange = { body = it }, label = { Text("Evolução") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
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

    LaunchedEffect(state.error) {
        val msg = state.error
        if (!msg.isNullOrBlank()) {
            if (context is android.app.Activity) {
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
            vm.clearError()
        }
    }

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

    var query by remember { mutableStateOf("") }
    val filteredDocuments = remember(state.documents, query) {
        if (query.isBlank()) state.documents else state.documents.filter { it.name.contains(query, ignoreCase = true) }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Documentos", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
                TextButton(onClick = { pickerLauncher.launch(arrayOf("*/*")) }) { Text("+ Adicionar") }
            }
        }
        if (state.documents.size > 1) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar por nome…") },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (state.documents.isEmpty()) {
            item { Text("Sem documentos anexados.", color = TextMuted, style = MaterialTheme.typography.bodySmall) }
        } else if (filteredDocuments.isEmpty()) {
            item { Text("Nenhum documento corresponde à busca.", color = TextMuted, style = MaterialTheme.typography.bodySmall) }
        } else {
            items(filteredDocuments, key = { it.id }) { d ->
                SupremoCard(
                    modifier = Modifier.clickable {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(d.uri)).apply {
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            })
                        }
                    },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
}

private fun formatDocMeta(mimeType: String, sizeBytes: Long): String {
    val kb = sizeBytes / 1024.0
    val sizeLabel = if (kb >= 1024) "%.1f MB".format(kb / 1024) else "%.0f KB".format(kb)
    return if (mimeType.isNotBlank()) "$mimeType · $sizeLabel" else sizeLabel
}

// ── SÍNTESE DIAGNÓSTICA IA ────────────────────────────────────────────
//
// Card de sugestão diagnóstica gerada pela IA a partir de TODOS os dados do
// prontuário. A médica revisa cada componente (MTC, biomédico, plano) e decide
// se aceita, edita ou descarta. NUNCA salva automaticamente.
//
// Dividido em dois composables: [DiagnosticSynthesisTriggerCard] (só o botão de
// gerar, quando não há resultado) e [DiagnosticSynthesisResultCard] (resultado
// completo, loading ou erro). Assim o LazyColumn não renderiza o card enorme
// enquanto não há síntese.

/** Botão para gerar a síntese — renderizado quando não há resultado pendente. */
@Composable
private fun DiagnosticSynthesisTriggerCard(
    completeness: Float,
    onSynthesize: () -> Unit,
) {
    SupremoCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("🤖 Síntese Diagnóstica IA", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = Primary))
                Text(
                    if (completeness < 0.5f) "Preencha mais dados do prontuário para uma análise mais precisa"
                    else "Analisa todo o prontuário e sugere diagnóstico MTC + CID + plano terapêutico",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onSynthesize,
            modifier = Modifier.fillMaxWidth(),
            enabled = completeness > 0f,
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Icon(Icons.Default.Star, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Gerar Síntese Diagnóstica IA")
        }
        if (completeness < 0.5f) {
            Spacer(Modifier.height(4.dp))
            Text("Complete ao menos o Motivo da Consulta, Ba Gang, Zang Fu, Língua e Pulso para uma análise mais precisa.",
                style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

/** Resultado da síntese — renderizado quando há síntese, loading ou erro. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiagnosticSynthesisResultCard(
    synthesis: ClinicalSynthesis?,
    synthesizing: Boolean,
    synthesisError: String?,
    onAcceptTcm: () -> Unit,
    onAcceptBiomedical: () -> Unit,
    onAcceptTherapy: () -> Unit,
    onDismiss: () -> Unit,
) {
    SupremoCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("🤖 Síntese Diagnóstica IA", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = Primary))
                Text(
                    when {
                        synthesis != null -> "Sugestão gerada — revise cada componente antes de aceitar"
                        synthesizing -> "Analisando prontuário e buscando evidências..."
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        // ── Loading state ──────────────────────────────
        if (synthesizing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text("Coletando dados clínicos, buscando na biblioteca e sintetizando...",
                style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }

        // ── Error state ────────────────────────────────
        synthesisError?.let { error ->
            Text(error, style = MaterialTheme.typography.bodySmall, color = SemanticError)
        }

        // ── Synthesis result ───────────────────────────
        if (synthesis != null && !synthesizing) {
            val confidenceColor = when (synthesis.overallConfidence) {
                ConfidenceLevel.HIGH -> SemanticSuccess
                ConfidenceLevel.MODERATE -> SemanticWarning
                ConfidenceLevel.LOW -> SemanticError
                ConfidenceLevel.INSUFFICIENT_EVIDENCE -> TextMuted
            }
            Row(
                modifier = Modifier.clip(MaterialTheme.shapes.extraLarge)
                    .background(confidenceColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text("Confiança: ${synthesis.overallConfidence.label}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = confidenceColor)
            }
            Spacer(Modifier.height(12.dp))

            // TCM Diagnosis
            synthesis.tcmDiagnosis?.let { tcm ->
                Text("DIAGNÓSTICO MTC", style = MaterialTheme.typography.labelMedium, color = Primary)
                Spacer(Modifier.height(6.dp))
                Text(tcm.patternName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                if (tcm.organInvolvement.isNotEmpty()) {
                    Text("Órgãos: ${tcm.organInvolvement.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                if (tcm.baGangClassification.isNotBlank()) {
                    Text("Ba Gang: ${tcm.baGangClassification}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                if (tcm.explanation.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    MarkdownText(tcm.explanation, bodyStyle = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = onAcceptTcm, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Aceitar diagnóstico MTC")
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
            }

            // Biomedical Diagnosis
            synthesis.biomedicalDiagnosis?.let { bio ->
                Text("DIAGNÓSTICO BIOMÉDICO", style = MaterialTheme.typography.labelMedium, color = Primary)
                Spacer(Modifier.height(6.dp))
                Text(bio.diagnosis, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                if (bio.cidCode.isNotBlank()) Text("CID-10: ${bio.cidCode}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                if (bio.cid11Code.isNotBlank()) Text("CID-11: ${bio.cid11Code}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                if (bio.explanation.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    MarkdownText(bio.explanation, bodyStyle = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = onAcceptBiomedical, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Aceitar diagnóstico biomédico")
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
            }

            // Differential Diagnoses
            if (synthesis.differentialDiagnoses.isNotEmpty()) {
                Text("DIAGNÓSTICOS DIFERENCIAIS", style = MaterialTheme.typography.labelMedium, color = Primary)
                Spacer(Modifier.height(6.dp))
                synthesis.differentialDiagnoses.sortedBy { it.priority }.forEach { dd ->
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                        Text("${dd.priority}. ", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        Column {
                            Text(dd.description, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                            if (dd.rationale.isNotBlank()) {
                                Text(dd.rationale, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
            }

            // Therapeutic Plan
            synthesis.therapeuticSuggestion?.let { therapy ->
                Text("PLANO TERAPÊUTICO SUGERIDO", style = MaterialTheme.typography.labelMedium, color = Primary)
                Spacer(Modifier.height(6.dp))
                if (therapy.objectives.isNotBlank()) {
                    MarkdownText("**Objetivos:** ${therapy.objectives}", bodyStyle = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                }
                if (therapy.recommendedTechniques.isNotEmpty()) {
                    Text("Técnicas: ${therapy.recommendedTechniques.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                if (therapy.acupuncturePoints.isNotEmpty()) {
                    Text("Pontos: ${therapy.acupuncturePoints.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                if (therapy.pointCombinations.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    therapy.pointCombinations.forEach { combo ->
                        Text("• $combo", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (therapy.cautionAndContraindications.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    therapy.cautionAndContraindications.forEach { caution ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = SemanticWarning, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(caution, style = MaterialTheme.typography.labelSmall, color = SemanticWarning)
                        }
                    }
                }
                therapy.sessionCount?.let {
                    Text("Sessões sugeridas: $it", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                if (therapy.frequency.isNotBlank()) {
                    Text("Frequência: ${therapy.frequency}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = onAcceptTherapy, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Aceitar plano terapêutico")
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
            }

            // Evidence Sources
            if (synthesis.evidenceSources.isNotEmpty()) {
                Text("FONTES UTILIZADAS", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Spacer(Modifier.height(6.dp))
                synthesis.evidenceSources.forEach { source ->
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(
                            when (source.type) {
                                com.bioacupunt.prontuario.domain.model.SourceType.LIBRARY -> Icons.Default.Description
                                com.bioacupunt.prontuario.domain.model.SourceType.WEB -> Icons.Default.Search
                                com.bioacupunt.prontuario.domain.model.SourceType.CLINICAL_DATA -> Icons.Default.Medication
                            },
                            null,
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("${source.type.label}: ${source.title}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Dismiss all
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Descartar")
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("Sugestão da IA — revise e valide antes de utilizar clinicamente.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted)
        }
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamDialog(onDismiss: () -> Unit, onSave: (String, String, ExamResultTag) -> Unit) {
    var name by remember { mutableStateOf("") }
    // Antes: OutlinedTextField livre com placeholder "AAAA-MM-DD" — sem DatePicker,
    // aberto a formato inconsistente (e sem base confiável pra um futuro filtro por
    // data). DatePicker real garante o formato ISO sempre, sem a médica precisar digitar.
    var date by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var tag by remember { mutableStateOf(ExamResultTag.PENDING) }
    var showDatePicker by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo exame", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome do exame") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Data") },
                    trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.CalendarMonth, "Escolher data") } },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    singleLine = true,
                )
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

    if (showDatePicker) {
        val initialMillis = runCatching {
            java.time.LocalDate.parse(date).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        date = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
