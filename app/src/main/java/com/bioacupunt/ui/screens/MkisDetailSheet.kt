package com.bioacupunt.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bioacupunt.data.local.model.KnowledgeNodeEntity
import com.bioacupunt.ui.theme.*

/**
 * Página de detalhe de um nó do MKIS — full screen via bottom sheet expandido.
 *
 * Mostra todos os metadados disponíveis do [KnowledgeNodeEntity]:
 * - Tipo de conhecimento, evidência, viés
 * - Fonte, citação, DOI, PMID, autores
 * - Scores científicos/de IA/confiabilidade
 * - Tags, conteúdo completo
 * - Timestamps e governança
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MkisDetailSheet(
    node: KnowledgeNodeEntity,
    score: Double = 0.0,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            // ── Status bar: tipo + badge de score ──────────
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(KnowledgeTypeColor(node.knowledge_type).copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        "${KnowledgeTypeEmoji(node.knowledge_type)} ${KnowledgeTypeLabel(node.knowledge_type)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = KnowledgeTypeColor(node.knowledge_type),
                    )
                }
                if (score > 0.0) {
                    val pct = (score * 100).toInt().coerceIn(0, 99)
                    val sc = when { pct >= 70 -> SemanticSuccess; pct >= 40 -> Accent; else -> TextMuted }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(sc.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text("$pct%", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = sc)
                    }
                }
                Spacer(Modifier.weight(1f))
                StatusBadge(node.status)
            }
            Spacer(Modifier.height(12.dp))

            // ── Título ──
            Text(node.title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(4.dp))
            if (node.summary.isNotBlank()) {
                Text(node.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // ── Metadados ──
            Spacer(Modifier.height(16.dp))
            MetadataSection(node)

            if (node.content.isNotBlank()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                // ── Conteúdo ──
                Text(node.content, style = MaterialTheme.typography.bodyMedium)
            }

            // ── Tags ──
            val tags = node.tags.split(" ").filter { it.isNotBlank() }
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("TAGS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraLarge)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(tag, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Primary)
                        }
                    }
                }
            }

            // ── Scores ──
            if (node.scientific_score != null || node.ai_score != null || node.reliability_score != null) {
                Spacer(Modifier.height(20.dp))
                Text("SCORES", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    node.scientific_score?.let { ScoreBadge("Científico", it, Primary) }
                    node.ai_score?.let { ScoreBadge("IA", it, Accent) }
                    node.reliability_score?.let { ScoreBadge("Confiabilidade", it, SemanticInfo) }
                }
            }

            // ── Fonte / citação ──
            if (node.source_url != null || node.citation.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                Text("FONTE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(Modifier.height(6.dp))
                if (node.citation.isNotBlank()) {
                    Text(node.citation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (node.source_url != null) {
                    val context = LocalContext.current
                    TextButton(
                        onClick = {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(node.source_url))) }
                        },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                    ) {
                        Icon(Icons.Default.OpenInNew, null, tint = Primary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Abrir fonte", style = MaterialTheme.typography.labelSmall, color = Primary)
                    }
                }
            }
        }
    }
}

// ======================== Sub-components ========================

@Composable
private fun MetadataSection(node: KnowledgeNodeEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MetadataRow("Tipo", "${KnowledgeTypeLabel(node.knowledge_type)} (${node.knowledge_type})")
            MetadataRow("Evidência", node.evidence_level ?: "Não avaliado")
            MetadataRow("Risco de viés", BiasRiskLabel(node.bias_risk))
            MetadataRow("Fonte", SourceLabel(node.source))
            MetadataRow("Idioma", node.language.uppercase())
            MetadataRow("Especialidade", SpecialtyLabel(node.specialty))
            MetadataRow("Categoria clínica", node.category.uppercase())
            MetadataRow("Versão", node.version)
            if (node.doi != null) MetadataRow("DOI", node.doi)
            if (node.pmid != null) MetadataRow("PMID", node.pmid)
            MetadataRow("Criado em", formatTimestamp(node.created_at))
            if (node.approved_at != null) MetadataRow("Aprovado em", formatTimestamp(node.approved_at))
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 200.dp))
    }
}

@Composable
private fun ScoreBadge(label: String, score: Double, color: Color) {
    val pct = (score * 100).toInt().coerceIn(0, 100)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("$pct%", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (color, label) = when (status) {
        "aprovado" -> SemanticSuccess to "APROVADO"
        "rascunho" -> Accent to "RASCUNHO"
        "rejeitado" -> SemanticError to "REJEITADO"
        "descontinuado" -> TextMuted to "DESCONTINUADO"
        "em_revisao" -> SemanticInfo to "EM REVISÃO"
        "aguardando_aprovacao" -> SemanticWarning to "AGUARDANDO"
        else -> TextMuted to status.uppercase()
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = color)
    }
}

// ======================== Helpers ========================

private fun KnowledgeTypeLabel(type: String): String = when (type) {
    "artigo" -> "Artigo"
    "revisao" -> "Revisão"
    "ensaio_clinico" -> "Ensaio Clínico"
    "guideline" -> "Diretriz"
    "capitulo" -> "Capítulo"
    "livro" -> "Livro"
    "tese" -> "Tese"
    "caso_clinico" -> "Caso Clínico"
    "protocolo" -> "Protocolo"
    "nota" -> "Nota Técnica"
    "relatorio" -> "Relatório"
    "educacional" -> "Educacional"
    else -> type.replace("_", " ").replaceFirstChar { it.uppercase() }
}

private fun KnowledgeTypeEmoji(type: String): String = when (type) {
    "artigo" -> "📄"
    "revisao" -> "📚"
    "ensaio_clinico" -> "🔬"
    "guideline" -> "📋"
    "capitulo" -> "📖"
    "livro" -> "📕"
    "tese" -> "🎓"
    "caso_clinico" -> "🩺"
    "protocolo" -> "📝"
    "nota" -> "📌"
    "relatorio" -> "📊"
    "educacional" -> "🎯"
    else -> "📄"
}

private fun KnowledgeTypeColor(type: String): Color = when (type) {
    "artigo" -> Primary
    "revisao" -> Color(0xFF7B1FA2) // roxo
    "ensaio_clinico" -> Color(0xFF1565C0) // azul
    "guideline" -> Color(0xFF2E7D32) // verde
    "capitulo" -> Color(0xFFE65100) // laranja
    "livro" -> Color(0xFF4E342E) // marrom
    "tese" -> Color(0xFF6A1B9A) // roxo escuro
    "caso_clinico" -> Color(0xFF00838F) // ciano
    "protocolo" -> Color(0xFF37474F) // azul cinza
    "nota" -> Color(0xFFF57F17) // amarelo escuro
    "relatorio" -> Color(0xFF004D40) // verde escuro
    "educacional" -> Color(0xFFF06292) // rosa
    else -> Primary
}

private fun BiasRiskLabel(risk: String): String = when (risk) {
    "baixo" -> "Baixo"
    "moderado" -> "Moderado"
    "alto" -> "Alto"
    "nao_avaliado" -> "Não avaliado"
    else -> risk
}

private fun SourceLabel(source: String): String = when (source) {
    "pubmed" -> "PubMed"
    "europe_pmc" -> "Europe PMC"
    "semantic_scholar" -> "Semantic Scholar"
    "crossref" -> "CrossRef"
    "openalex" -> "OpenAlex"
    "scielo" -> "SciELO"
    "bvs" -> "BVS/LILACS"
    "who_iris" -> "WHO IRIS"
    "paho_iris" -> "PAHO IRIS"
    "doaj" -> "DOAJ"
    "clinical_trials" -> "ClinicalTrials.gov"
    "manual" -> "Inserção manual"
    "pacote" -> "Pacote curado"
    else -> source.replace("_", " ").replaceFirstChar { it.uppercase() }
}

private fun SpecialtyLabel(specialty: String): String = when (specialty) {
    "acupuntura" -> "Acupuntura"
    "auriculoterapia" -> "Auriculoterapia"
    "fitoterapia" -> "Fitoterapia"
    "moxabustao" -> "Moxabustão"
    "ventosaterapia" -> "Ventosaterapia"
    "tui_na" -> "Tui Ná"
    "eletroacupuntura" -> "Eletroacupuntura"
    "craniopuntura" -> "Craniopuntura"
    "geral" -> "Geral"
    else -> specialty.replace("_", " ").replaceFirstChar { it.uppercase() }
}

private val DATE_FMT = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR"))

private fun formatTimestamp(millis: Long): String = DATE_FMT.format(java.util.Date(millis))
