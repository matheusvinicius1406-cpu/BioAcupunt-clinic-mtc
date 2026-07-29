package com.bioacupunt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.di.AppContainer
import com.bioacupunt.pharma.domain.model.CategoriaRegulatoria
import com.bioacupunt.pharma.domain.model.Medicamento
import com.bioacupunt.pharma.domain.model.SeveridadeInteracao
import com.bioacupunt.pharma.presentation.FarmacologiaCuradoriaViewModel
import com.bioacupunt.prontuario.domain.model.ClinicalFlag
import com.bioacupunt.ui.design.SelectableChip
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.TextMuted
import com.bioacupunt.ui.theme.statusColors

/**
 * Curadoria Farmacológica — a médica preenche, com a bula em mãos, a camada clínica
 * de um item do catálogo ANVISA. Nada aqui é gerado: todo campo começa vazio.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FarmacologiaCuradoriaScreen(onBack: () -> Unit) {
    val vm: FarmacologiaCuradoriaViewModel = viewModel(factory = AppContainer.farmacologiaCuradoriaViewModelFactory)
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Curadoria Farmacológica") },
                navigationIcon = {
                    IconButton(onClick = { if (state.selected != null) vm.clearSelection() else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
            )
        },
    ) { padding ->
        val selected = state.selected
        val draft = state.draft
        if (selected == null) {
            Column(Modifier.padding(padding).padding(16.dp)) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = vm::onQueryChanged,
                    label = { Text("Buscar medicamento no catálogo") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                if (state.isSearching) CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                val selectedClasse = state.selectedClasse
                when {
                    state.query.isNotBlank() -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.results, key = { it.id }) { med ->
                            MedicamentoResultCard(med, onClick = { vm.selectMedicamento(med) })
                        }
                    }
                    selectedClasse != null -> MedicamentoClassResultsList(
                        classe = selectedClasse,
                        items = state.classResults,
                        onBack = vm::clearClasseSelection,
                        onSelect = vm::selectMedicamento,
                    )
                    else -> {
                        if (state.loadingClasses) CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        ClasseTerapeuticaGrid(classes = state.classes, onSelect = vm::selectClasse)
                    }
                }
            }
        } else if (draft == null) {
            CircularProgressIndicator(modifier = Modifier.padding(32.dp))
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text(selected.nomeComercial, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${selected.principiosAtivos.joinToString()} · ${selected.classeTerapeutica}",
                        style = MaterialTheme.typography.bodySmall, color = TextMuted,
                    )
                    Text(
                        "Status: ${draft.status.name}${if (draft.autor.isNotBlank()) " · por ${draft.autor}" else ""}",
                        style = MaterialTheme.typography.labelSmall, color = TextMuted,
                    )
                }

                item { SectionLabel("POSOLOGIA") }
                item {
                    OutlinedTextField(
                        value = draft.posologiaAdulto, onValueChange = vm::onPosologiaAdultoChanged,
                        label = { Text("Adulto (obrigatório pra aprovar)") }, modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = draft.posologiaPediatrica, onValueChange = vm::onPosologiaPediatricaChanged,
                        label = { Text("Pediátrica") }, modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = draft.posologiaIdoso, onValueChange = vm::onPosologiaIdosoChanged,
                        label = { Text("Idoso") }, modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = draft.posologiaRenal, onValueChange = vm::onPosologiaRenalChanged,
                        label = { Text("Insuficiência renal") }, modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = draft.posologiaHepatica, onValueChange = vm::onPosologiaHepaticaChanged,
                        label = { Text("Insuficiência hepática") }, modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = draft.viaAdministracao, onValueChange = vm::onViaChanged,
                        label = { Text("Via de administração (obrigatório pra aprovar)") }, modifier = Modifier.fillMaxWidth(),
                    )
                }

                item { SectionLabel("CONTRAINDICAÇÕES ABSOLUTAS") }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ClinicalFlag.entries.forEach { flag ->
                            SelectableChip(flag.label, flag in draft.contraindicacoesAbsolutas, { vm.toggleContraindicacaoAbsoluta(flag) })
                        }
                    }
                }
                item { SectionLabel("CONTRAINDICAÇÕES RELATIVAS") }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ClinicalFlag.entries.forEach { flag ->
                            SelectableChip(flag.label, flag in draft.contraindicacoesRelativas, { vm.toggleContraindicacaoRelativa(flag) })
                        }
                    }
                }

                item { SectionLabel("ALÉRGENOS / COMPOSIÇÃO (separados por vírgula)") }
                item {
                    OutlinedTextField(
                        value = draft.alergenos.joinToString(", "),
                        onValueChange = vm::onAlergenosChanged,
                        placeholder = { Text("lactose, glúten, gelatina, corante...") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item { SectionLabel("INTERAÇÕES MEDICAMENTOSAS") }
                items(draft.interacoes, key = { it.outroMedicamentoId }) { interacao ->
                    Row(
                        Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${interacao.outroNome} — ${interacao.severidade}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(interacao.descricao, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        IconButton(onClick = { vm.removeInteracao(interacao.outroMedicamentoId) }) { Icon(Icons.Default.Delete, null, tint = statusColors().danger) }
                    }
                }
                item { AddInteracaoForm(vm, state) }

                item { SectionLabel("EFEITOS ADVERSOS") }
                items(draft.efeitosAdversos, key = { it.id }) { efeito ->
                    Row(
                        Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(efeito.descricao, style = MaterialTheme.typography.bodyMedium)
                            if (efeito.frequencia.isNotBlank()) Text(efeito.frequencia, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        IconButton(onClick = { vm.removeEfeitoAdverso(efeito.id) }) { Icon(Icons.Default.Delete, null, tint = statusColors().danger) }
                    }
                }
                item { AddEfeitoAdversoForm(vm, state) }

                item { SectionLabel("VISÃO INTEGRATIVA (MTC) — opcional") }
                item {
                    OutlinedTextField(
                        value = draft.visaoIntegrativaMtc, onValueChange = vm::onVisaoMtcChanged,
                        placeholder = { Text("Natureza energética, sabores, Zang-Fu, estratégias complementares...") },
                        modifier = Modifier.fillMaxWidth(), minLines = 3,
                    )
                }

                state.error?.let { item { Text(it, color = statusColors().danger, style = MaterialTheme.typography.bodySmall) } }
                state.savedMessage?.let { item { Text(it, color = statusColors().success, style = MaterialTheme.typography.bodySmall) } }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = vm::saveDraft, enabled = !state.saving, modifier = Modifier.weight(1f)) {
                            Text("Salvar rascunho")
                        }
                        Button(
                            onClick = vm::approve,
                            enabled = !state.saving && state.canApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            modifier = Modifier.weight(1f),
                        ) { Text("Aprovar") }
                    }
                    if (!state.canApprove) {
                        Text(
                            "Preencha posologia adulto e via de administração pra habilitar aprovação.",
                            style = MaterialTheme.typography.labelSmall, color = TextMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = TextMuted, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun MedicamentoResultCard(medicamento: Medicamento, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(medicamento.nomeComercial, fontWeight = FontWeight.SemiBold)
            Text(
                "${medicamento.principiosAtivos.joinToString()} · ${medicamento.categoriaRegulatoria.label()}",
                style = MaterialTheme.typography.bodySmall, color = TextMuted,
            )
        }
    }
}

private fun CategoriaRegulatoria.label(): String = when (this) {
    CategoriaRegulatoria.GENERICO -> "Genérico"
    CategoriaRegulatoria.SIMILAR -> "Similar"
    CategoriaRegulatoria.REFERENCIA -> "Referência"
    CategoriaRegulatoria.FITOTERAPICO -> "Fitoterápico"
    CategoriaRegulatoria.BIOLOGICO -> "Biológico"
    CategoriaRegulatoria.NOVO -> "Novo"
    CategoriaRegulatoria.ESPECIFICO -> "Específico"
    CategoriaRegulatoria.DINAMIZADO -> "Dinamizado"
    CategoriaRegulatoria.OUTRO -> "Outro"
}

@Composable
private fun AddInteracaoForm(vm: FarmacologiaCuradoriaViewModel, state: com.bioacupunt.pharma.presentation.FarmacologiaCuradoriaUiState) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        OutlinedTextField(
            value = state.interacaoQuery,
            onValueChange = { vm.onInteracaoQueryChanged(it); expanded = true },
            label = { Text("Buscar outro medicamento") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (expanded && state.interacaoResults.isNotEmpty() && state.interacaoOutro == null) {
            Column {
                state.interacaoResults.take(5).forEach { med ->
                    Text(
                        med.nomeComercial,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        if (state.interacaoOutro != null) {
            Row {
                Text("Selecionado: ${state.interacaoOutro.nomeComercial}", style = MaterialTheme.typography.labelMedium)
            }
        }
        SeveridadeDropdown(state.interacaoSeveridade, vm::onInteracaoSeveridadeChanged)
        OutlinedTextField(
            value = state.interacaoDescricao, onValueChange = vm::onInteracaoDescricaoChanged,
            label = { Text("Descrição clínica da interação") }, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(onClick = { vm.addInteracao(); expanded = false }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Adicionar interação")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeveridadeDropdown(value: SeveridadeInteracao, onChange: (SeveridadeInteracao) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Severidade") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SeveridadeInteracao.entries.forEach { sev ->
                DropdownMenuItem(
                    text = { Text(sev.name) },
                    onClick = { onChange(sev); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun AddEfeitoAdversoForm(vm: FarmacologiaCuradoriaViewModel, state: com.bioacupunt.pharma.presentation.FarmacologiaCuradoriaUiState) {
    Column(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        OutlinedTextField(
            value = state.efeitoDescricao, onValueChange = vm::onEfeitoDescricaoChanged,
            label = { Text("Efeito adverso") }, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.efeitoFrequencia, onValueChange = vm::onEfeitoFrequenciaChanged,
            label = { Text("Frequência (ex.: comum, rara)") }, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(onClick = vm::addEfeitoAdverso, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Adicionar efeito adverso")
        }
    }
}
