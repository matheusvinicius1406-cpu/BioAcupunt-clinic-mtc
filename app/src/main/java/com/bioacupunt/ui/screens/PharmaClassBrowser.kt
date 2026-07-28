package com.bioacupunt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Coronavirus
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bioacupunt.pharma.domain.model.ClasseTerapeuticaSummary
import com.bioacupunt.pharma.domain.model.Medicamento
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.TextMuted

/**
 * Ícone genérico por classe terapêutica — não existe fonte aberta em bulk de fotos reais
 * de embalagem (Bulário ANVISA é per-item, atrás de Cloudflare, sem API; ver CLAUDE.md).
 * Heurística por palavra-chave sobre a string ANVISA (maiúscula, sem acento); classes não
 * mapeadas caem no ícone genérico — nunca ficam sem ícone nenhum.
 */
fun iconForClasseTerapeutica(classe: String): ImageVector {
    val c = classe.uppercase()
    return when {
        c.contains("VACINA") -> Icons.Default.Vaccines
        c.contains("ANTIBIOTIC") || c.contains("CEFALOSPORIN") || c.contains("PENICILIN") ||
            c.contains("ANTIPARASITARI") || c.contains("ANTI-HELMINTIC") || c.contains("ANTIFUNGIC") ||
            c.contains("ANTIMICOTIC") || c.contains("ANTINFECCIOS") -> Icons.Default.BugReport
        c.contains("ANTIVIRAL") || c.contains("ANTIRRETROVIRAL") -> Icons.Default.Coronavirus
        c.contains("ANTIDEPRESSIV") || c.contains("ANSIOLITIC") || c.contains("ANTIPSICOTIC") ||
            c.contains("SISTEMA NERVOSO") || c.contains("PSIQUIATR") -> Icons.Default.Psychology
        c.contains("HIPNOTIC") || c.contains("SEDATIV") || c.contains("ANESTESIC") -> Icons.Default.NightsStay
        c.contains("ANTICONVULSIV") -> Icons.Default.Bolt
        c.contains("ANTIDIABETIC") -> Icons.Default.Bloodtype
        c.contains("ANTI-HIPERTENSIV") || c.contains("ANTIHIPERTENSIV") || c.contains("VASODILATADOR") ||
            c.contains("CARDIO") || c.contains("ANTIARRITMIC") -> Icons.Default.MonitorHeart
        c.contains("ANTILIPEMIC") || c.contains("DIURETIC") || c.contains("RENAL") -> Icons.Default.WaterDrop
        c.contains("ANTICONCEPCIONA") || c.contains("HORMON") -> Icons.Default.CalendarMonth
        c.contains("ANTIEMETIC") || c.contains("ANTINAUSEANT") || c.contains("ANTIULCEROS") ||
            c.contains("GASTR") || c.contains("DIGESTIV") -> Icons.Default.Restaurant
        c.contains("EXPECTORANT") || c.contains("MUCOLITIC") || c.contains("RESPIRATOR") ||
            c.contains("BRONC") || c.contains("ANTIASMATIC") -> Icons.Default.Air
        c.contains("GLICOCORTICOID") || c.contains("CORTICOID") || c.contains("IMUNOSSUPRESSOR") ||
            c.contains("IMUNOLOGIC") -> Icons.Default.Shield
        c.contains("OFTALM") || c.contains("GLAUCOMA") -> Icons.Default.Visibility
        c.contains("DERMATOLOG") || c.contains("TOPIC") || c.contains("PELE") -> Icons.Default.Face
        c.contains("VITAMIN") || c.contains("SUPLEMENTO") || c.contains("MINERAL") || c.contains("FITOTERAPIC") -> Icons.Default.Eco
        c.contains("ANTINFLAMATORI") || c.contains("ANALGESIC") || c.contains("ANTIRREUMATIC") ||
            c.contains("ANTIREUMATIC") -> Icons.Default.Healing
        else -> Icons.Default.Medication
    }
}

/** "ANTI-HIPERTENSIVOS SIMPLES" -> "Anti-hipertensivos simples" — legível sem gritar em maiúsculas. */
fun classeTerapeuticaLabel(classe: String): String =
    classe.lowercase().replaceFirstChar { it.uppercase() }

@Composable
private fun ClasseTerapeuticaRow(summary: ClasseTerapeuticaSummary, onSelect: (String) -> Unit) {
    Card(
        onClick = { onSelect(summary.classeTerapeutica) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(iconForClasseTerapeutica(summary.classeTerapeutica), null, tint = Primary, modifier = Modifier.size(24.dp)) }
            Column(Modifier.weight(1f)) {
                Text(
                    classeTerapeuticaLabel(summary.classeTerapeutica),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    "${summary.count} medicamento${if (summary.count == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall, color = TextMuted,
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = TextMuted, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun MedicamentoClassRow(classe: String, med: Medicamento, onSelect: (Medicamento) -> Unit) {
    Card(
        onClick = { onSelect(med) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(iconForClasseTerapeutica(classe), null, tint = Primary, modifier = Modifier.size(18.dp)) }
            Column(Modifier.weight(1f)) {
                Text(med.nomeComercial, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(med.principiosAtivos.joinToString(), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
}

/**
 * Itens da grade de classes terapêuticas, direto num [LazyListScope] já existente — use
 * isto (não [ClasseTerapeuticaGrid]) quando o chamador já está dentro de um `LazyColumn`
 * (ex.: [PrescricaoTab]). Um `LazyColumn` dentro de outro `LazyColumn` da mesma orientação
 * crasha em runtime ("measured with an infinity maximum height constraint").
 */
fun LazyListScope.classeTerapeuticaGridItems(classes: List<ClasseTerapeuticaSummary>, onSelect: (String) -> Unit) {
    items(classes, key = { it.classeTerapeutica }) { summary -> ClasseTerapeuticaRow(summary, onSelect) }
    if (classes.isEmpty()) {
        item {
            Text(
                "Catálogo ainda não carregado.",
                style = MaterialTheme.typography.bodySmall, color = TextMuted,
                modifier = Modifier.padding(vertical = 20.dp),
            )
        }
    }
}

/** Itens da lista de medicamentos de uma classe, direto num [LazyListScope] já existente. */
fun LazyListScope.medicamentoClassResultItems(
    classe: String,
    items: List<Medicamento>,
    onBack: () -> Unit,
    onSelect: (Medicamento) -> Unit,
) {
    item {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Column(Modifier.weight(1f)) {
                Text(classeTerapeuticaLabel(classe), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Text("${items.size} medicamento${if (items.size == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
    items(items, key = { it.id }) { med -> MedicamentoClassRow(classe, med, onSelect) }
}

/** Grade de classes terapêuticas — versão standalone (cria seu próprio `LazyColumn`). */
@Composable
fun ClasseTerapeuticaGrid(classes: List<ClasseTerapeuticaSummary>, onSelect: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        classeTerapeuticaGridItems(classes, onSelect)
    }
}

/** Lista de medicamentos de UMA classe — versão standalone (cria seu próprio `LazyColumn`). */
@Composable
fun MedicamentoClassResultsList(
    classe: String,
    items: List<Medicamento>,
    onBack: () -> Unit,
    onSelect: (Medicamento) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        medicamentoClassResultItems(classe, items, onBack, onSelect)
    }
}
