package com.bioacupunt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.di.AppContainer
import com.bioacupunt.pharma.domain.model.FormularioMedicamento
import com.bioacupunt.pharma.domain.model.Medicamento
import com.bioacupunt.pharma.presentation.FarmacologiaViewModel
import com.bioacupunt.ui.design.SupremoClickableCard
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.TextMuted
import com.bioacupunt.ui.theme.statusColors

/**
 * BioAcupunt Pharma Library — consulta de medicamentos, funciona sem paciente
 * selecionado. Identificação vem sempre do catálogo ANVISA (bulk, real); a seção
 * clínica (posologia/interação/contraindicação) só aparece quando existe
 * [FormularioMedicamento] APROVADO — nunca finge que catálogo sozinho é curadoria.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmacologiaScreen() {
    val vm: FarmacologiaViewModel = viewModel(factory = AppContainer.farmacologiaViewModelFactory)
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Farmacologia", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Biblioteca de medicamentos — consulta livre, não exige paciente selecionado.",
            style = MaterialTheme.typography.bodySmall, color = TextMuted,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChanged,
            label = { Text("Buscar por nome, princípio ativo ou classe") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        if (state.isSearching) CircularProgressIndicator(modifier = Modifier.padding(16.dp))

        val selectedClasse = state.selectedClasse
        when {
            // Busca ativa: resultados de texto, do jeito que já era.
            state.query.isNotBlank() -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.results, key = { it.id }) { med ->
                    SupremoClickableCard(
                        onClick = { vm.selectMedicamento(med) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(med.nomeComercial, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${med.principiosAtivos.joinToString()} · ${med.classeTerapeutica}",
                            style = MaterialTheme.typography.bodySmall, color = TextMuted,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                if (!state.isSearching && state.results.isEmpty()) {
                    item {
                        Text(
                            "Nenhum medicamento encontrado no catálogo ANVISA pra essa busca.",
                            style = MaterialTheme.typography.bodySmall, color = TextMuted,
                            modifier = Modifier.padding(vertical = 20.dp),
                        )
                    }
                }
            }
            // Sem busca + classe escolhida: lista da classe.
            selectedClasse != null -> MedicamentoClassResultsList(
                classe = selectedClasse,
                items = state.classResults,
                onBack = vm::clearClasseSelection,
                onSelect = vm::selectMedicamento,
            )
            // Sem busca, sem classe: navegação por classe terapêutica — igual à Biblioteca.
            else -> {
                if (state.loadingClasses) CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                ClasseTerapeuticaGrid(classes = state.classes, onSelect = vm::selectClasse)
            }
        }
    }

    val selected = state.selected
    if (selected != null) {
        ModalBottomSheet(onDismissRequest = { vm.clearSelection() }) {
            MedicamentoDetail(
                medicamento = selected,
                formulario = state.selectedFormulario,
                hasApprovedClinicalContent = state.hasApprovedClinicalContent,
                loading = state.loadingDetail,
            )
        }
    }
}

@Composable
private fun MedicamentoDetail(
    medicamento: Medicamento,
    formulario: FormularioMedicamento?,
    hasApprovedClinicalContent: Boolean,
    loading: Boolean,
) {
    Column(Modifier.fillMaxWidth().padding(20.dp)) {
        Text(medicamento.nomeComercial, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(medicamento.principiosAtivos.joinToString(), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        Spacer(Modifier.height(12.dp))

        SectionTitle("IDENTIFICAÇÃO")
        InfoRow("Classe terapêutica", medicamento.classeTerapeutica)
        InfoRow("Categoria regulatória", medicamento.categoriaRegulatoria.name)
        InfoRow("Empresa detentora", medicamento.empresaDetentora)
        InfoRow("Registro ANVISA", medicamento.id)
        InfoRow("Situação", if (medicamento.situacaoAtiva) "Ativo" else "Inativo")

        Spacer(Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else if (!hasApprovedClinicalContent) {
            Row(
                Modifier.fillMaxWidth()
                    .background(TextMuted.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = TextMuted)
                Column(Modifier.padding(start = 10.dp)) {
                    Text("Sem curadoria clínica ainda", fontWeight = FontWeight.SemiBold, color = TextMuted)
                    Text(
                        "Apenas dado de registro ANVISA. Posologia, interações e contraindicações " +
                            "ainda não foram revisadas por um profissional pra este item.",
                        style = MaterialTheme.typography.bodySmall, color = TextMuted,
                    )
                }
            }
        } else if (formulario != null) {
            SectionTitle("POSOLOGIA")
            InfoRow("Adulto", formulario.posologiaAdulto)
            if (formulario.posologiaPediatrica.isNotBlank()) InfoRow("Pediátrica", formulario.posologiaPediatrica)
            if (formulario.posologiaIdoso.isNotBlank()) InfoRow("Idoso", formulario.posologiaIdoso)
            if (formulario.posologiaRenal.isNotBlank()) InfoRow("Insuf. renal", formulario.posologiaRenal)
            if (formulario.posologiaHepatica.isNotBlank()) InfoRow("Insuf. hepática", formulario.posologiaHepatica)
            InfoRow("Via", formulario.viaAdministracao)

            if (formulario.contraindicacoesAbsolutas.isNotEmpty() || formulario.contraindicacoesRelativas.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                SectionTitle("CONTRAINDICAÇÕES")
                formulario.contraindicacoesAbsolutas.forEach {
                    Text("• ${it.label} (absoluta)", color = statusColors().danger, style = MaterialTheme.typography.bodySmall)
                }
                formulario.contraindicacoesRelativas.forEach {
                    Text("• ${it.label} (relativa)", color = statusColors().warning, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (formulario.alergenos.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                SectionTitle("ALÉRGENOS / COMPOSIÇÃO")
                Text(formulario.alergenos.joinToString(), style = MaterialTheme.typography.bodySmall)
            }

            if (formulario.interacoes.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                SectionTitle("INTERAÇÕES")
                formulario.interacoes.forEach {
                    Text("• ${it.outroNome} (${it.severidade}) — ${it.descricao}", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (formulario.efeitosAdversos.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                SectionTitle("EFEITOS ADVERSOS")
                formulario.efeitosAdversos.forEach {
                    Text("• ${it.descricao}${if (it.frequencia.isNotBlank()) " (${it.frequencia})" else ""}", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (formulario.visaoIntegrativaMtc.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Column(
                    Modifier.fillMaxWidth()
                        .background(Primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                ) {
                    Text("VISÃO INTEGRATIVA (MTC)", style = MaterialTheme.typography.labelMedium, color = Primary, fontWeight = FontWeight.Bold)
                    Text(
                        "Informação de Medicina Tradicional Chinesa — não substitui a avaliação biomédica acima.",
                        style = MaterialTheme.typography.labelSmall, color = TextMuted,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(formulario.visaoIntegrativaMtc, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = TextMuted, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun InfoRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}
