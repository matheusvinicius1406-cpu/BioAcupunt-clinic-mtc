package com.bioacupunt.copilot.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.copilot.rag.EvidenceGate
import com.bioacupunt.copilot.rag.ResponseValidator
import com.bioacupunt.copilot.retrieval.IntentType
import com.bioacupunt.di.AppContainer
import com.bioacupunt.ui.theme.Accent
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.SemanticError
import com.bioacupunt.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopilotScreen(
    patientId: Long? = null,
    patientName: String? = null,
    onBack: (() -> Unit)? = null,
) {
    val vm = viewModel<CopilotViewModel>(
        key = "copilot-${patientId ?: 0}",
        factory = CopilotViewModelFactory(
            engine = AppContainer.clinicalCopilotEngine,
            evidenceExplorer = AppContainer.evidenceExplorer,
            patientContextProvider = AppContainer.copilotPatientContextProvider,
            patientId = patientId,
            patientName = patientName,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Header ─────────────────────────────────────────
        CopilotHeader(onBack = onBack)

        // ── Context Indicator ──────────────────────────────
        CopilotContextIndicator(state = state)

        // ── Validation Warning ─────────────────────────────
        AnimatedVisibility(
            visible = state.uiState == CopilotUiState.PARTIAL_RESULT,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ValidationWarningBar(report = state.lastValidationReport)
        }

        HorizontalDivider()

        // ── Messages ───────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.messages, key = { it.id }) { message ->
                CopilotMessageBubble(
                    message = state.messages.indexOf(message).let { idx ->
                        state.messages[idx]
                    },
                    onToggleEvidence = { vm.toggleEvidenceExplorer() },
                    onToggleDifferential = { vm.toggleDifferentialExplanation() },
                    onToggleMissingData = { vm.toggleMissingData() },
                )
            }

            if (state.thinking) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Text("processando...", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }
            }

            // ── Evidence Explorer Panel ────────────────────
            if (state.showEvidenceExplorer && state.lastResponse != null) {
                item {
                    EvidenceExplorerPanel(response = state.lastResponse!!)
                }
            }

            // ── Differential Explanation Panel ─────────────
            if (state.showDifferentialExplanation && state.lastResponse != null) {
                item {
                    DifferentialExplanationPanel(response = state.lastResponse!!)
                }
            }

            // ── Missing Data Panel ─────────────────────────
            if (state.showMissingData && state.lastResponse != null) {
                item {
                    MissingDataPanel(response = state.lastResponse!!)
                }
            }
        }

        HorizontalDivider()

        // ── Input ──────────────────────────────────────────
        CopilotInput(
            value = state.input,
            onValueChange = vm::onInputChanged,
            onSend = { vm.send() },
            enabled = !state.thinking,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// HEADER
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun CopilotHeader(onBack: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = TextMuted)
            }
            Spacer(Modifier.width(4.dp))
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(Primary, Accent))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Psychology, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "Copiloto Clínico",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f),
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// CONTEXT INDICATOR
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun CopilotContextIndicator(state: CopilotUiStateData) {
    AnimatedVisibility(
        visible = state.mode != CopilotMode.GENERAL || state.patientName != null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val icon = when (state.mode) {
                CopilotMode.PATIENT -> Icons.Default.Person
                CopilotMode.KNOWLEDGE -> Icons.Default.LibraryBooks
                CopilotMode.DIFFERENTIAL -> Icons.Default.CompareArrows
                CopilotMode.GENERAL -> Icons.Default.Chat
            }
            Icon(icon, null, tint = Primary, modifier = Modifier.size(14.dp))

            val label = when (state.mode) {
                CopilotMode.PATIENT -> "Paciente: ${state.patientName ?: "ativo"}"
                CopilotMode.KNOWLEDGE -> "Modo: Conhecimento"
                CopilotMode.DIFFERENTIAL -> "Modo: Diferencial"
                CopilotMode.GENERAL -> "Modo: Geral"
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Primary,
                modifier = Modifier.weight(1f),
            )

            if (state.lastResponse?.confidence != null) {
                ConfidenceBadge(confidence = state.lastResponse!!.confidence)
            }
        }
    }
}

