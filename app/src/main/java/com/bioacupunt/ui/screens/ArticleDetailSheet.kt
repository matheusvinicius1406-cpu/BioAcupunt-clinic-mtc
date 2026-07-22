package com.bioacupunt.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bioacupunt.biblioteca.domain.ingestion.Provenance
import com.bioacupunt.biblioteca.domain.model.MtcArticle
import com.bioacupunt.biblioteca.domain.model.MtcCategory
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.TextMuted

/**
 * Página de detalhe de um artigo da Biblioteca — full screen via bottom sheet expandido
 * (não um [AlertDialog] pequeno). Mostra tudo que a curadoria já carrega mas que se
 * perdia ao sair da fila de revisão: citação, fonte, localizador e proveniência
 * (ver `MtcArticle.citation/sourceUrl/sourceRef/provenance` e
 * `LibraryStagingRepository.toArticle`).
 *
 * Artigos do acervo interno fixo (sem proveniência de curadoria) continuam abrindo
 * normalmente — a seção de proveniência é simplesmente omitida, nunca um "N/A" feio
 * nem um crash (mesmo espírito de "codec degrada, não quebra").
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ArticleDetailSheet(
    article: MtcArticle,
    relatedArticles: List<MtcArticle>,
    onOpenRelated: (MtcArticle) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val category = remember(article.category) { runCatching { MtcCategory.valueOf(article.category) }.getOrNull() }

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
            // ── Categoria ──
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Primary.copy(alpha = 0.10f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        "${category?.emoji ?: "📄"}  ${category?.label ?: article.category}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Primary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // ── Título ──
            Text(article.title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(4.dp))
            if (article.summary.isNotBlank()) {
                Text(article.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // ── Proveniência (só quando existir) ──
            val hasProvenance = article.citation.isNotBlank() || article.sourceUrl.isNotBlank() || article.sourceRef.isNotBlank()
            val provenance = remember(article.provenance) { runCatching { Provenance.valueOf(article.provenance) }.getOrNull() }
            Spacer(Modifier.height(14.dp))
            if (hasProvenance) {
                ProvenanceSection(article, provenance)
            } else {
                Text(
                    "Fonte: acervo interno do app",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // ── Conteúdo completo, sem limite de altura ──
            Text(article.content, style = MaterialTheme.typography.bodyMedium)

            // ── Tags ──
            if (article.tags.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("TAGS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    article.tags.forEach { tag ->
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

            // ── Artigos relacionados ──
            if (relatedArticles.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text("ARTIGOS RELACIONADOS", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    relatedArticles.forEach { related ->
                        RelatedArticleRow(related, onClick = { onOpenRelated(related) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ProvenanceSection(article: MtcArticle, provenance: Provenance?) {
    Column {
        if (provenance != null) {
            ProvenanceBadge(provenance)
            Spacer(Modifier.height(8.dp))
        }
        if (article.citation.isNotBlank()) {
            Text("Citação: ${article.citation}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (article.sourceRef.isNotBlank()) {
            Text("Localizador: ${article.sourceRef}", style = MaterialTheme.typography.labelSmall, color = Primary)
        }
        if (article.sourceUrl.isNotBlank()) {
            val context = LocalContext.current
            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.sourceUrl)))
                    }
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

/** Mesmo padrão visual do badge de proveniência de CuradoriaScreen.kt. */
@Composable
private fun ProvenanceBadge(provenance: Provenance) {
    val (color, label) = when (provenance) {
        Provenance.VERIFICAVEL -> Pair(Color(0xFF2E7D32) /* verde escuro */, "VERIFICÁVEL")
        Provenance.RASCUNHO -> Pair(Color(0xFFE65100) /* laranja escuro */, "RASCUNHO")
    }
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color,
        )
    }
}

@Composable
private fun RelatedArticleRow(article: MtcArticle, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(article.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (article.summary.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(article.summary, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(Icons.Default.OpenInNew, null, tint = TextMuted, modifier = Modifier.size(16.dp))
    }
}
