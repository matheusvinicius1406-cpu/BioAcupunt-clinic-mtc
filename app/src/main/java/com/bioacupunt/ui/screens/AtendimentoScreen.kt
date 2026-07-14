package com.bioacupunt.ui.screens

import android.net.Uri
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
import com.bioacupunt.agenda.presentation.AtendimentoViewModel
import com.bioacupunt.di.AppContainer
import com.bioacupunt.prontuario.domain.model.Element
import com.bioacupunt.prontuario.domain.model.Organ
import com.bioacupunt.prontuario.domain.model.TongueBodyColor
import com.bioacupunt.prontuario.domain.model.TongueCoatingColor
import com.bioacupunt.prontuario.domain.model.TongueCoatingThickness
import com.bioacupunt.prontuario.domain.model.TongueMoisture
import com.bioacupunt.prontuario.domain.model.PulsePosition
import com.bioacupunt.prontuario.domain.model.Wrist
import com.bioacupunt.prontuario.domain.safety.BodyRegion
import com.bioacupunt.prontuario.domain.safety.Technique
import com.bioacupunt.prontuario.presentation.SupremoViewModel
import com.bioacupunt.ui.components.ClinicalSafetyPanel
import com.bioacupunt.ui.design.SectionHeader
import com.bioacupunt.ui.design.SelectableChip
import com.bioacupunt.ui.design.SupremoCard
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.SemanticWarning
import com.bioacupunt.ui.theme.SemanticWarningBg
import com.bioacupunt.ui.theme.TextMuted

private enum class AtStep(val label: String) {
    QUEIXA("Queixa"), INTERROGATORIO("Interrogatório"), ZANGFU("Zang Fu"), LINGUA_PULSO("Língua/Pulso"), PLANO("Plano"),
}

private val relievingOptions = listOf("Repouso", "Calor", "Movimento leve", "Pressão", "Alongamento")
private val aggravatingOptions = listOf("Frio", "Umidade", "Estresse", "Longo período sentado", "Esforço físico", "Noite")

private val interrogationCategories = listOf(
    "Termorregulação" to listOf("Frio nas extremidades", "Calor nas palmas", "Sudorese noturna", "Calafrios"),
    "Digestão" to listOf("Distensão pós-prandial", "Refluxo", "Fezes moles", "Constipação", "Apetite reduzido"),
    "Energia" to listOf("Fadiga matinal", "Fadiga pós-prandial", "Falta de ar ao esforço"),
    "Sono" to listOf("Dificuldade em pegar no sono", "Despertar noturno", "Sono não reparador"),
    "Emocional" to listOf("Irritabilidade", "Ansiedade", "Tristeza"),
)