@Composable
private fun ConfidenceBadge(confidence: String) {
    val (color, label) = when (confidence) {
        "HIGH" -> Pair(MaterialTheme.colorScheme.primary, "ALTA")
        "MODERATE" -> Pair(MaterialTheme.colorScheme.tertiary, "MODERADA")
        "LOW" -> Pair(MaterialTheme.colorScheme.error, "BAIXA")
        else -> Pair(TextMuted, confidence)
    }
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

// ═══════════════════════════════════════════════════════════════════
// VALIDATION WARNING
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun ValidationWarningBar(report: ResponseValidator.ValidationReport?) {
    if (report == null) return
    val warnings = report.issues.filter {
        it.severity == ResponseValidator.Severity.WARNING || it.severity == ResponseValidator.Severity.ERROR
    }
    if (warnings.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Default.Warning, null, tint = SemanticError, modifier = Modifier.size(14.dp))
        Text(
            warnings.first().description,
            style = MaterialTheme.typography.labelSmall,
            color = SemanticError,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// MESSAGE BUBBLE
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun CopilotMessageBubble(
    message: CopilotMessage,
    onToggleEvidence: () -> Unit,
    onToggleDifferential: () -> Unit,
    onToggleMissingData: () -> Unit,
) {
    val isUser = message.role == CopilotRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(0.92f),
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isUser) 16.dp else 4.dp,
                            topEnd = if (isUser) 4.dp else 16.dp,
                            bottomStart = 16.dp, bottomEnd = 16.dp,
                        )
                    )
                    .background(if (isUser) Primary else MaterialTheme.colorScheme.background)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                if (isUser) {
                    Text(message.text, style = MaterialTheme.typography.bodySmall, color = Color.White)
                } else {
                    // Render markdown for assistant messages
                    com.bioacupunt.ui.screens.MarkdownText(message.text)
                }
            }

            // ── Action chips for assistant messages ────────
            if (!isUser && message.gateResult != null) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Intent badge
                    IntentBadge(intent = message.intent)

                    // Latency
                    if (message.latencyMs > 0) {
                        Text(
                            "${message.latencyMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                        )
                    }
                }

                // Action buttons
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (message.gateResult?.decision == EvidenceGate.GateDecision.ALLOW) {
                        ActionChip(
                            icon = Icons.Default.Visibility,
                            label = "Evidências",
                            onClick = onToggleEvidence,
                        )
                    }
                    if (message.validationReport?.result == ResponseValidator.ValidationResult.HAS_UNSUPPORTED_CLAIMS) {
                        ActionChip(
                            icon = Icons.Default.Warning,
                            label = "Avisos",
                            onClick = {},
                            color = SemanticError,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IntentBadge(intent: IntentType?) {
    if (intent == null) return
    val label = when (intent) {
        IntentType.KNOWLEDGE_SEARCH -> "BUSCA"
        IntentType.PATIENT_SUMMARY -> "RESUMO"
        IntentType.DIFFERENTIAL_EXPLANATION -> "DIFERENCIAL"
        IntentType.MISSING_DATA -> "DADOS"
        IntentType.EVIDENCE_LOOKUP -> "EVIDÊNCIA"
        IntentType.POINT_LOOKUP -> "PONTO"
        IntentType.FORMULA_LOOKUP -> "FÓRMULA"
        IntentType.PROTOCOL_LOOKUP -> "PROTOCOLO"
        IntentType.CLINICAL_ANALYSIS -> "ANÁLISE"
        IntentType.RESEARCH_QUERY -> "PESQUISA"
        IntentType.GENERAL_CLINICAL_QUERY -> "GERAL"
    }
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    color: androidx.compose.ui.graphics.Color = Primary,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = 0.1f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// EVIDENCE EXPLORER PANEL
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun EvidenceExplorerPanel(
    response: com.bioacupunt.copilot.rag.GroundedResponseGenerator.GroundedResponse,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Source, null, tint = Primary, modifier = Modifier.size(16.dp))
                Text("Evidências", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            }
            Spacer(Modifier.height(8.dp))

            if (response.evidenceIds.isEmpty() && response.citations.isEmpty()) {
                Text(
                    "Nenhuma evidência registrada para esta resposta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            } else {
                // Citations
                response.citations.forEachIndexed { idx, citation ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "[${idx + 1}]",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                        )
                        Text(citation, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Evidence IDs
                if (response.evidenceIds.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "IDs de evidência: ${response.evidenceIds.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }

                // Knowledge version
                if (response.knowledgeVersion != null) {
                    Text(
                        "Versão: ${response.knowledgeVersion}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// DIFFERENTIAL EXPLANATION PANEL
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun DifferentialExplanationPanel(
    response: com.bioacupunt.copilot.rag.GroundedResponseGenerator.GroundedResponse,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.CompareArrows, null, tint = Primary, modifier = Modifier.size(16.dp))
                Text("Explicação do Diferencial", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            }
            Spacer(Modifier.height(8.dp))

            // Claims
            if (response.claims.isNotEmpty()) {
                Text("Afirmações:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                response.claims.forEach { claim ->
                    Text("• $claim", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Uncertainties
            if (response.uncertainties.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("Incertezas:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = SemanticError)
                response.uncertainties.forEach { uncertainty ->
                    Text("• $uncertainty", style = MaterialTheme.typography.bodySmall, color = SemanticError)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// MISSING DATA PANEL
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun MissingDataPanel(
    response: com.bioacupunt.copilot.rag.GroundedResponseGenerator.GroundedResponse,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.HelpOutline, null, tint = Primary, modifier = Modifier.size(16.dp))
                Text("Dados Faltantes", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            }
            Spacer(Modifier.height(8.dp))

            if (response.warnings.contains("NO_CLINICAL_INTELLIGENCE")) {
                Text(
                    "Execute a análise clínica primeiro para ver dados faltantes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            } else {
                Text(response.answer, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// INPUT
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun CopilotInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Pergunte ao copiloto...") },
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.extraLarge,
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
        )
        IconButton(
            onClick = onSend,
            enabled = value.isNotBlank() && enabled,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Primary),
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White)
        }
    }
}
