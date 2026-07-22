package com.bioacupunt.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.data.local.model.IngestionJobEntity
import com.bioacupunt.mkis.domain.pipeline.PipelineService
import com.bioacupunt.ui.theme.*

/**
 * PIPELINE MONITOR — tela de gerenciamento do pipeline de ingestão MKIS.
 *
 * Exibe:
 * - Overview com pipeline state + estatísticas (total, pendentes, concluídos, falhas)
 * - Filtros por status + busca textual
 * - Lista de jobs com status, progresso, ações (retry, quarentena, cancelar)
 * - Bottom sheet de detalhes do job selecionado
 * - Seção de quarentena gerenciável
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipelineMonitorScreen(onBack: () -> Unit = {}) {
    val vm = viewModel<PipelineMonitorViewModel>(factory = pipelineMonitorFactory)
    val stats by vm.stats.collectAsStateWithLifecycle()
    val filteredJobs by vm.filteredJobs.collectAsStateWithLifecycle()
    val pipelineState by vm.pipelineState.collectAsStateWithLifecycle()
    val statusFilter by vm.statusFilter.collectAsStateWithLifecycle()
    val searchText by vm.searchText.collectAsStateWithLifecycle()
    val quarantinedJobs by vm.quarantinedJobs.collectAsStateWithLifecycle()

    var selectedJob by remember { mutableStateOf<IngestionJobEntity?>(null) }
    var showQuarantineSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pipeline MKIS", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") }
                },
                actions = {
                    // Pipeline toggle
                    when (pipelineState) {
                        is PipelineService.PipelineState.Running -> {
                            IconButton(onClick = { vm.stopPipeline() }) {
                                Icon(Icons.Default.Pause, "Pausar pipeline", tint = SemanticError)
                            }
                        }
                        else -> {
                            IconButton(onClick = { vm.startPipeline() }) {
                                Icon(Icons.Default.PlayArrow, "Iniciar pipeline", tint = Primary)
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Pipeline state card ───────────────────────────
            item {
                PipelineStatusCard(pipelineState, stats, vm::startPipeline, vm::stopPipeline)
            }

            // ── Stats cards row ───────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatCard(Modifier.weight(1f), "Pendentes", stats.pending.toString(), Accent)
                    StatCard(Modifier.weight(1f), "Processando", stats.processing.toString(), SemanticInfo)
                    StatCard(Modifier.weight(1f), "Concluídos", stats.completed.toString(), Primary)
                    StatCard(Modifier.weight(1f), "Falhas", stats.failed.toString(), SemanticError)
                }
            }

            // ── Filters ───────────────────────────────────────
            item {
                PipelineFilters(
                    statusFilter = statusFilter,
                    searchText = searchText,
                    onStatusChange = vm::setStatusFilter,
                    onSearchChange = vm::setSearchText,
                    totalJobs = filteredJobs.size,
                )
            }

            // ── Quarantine banner (if any) ────────────────────
            if (quarantinedJobs.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SemanticWarningBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SemanticWarning),
                        modifier = Modifier.clickable { showQuarantineSheet = true },
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Warning, null, tint = SemanticWarning, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${quarantinedJobs.size} job(s) em quarentena",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = SemanticWarning,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(Icons.Default.ChevronRight, null, tint = SemanticWarning)
                        }
                    }
                }
            }

            // ── Job list header ───────────────────────────────
            item {
                Text(
                    "JOBS · ${filteredJobs.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── Empty state ───────────────────────────────────
            if (filteredJobs.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(Icons.Default.Storage, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Nenhum job encontrado",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextMuted,
                            )
                            Text(
                                "Importe conteúdo na Curadoria para ver jobs aqui.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                        }
                    }
                }
            }

            // ── Job cards ─────────────────────────────────────
            items(filteredJobs, key = { it.id }) { job ->
                JobCard(
                    job = job,
                    vm = vm,
                    onClick = { selectedJob = job },
                )
            }
        }
    }

    // ── Job detail bottom sheet ─────────────────────────────
    selectedJob?.let { job ->
        JobDetailSheet(
            job = job,
            vm = vm,
            onDismiss = { selectedJob = null },
        )
    }

    // ── Quarantine management sheet ─────────────────────────
    if (showQuarantineSheet) {
        QuarantineSheet(
            jobs = quarantinedJobs,
            vm = vm,
            onDismiss = { showQuarantineSheet = false },
        )
    }
}

// ======================== Sub-components ========================

@Composable
private fun PipelineStatusCard(
    state: PipelineService.PipelineState,
    stats: PipelineMonitorViewModel.JobStats,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val (statusColor, statusLabel, statusIcon) = when (state) {
        is PipelineService.PipelineState.Running -> Triple(Primary, "Rodando", Icons.Default.PlayCircle)
        is PipelineService.PipelineState.Processing -> Triple(SemanticInfo, "Processando…", Icons.Default.Sync)
        is PipelineService.PipelineState.Idle -> Triple(TextMuted, "Parado", Icons.Default.PauseCircle)
        is PipelineService.PipelineState.Failed -> Triple(SemanticError, "Erro", Icons.Default.Error)
    }

    val bgColor by animateColorAsState(
        targetValue = statusColor.copy(alpha = 0.08f),
        label = "statusBg",
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Pipeline de Ingestão", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text(
                    statusLabel + " · ${stats.completed}/${stats.total} concluídos",
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                )
            }
            // Overall progress bar
            if (stats.total > 0) {
                val progress = stats.completed.toFloat() / stats.total
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.width(80.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = statusColor,
                    trackColor = statusColor.copy(alpha = 0.15f),
                )
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = color,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PipelineFilters(
    statusFilter: String?,
    searchText: String,
    onStatusChange: (String?) -> Unit,
    onSearchChange: (String) -> Unit,
    totalJobs: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Search
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar por pacote, erro…") },
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, "Limpar", modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
        )

        // Status filter chips
        val chips = listOf(
            null to "Todos",
            "na_fila" to "Fila",
            "concluido" to "OK",
            "falhou" to "Falha",
            "em_quarentena" to "Quarentena",
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(chips) { (status, label) ->
                FilterChip(
                    selected = statusFilter == status,
                    onClick = { onStatusChange(status) },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = Color.White,
                    ),
                )
            }
        }
    }
}

@Composable
private fun JobCard(
    job: IngestionJobEntity,
    vm: PipelineMonitorViewModel,
    onClick: () -> Unit,
) {
    val statusColor = statusColor(job.status)
    val bgColor by animateColorAsState(
        targetValue = if (job.status == "falhou" || job.status in PipelineMonitorViewModel.FAILED_STATES)
            SemanticErrorBg.copy(alpha = 0.3f)
        else if (job.status == "em_quarentena")
            SemanticWarningBg.copy(alpha = 0.3f)
        else Color.Transparent,
        label = "jobBg",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (job.status == "concluido") Primary.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Status dot + label
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(statusColor)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        vm.statusLabel(job.status),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = statusColor,
                    )
                    Text(
                        "ID: ${job.id.take(8)}… | ${job.source_package ?: "manual"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Attempts badge
                if (job.attempt_count > 1) {
                    Badge(containerColor = SemanticWarningBg) {
                        Text("${job.attempt_count}x", style = MaterialTheme.typography.labelSmall, color = SemanticWarning)
                    }
                    Spacer(Modifier.width(6.dp))
                }
                // Timestamp
                Text(
                    vm.formatTimestamp(job.created_at),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }

            // Error message (if failed)
            if (job.error_message != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    job.error_message,
                    style = MaterialTheme.typography.bodySmall,
                    color = SemanticError,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (job.error_code != null) {
                Text(
                    "Código: ${job.error_code}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SemanticError.copy(alpha = 0.7f),
                )
            }

            // Quarantine reason
            if (job.quarantine_reason != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Quarentena: ${job.quarantine_reason}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SemanticWarning,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Action buttons
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (job.status == "falhou" || job.status in PipelineMonitorViewModel.FAILED_STATES) {
                    SmallButton(onClick = { vm.retryJob(job.id) }, color = Primary) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp)); Text("Retentar")
                    }
                    SmallButton(onClick = { vm.quarantineJob(job.id) }, color = SemanticWarning) {
                        Icon(Icons.Default.Shield, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp)); Text("Quarentena")
                    }
                }
                if (job.status == "em_quarentena") {
                    SmallButton(onClick = { vm.retryJob(job.id) }, color = Primary) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp)); Text("Re-tentar")
                    }
                }
                if (job.status !in setOf("concluido", "cancelado", "bloqueado_manualmente")) {
                    SmallButton(onClick = { vm.cancelJob(job.id) }, color = SemanticError) {
                        Icon(Icons.Default.Cancel, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp)); Text("Cancelar")
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallButton(onClick: () -> Unit, color: Color, content: @Composable RowScope.() -> Unit) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        modifier = Modifier.height(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

// ======================== Detail Sheet ========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobDetailSheet(
    job: IngestionJobEntity,
    vm: PipelineMonitorViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor(job.status))
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    vm.statusLabel(job.status),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }

            // Detail fields
            DetailField("Job ID", job.id)
            DetailField("Pacote", job.source_package ?: "—")
            DetailField("URL", job.source_url ?: "—")
            DetailField("Tenant", job.tenant_id)
            DetailField("Tentativas", "${job.attempt_count}/${job.max_attempts}")
            DetailField("Criado em", vm.formatTimestamp(job.created_at))
            DetailField("Atualizado em", vm.formatTimestamp(job.updated_at))
            if (job.completed_at != null) {
                DetailField("Concluído em", vm.formatTimestamp(job.completed_at))
            }
            if (job.node_id != null) {
                DetailField("Knowledge Node", job.node_id)
            }
            if (job.error_code != null) {
                DetailField("Código de erro", job.error_code)
            }
            if (job.error_message != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SemanticErrorBg),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Erro: ${job.error_message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SemanticError,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
            if (job.quarantine_reason != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SemanticWarningBg),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Quarentena: ${job.quarantine_reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SemanticWarning,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            // Actions
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (job.status == "falhou" || job.status in PipelineMonitorViewModel.FAILED_STATES) {
                    Button(onClick = { vm.retryJob(job.id); onDismiss() }) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("Retentar")
                    }
                }
                if (job.status == "em_quarentena") {
                    Button(onClick = { vm.retryJob(job.id); onDismiss() }) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("Liberar da quarentena")
                    }
                }
                if (job.status !in setOf("concluido", "cancelado", "bloqueado_manualmente")) {
                    OutlinedButton(onClick = { vm.cancelJob(job.id); onDismiss() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SemanticError)) {
                        Icon(Icons.Default.Cancel, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("Cancelar")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 220.dp))
    }
}

// ======================== Quarantine Sheet ========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuarantineSheet(
    jobs: List<IngestionJobEntity>,
    vm: PipelineMonitorViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Jobs em Quarentena (${jobs.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )

            if (jobs.isEmpty()) {
                Text("Nenhum job em quarentena.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }

            jobs.forEach { job ->
                Card(
                    border = androidx.compose.foundation.BorderStroke(1.dp, SemanticWarning.copy(alpha = 0.3f)),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Job ${job.id.take(8)}…",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                vm.formatTimestamp(job.created_at),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                            )
                        }
                        if (job.quarantine_reason != null) {
                            Text(
                                job.quarantine_reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = SemanticWarning,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { vm.retryJob(job.id) },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp)); Text("Liberar", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(onClick = { vm.cancelJob(job.id) },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SemanticError)) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp)); Text("Remover", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ======================== Helpers ========================

private fun statusColor(status: String): Color = when (status) {
    "na_fila" -> Accent
    "concluido" -> Primary
    "falhou" -> SemanticError
    "em_quarentena" -> SemanticWarning
    "cancelado" -> TextMuted
    "bloqueado_manualmente" -> TextMuted
    else -> SemanticInfo
}

/** Variável global setada pelo AppContainer para injeção manual no ViewModel. */
lateinit var pipelineMonitorFactory: PipelineMonitorViewModelFactory
