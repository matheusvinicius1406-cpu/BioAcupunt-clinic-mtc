package com.bioacupunt.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.di.AppContainer
import com.bioacupunt.prontuario.domain.model.MtcAssessment
import com.bioacupunt.prontuario.domain.model.ProntuarioEntry
import com.bioacupunt.prontuario.presentation.EvolucaoViewModel
import com.bioacupunt.ui.theme.Accent
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.SemanticError
import com.bioacupunt.ui.theme.SemanticSuccess
import com.bioacupunt.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvolucaoScreen(patientId: Long, onBack: () -> Unit = {}) {
    val vm: EvolucaoViewModel = viewModel(
        key = "evolucao-$patientId",
        factory = AppContainer.evolucaoViewModelFactory(patientId),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val comparison = remember(state.history) { vm.comparison() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evolução Clínica") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") } },
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                EvaTrendCard(state.history, evaFor = vm::evaFor)
            }
            if (comparison.hasData) {
                item { ComparisonBanner(comparison) }
            }
            item {
                Text("TIMELINE DE SESSÕES", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            }
            if (state.entries.isEmpty()) {
                item { Text("Sem sessões registradas.", color = TextMuted, style = MaterialTheme.typography.bodySmall) }
            } else {
                items(state.entries, key = { it.id }) { entry ->
                    TimelineEntry(entry)
                }
            }
        }
    }
}

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
                            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Accent, Primary)))
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("$eva", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    Text(assessment.date.take(5), style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
                }
            }
        }
    }
}

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
        Icon(Icons.Default.TrendingUp, null, tint = Primary, modifier = Modifier.size(20.dp))
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

@Composable
private fun TimelineEntry(entry: ProntuarioEntry) {
    var open by remember { mutableStateOf(false) }
    val rotation by animateFloatAsStateCompat(if (open) 180f else 0f)
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Column(modifier = Modifier.width(20.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.padding(top = 4.dp).size(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Primary))
            Box(modifier = Modifier.weight(1f).width(2.dp).background(MaterialTheme.colorScheme.outline))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f).padding(bottom = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { open = !open },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("${entry.type.label} · ${entry.date.take(10)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                }
                Icon(Icons.Default.ExpandMore, null, tint = TextMuted, modifier = Modifier.rotate(rotation))
            }
            AnimatedVisibility(visible = open) {
                Text(entry.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun animateFloatAsStateCompat(target: Float) = androidx.compose.animation.core.animateFloatAsState(target, label = "rotation")