/**
 * ATENDIMENTO — sessão guiada em 5 passos sobre o MESMO [MtcAssessment] do
 * Prontuário Supremo (via [SupremoViewModel]). Não é uma segunda ficha: é a mesma
 * ficha, em fluxo de atendimento em vez de abas.
 *
 * Sem card de diagnóstico por IA (mesma decisão do Prontuário — R2/R4: sem
 * fabricar % de confiança). O que aparece aqui é só dado real: o veredito do
 * [com.bioacupunt.prontuario.domain.safety.ClinicalSafetyEngine], que roda a cada
 * edição.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtendimentoScreen(
    appointmentId: Long,
    onBack: () -> Unit = {},
    onFinalized: () -> Unit = {},
) {
    val atendVm: AtendimentoViewModel = viewModel(
        key = "atendimento-$appointmentId",
        factory = AppContainer.atendimentoViewModelFactory(appointmentId),
    )
    val atendState by atendVm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Atendimento") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") } },
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                atendState.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                atendState.appointment == null -> Text(
                    atendState.error ?: "Consulta não encontrada.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = MaterialTheme.colorScheme.error,
                )
                else -> AtendimentoWizard(
                    appointment = atendState.appointment!!,
                    finalizing = atendState.finalizing,
                    onFinalize = { summary -> atendVm.finalize(summary, onFinalized) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AtendimentoWizard(
    appointment: com.bioacupunt.agenda.domain.model.Appointment,
    finalizing: Boolean,
    onFinalize: (String) -> Unit,
) {
    val supremoVm: SupremoViewModel = viewModel(
        key = "supremo-${appointment.patientId}",
        factory = AppContainer.supremoViewModelFactory(appointment.patientId),
    )
    val state by supremoVm.state.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableIntStateOf(0) }

    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            elapsedSeconds++
        }
    }
    val timerLabel = if (Technique.NEEDLING in state.proposal.techniques) "Agulhas" else "Sessão"

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Atendimento", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold))
                Box(
                    modifier = Modifier.clip(MaterialTheme.shapes.extraLarge).background(SemanticWarningBg).padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Timer, null, tint = SemanticWarning, modifier = Modifier.size(15.dp))
                        Text("$timerLabel ${formatElapsed(elapsedSeconds)}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = SemanticWarning)
                    }
                }
            }
            Text(
                "${appointment.patientName} · Sessão ${appointment.sessionNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AtStep.entries.forEachIndexed { i, s ->
                    val selected = step == i
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(if (selected) Primary else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (selected) Primary else MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraLarge)
                            .clickable { step = i }
                            .padding(vertical = 7.dp, horizontal = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(16.dp).clip(CircleShape).background(if (selected) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = if (selected) Color.White else TextMuted)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        when (AtStep.entries[step]) {
            AtStep.QUEIXA -> queixaStep(state, supremoVm)
            AtStep.INTERROGATORIO -> interrogatorioStep(state, supremoVm)
            AtStep.ZANGFU -> zangFuStep(state, supremoVm)
            AtStep.LINGUA_PULSO -> linguaPulsoStep(state, supremoVm)
            AtStep.PLANO -> planoStep(state, supremoVm, finalizing, onFinalize)
        }

        item {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { if (step > 0) step-- }, modifier = Modifier.weight(1f), enabled = step > 0) { Text("Anterior") }
                OutlinedButton(onClick = { supremoVm.save() }) { Text("Rascunho") }
                Button(onClick = { if (step < AtStep.entries.lastIndex) step++ }, modifier = Modifier.weight(1f), enabled = step < AtStep.entries.lastIndex, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("Próximo") }
            }
        }
    }
}

private fun formatElapsed(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}

private fun androidx.compose.foundation.lazy.LazyListScope.queixaStep(
    state: com.bioacupunt.prontuario.presentation.SupremoUiState,
    vm: SupremoViewModel,
) {
    item {
        SupremoCard {
            SectionHeader(title = "Queixa principal")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.draft.chiefComplaint,
                onValueChange = vm::updateChiefComplaint,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
        }
        Spacer(Modifier.height(14.dp))
    }
    item {
        SupremoCard {
            SectionHeader(title = "Mapa corporal", subtitle = "Toque na região · toque de novo para ciclar a intensidade (EVA)")
            Spacer(Modifier.height(10.dp))
            FlowRowCompatAt {
                BodyRegion.entries.forEach { region ->
                    val eva = state.draft.bodyMarks.firstOrNull { it.region == region }?.intensity ?: 0
                    val label = if (eva > 0) "${region.label} · $eva" else region.label
                    SelectableChip(label, eva > 0, {
                        val next = when (eva) { 0 -> 3; 3 -> 6; 6 -> 9; else -> 0 }
                        vm.setRegionEva(region, next)
                    })
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
    item {
        SupremoCard {
            Text("O QUE MELHORA", style = MaterialTheme.typography.labelMedium, color = Primary)
            Spacer(Modifier.height(8.dp))
            FlowRowCompatAt {
                relievingOptions.forEach { f -> SelectableChip(f, f in state.draft.relievingFactors, { vm.toggleRelieving(f) }) }
            }
            Spacer(Modifier.height(14.dp))
            Text("O QUE PIORA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            FlowRowCompatAt {
                aggravatingOptions.forEach { f -> SelectableChip(f, f in state.draft.aggravatingFactors, { vm.toggleAggravating(f) }) }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.interrogatorioStep(
    state: com.bioacupunt.prontuario.presentation.SupremoUiState,
    vm: SupremoViewModel,
) {
    interrogationCategories.forEach { (category, items) ->
        item {
            SupremoCard {
                Text(category, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(8.dp))
                FlowRowCompatAt {
                    items.forEach { i ->
                        val key = "$category:$i"
                        SelectableChip(i, key in state.draft.reviewOfSystems, { vm.toggleReviewOfSystems(key) })
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    item {
        SupremoCard {
            Text("Notas do interrogatório", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.draft.interrogationNotes,
                onValueChange = vm::updateInterrogationNotes,
                placeholder = { Text("Outros sinais e sintomas...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.zangFuStep(
    state: com.bioacupunt.prontuario.presentation.SupremoUiState,
    vm: SupremoViewModel,
) {
    item {
        SupremoCard {
            SectionHeader(title = "Zang Fu · órgãos", subtitle = "toque p/ ciclar: normal → deficiência → estagnação")
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Organ.entries.forEach { organ ->
                    val label = vm.organStateLabel(organ)
                    val color = when (label) {
                        "Deficiência" -> SemanticWarning
                        "Estagnação" -> MaterialTheme.colorScheme.error
                        else -> TextMuted
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.background)
                            .clickable { vm.cycleOrganState(organ) }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(organ.label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        Box(modifier = Modifier.clip(MaterialTheme.shapes.extraLarge).background(color.copy(alpha = 0.15f)).padding(horizontal = 12.dp, vertical = 3.dp)) {
                            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = color)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
    item {
        SupremoCard {
            SectionHeader(title = "5 Elementos (Wu Xing)")
            Spacer(Modifier.height(12.dp))
            val active = state.draft.elements
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Element.entries.forEach { el ->
                    val isActive = el in active
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape)
                                .background(if (isActive) Primary else MaterialTheme.colorScheme.background)
                                .border(1.dp, if (isActive) Primary else MaterialTheme.colorScheme.outline, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(el.label.take(1), color = if (isActive) Color.White else TextMuted, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(el.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Ciclo de geração (Sheng) e controle (Ke). Desequilíbrio num elemento reverbera nos vizinhos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.linguaPulsoStep(
    state: com.bioacupunt.prontuario.presentation.SupremoUiState,
    vm: SupremoViewModel,
) {
    item {
        val context = LocalContext.current
        val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            vm.updateTonguePhoto(uri?.toString())
        }
        SupremoCard {
            SectionHeader(title = "Exame da língua")
            Spacer(Modifier.height(10.dp))
            val tongue = state.draft.tongue
            Text("COR", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            FlowRowCompatAt {
                TongueBodyColor.entries.filter { it != TongueBodyColor.UNSET }.forEach { c ->
                    SelectableChip(c.label, tongue.bodyColor == c, { vm.updateTongue(tongue.copy(bodyColor = if (tongue.bodyColor == c) TongueBodyColor.UNSET else c)) })
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("SABURRA", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            FlowRowCompatAt {
                TongueCoatingColor.entries.filter { it != TongueCoatingColor.UNSET }.forEach { c ->
                    SelectableChip(c.label, tongue.coatingColor == c, { vm.updateTongue(tongue.copy(coatingColor = if (tongue.coatingColor == c) TongueCoatingColor.UNSET else c)) })
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("ESPESSURA", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            FlowRowCompatAt {
                TongueCoatingThickness.entries.filter { it != TongueCoatingThickness.UNSET }.forEach { c ->
                    SelectableChip(c.label, tongue.coatingThickness == c, { vm.updateTongue(tongue.copy(coatingThickness = if (tongue.coatingThickness == c) TongueCoatingThickness.UNSET else c)) })
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("UMIDADE", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            FlowRowCompatAt {
                TongueMoisture.entries.filter { it != TongueMoisture.UNSET }.forEach { c ->
                    SelectableChip(c.label, tongue.moisture == c, { vm.updateTongue(tongue.copy(moisture = if (tongue.moisture == c) TongueMoisture.UNSET else c)) })
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { photoPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (tongue.photoUri != null) "Foto anexada · trocar" else "Anexar foto da língua")
            }
        }
        Spacer(Modifier.height(14.dp))
    }
    item {
        SupremoCard {
            SectionHeader(title = "Pulsologia · 28 qualidades", subtitle = "por posição e lado")
            Spacer(Modifier.height(10.dp))
        }
    }
    Wrist.entries.forEach { wrist ->
        PulsePosition.entries.forEach { position ->
            item {
                PulseCard(wrist, position, state.draft.pulse.readings) { depth, quality ->
                    val current = state.draft.pulse.at(wrist, position, depth)
                    val qualities = current?.qualities.orEmpty()
                    val next = if (quality in qualities) qualities - quality else qualities + quality
                    vm.setPulseReading(com.bioacupunt.prontuario.domain.model.PulseReading(wrist, position, depth, next))
                }
            }
        }
    }
    item {
        SupremoCard {
            Text("Frequência", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("Lento" to 55, "Normal" to 75, "Rápido" to 95).forEach { (label, bpm) ->
                    val selected = state.draft.pulse.rateBpm == bpm
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (selected) Primary else MaterialTheme.colorScheme.background)
                            .border(1.dp, if (selected) Primary else MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                            .clickable { vm.updatePulseRate(bpm) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.draft.pulse.notes,
                onValueChange = vm::updatePulseNotes,
                placeholder = { Text("Impressão geral (ex: pulso em corda e fino)...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.planoStep(
    state: com.bioacupunt.prontuario.presentation.SupremoUiState,
    vm: SupremoViewModel,
    finalizing: Boolean,
    onFinalize: (String) -> Unit,
) {
    item {
        SupremoCard {
            SectionHeader(title = "Segurança clínica", subtitle = "Triagem determinística, sem IA.")
            Spacer(Modifier.height(12.dp))
            ClinicalSafetyPanel(verdict = state.verdict)
        }
        Spacer(Modifier.height(14.dp))
    }
    item {
        val securePrefs = AppContainer.securePreferences
        val enabledNames = remember {
            securePrefs.enabledTechniquesCsv.split(",").filter { it.isNotBlank() }.toSet()
                .ifEmpty { Technique.entries.map { it.name }.toSet() }
        }
        SupremoCard {
            Text("Técnicas selecionadas", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(10.dp))
            FlowRowCompatAt {
                Technique.entries.filter { it.name in enabledNames }.forEach { t ->
                    SelectableChip(t.label, t in state.proposal.techniques, { vm.toggleTechnique(t) })
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
    item {
        SupremoCard {
            Text("Orientações e dietoterapia", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.draft.orientations,
                onValueChange = vm::updateOrientations,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
        }
        Spacer(Modifier.height(14.dp))
    }
    item {
        Button(
            onClick = {
                vm.save()
                val summary = buildString {
                    if (state.draft.chiefComplaint.isNotBlank()) appendLine("Queixa: ${state.draft.chiefComplaint}")
                    if (state.draft.orientations.isNotBlank()) appendLine("Orientações: ${state.draft.orientations}")
                }
                onFinalize(summary.trim())
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !finalizing,
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
        ) {
            if (finalizing) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Finalizar atendimento", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowCompatAt(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        content()
    }
}
