package com.bioacupunt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.bioacupunt.biblioteca.domain.model.MtcArticle
import com.bioacupunt.biblioteca.domain.model.MtcCategory
import com.bioacupunt.biblioteca.domain.usecase.AskLibraryUseCase
import com.bioacupunt.di.AppContainer
import com.bioacupunt.biblioteca.presentation.HybridResultItem
import com.bioacupunt.biblioteca.presentation.SearchMode
import com.bioacupunt.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BibliotecaScreen(
    onNavigateToFlashcards: () -> Unit = {},
    onNavigateToSimulador: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {}
) {
    val vm = viewModel<com.bioacupunt.biblioteca.presentation.BibliotecaViewModel>(factory = AppContainer.bibliotecaViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()
    // Guardamos só o id, não o MtcArticle inteiro: navegar para um "relacionado" troca
    // o id, e o artigo atual é sempre resolvido contra state.allArticles (o universo
    // completo, não filtrado por busca/categoria) — ver comentário em BibliotecaUiState.
    var openArticleId by remember { mutableStateOf<String?>(null) }
    val openArticle = remember(openArticleId, state.allArticles) {
        openArticleId?.let { id -> state.allArticles.find { it.id == id } }
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Biblioteca", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold))
            Text(
                "Conhecimento Ativo · ${MtcKnowledgeCount.totalArticles} artigos revisados",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(onClick = onNavigateToFlashcards, label = { Text("🃏 Flashcards", style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.weight(1f))
                AssistChip(onClick = onNavigateToSimulador, label = { Text("🧪 Simulador", style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.weight(1f))
                AssistChip(onClick = onNavigateToAnalytics, label = { Text("📊 Analytics", style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.weight(1f))
            }
        }

        // ── Ask the library — the one sanctioned AI path, gated by AskLibraryUseCase ──
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.dp, Primary, MaterialTheme.shapes.extraLarge)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Psychology, null, tint = Primary, modifier = Modifier.size(20.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = state.askQuestion,
                        onValueChange = vm::onAskQuestionChanged,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { vm.ask() }),
                        decorationBox = { inner ->
                            if (state.askQuestion.isBlank()) Text("Busca semântica · pergunte à IA...", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                            inner()
                        },
                    )
                    if (state.asking) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = vm::ask) { Icon(Icons.Default.Search, null, tint = Primary) }
                    }
                }
                state.askAnswer?.let { answer ->
                    HorizontalDivider()
                    Box(modifier = Modifier.padding(vertical = 10.dp)) {
                        AskAnswerView(answer, onDismiss = vm::clearAnswer)
                    }
                }
            }
        }

        // ── Search mode toggle ────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Buscar em:", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                FilterChip(
                    selected = state.searchMode == SearchMode.LEGACY,
                    onClick = { if (state.searchMode != SearchMode.LEGACY) vm.toggleSearchMode() },
                    label = { Text("Acervo fixo", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.MenuBook, null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = Color.White,
                    ),
                )
                FilterChip(
                    selected = state.searchMode == SearchMode.MKIS_HYBRID,
                    onClick = { if (state.searchMode != SearchMode.MKIS_HYBRID) vm.toggleSearchMode() },
                    label = { Text("MKIS Híbrido", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.TravelExplore, null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = Color.White,
                    ),
                )
            }
        }

        // ── Hybrid search field (only in MKIS_HYBRID mode) ──
        if (state.searchMode == SearchMode.MKIS_HYBRID) {
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = vm::onQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar nos nós do MKIS…") },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (state.isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else if (state.query.isNotEmpty()) {
                            IconButton(onClick = { vm.onQueryChanged("") }) {
                                Icon(Icons.Default.Close, "Limpar", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // ── Stats row ──────────────────────────────────────
        item {
            val favCount = state.favoriteIds.size
            val categoryCount = MtcKnowledgeBaseCategories.usedCategories.size
            val stats = listOf(
                "${MtcKnowledgeCount.totalArticles}" to "Artigos",
                "$categoryCount" to "Categorias",
                "$favCount" to "Favoritos",
                "${MtcKnowledgeCount.totalTags}" to "Tags",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                stats.forEach { (value, label) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Primary)
                        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }
            }
        }

        item {
            Text("ACERVO", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Spacer(Modifier.height(8.dp))
            MtcCategory.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    row.forEach { cat ->
                        val count = MtcKnowledgeBaseCategories.countFor(cat)
                        val selected = state.category == cat.name
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.large)
                                .background(if (selected) Primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface)
                                .border(1.dp, if (selected) Primary else MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                                .clickable { vm.onCategorySelected(if (selected) null else cat.name) }
                                .padding(vertical = 14.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(cat.emoji, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(cat.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2, color = MaterialTheme.colorScheme.onSurface)
                            Text("$count artigo${if (count == 1) "" else "s"}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.weight(1f).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.Bookmark, null, tint = Primary, modifier = Modifier.size(18.dp))
                    Text("${state.favoriteIds.size} favoritos", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = Primary)
                }
                Row(
                    modifier = Modifier.weight(1f).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.DownloadForOffline, null, tint = Primary, modifier = Modifier.size(18.dp))
                    Text("Todo o conteúdo já é offline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (state.searchMode == SearchMode.MKIS_HYBRID) {
                    Text("RESULTADOS MKIS", style = MaterialTheme.typography.labelMedium, color = Primary, modifier = Modifier.weight(1f))
                    Text("${state.hybridResults.size}", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                } else {
                    Text("ARTIGOS", style = MaterialTheme.typography.labelMedium, color = TextMuted, modifier = Modifier.weight(1f))
                    Text("${state.articles.size}", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                }
            }
        }

        if (state.searchMode == SearchMode.MKIS_HYBRID) {
            // ── Hybrid results list ────────────────────────
            if (state.query.isBlank()) {
                item {
                    Text("Digite uma busca acima para pesquisar nos nós do MKIS.", color = TextMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 20.dp))
                }
            } else if (state.hybridResults.isEmpty() && !state.isSearching) {
                item {
                    Text("Nenhum resultado encontrado no MKIS para sua busca.", color = TextMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 20.dp))
                }
            } else {
                items(state.hybridResults, key = { it.id }) { item ->
                    HybridResultCard(item, vm::onHybridResultClick)
                }
            }
        } else {
            // ── Legacy articles list ───────────────────────
            if (state.articles.isEmpty()) {
                item {
                    Text("Nenhum resultado para sua busca", color = TextMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 20.dp))
                }
            } else {
                items(state.articles, key = { it.id }) { article ->
                    ArticleCard(
                        article = article,
                        isFavorite = article.id in state.favoriteIds,
                        onOpen = { openArticleId = article.id },
                        onToggleFavorite = { vm.toggleFavorite(article.id) },
                    )
                }
            }
        }
    }

    // ── Legacy article detail sheet ────────────────────────
    openArticle?.let { article ->
        val related = remember(article.id, state.allArticles) {
            state.allArticles.filter { it.category == article.category && it.id != article.id }
        }
        ArticleDetailSheet(
            article = article,
            relatedArticles = related,
            onOpenRelated = { openArticleId = it.id },
            onDismiss = { openArticleId = null },
        )
    }

    // ── MKIS node detail sheet ────────────────────────────
    state.selectedMkisNode?.let { node ->
        MkisDetailSheet(
            node = node,
            score = state.selectedMkisNodeScore,
            onDismiss = { vm.clearSelectedNode() },
        )
    }
}

@Composable
private fun AskAnswerView(answer: AskLibraryUseCase.Answer, onDismiss: () -> Unit) {
    when (answer) {
        is AskLibraryUseCase.Answer.Grounded -> Column {
            Text(answer.text, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Text(
                "Fontes: " + answer.sources.joinToString(", ") { "[${it.ordinal}] ${it.articleTitle}" },
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
        AskLibraryUseCase.Answer.NoEvidence -> Text(
            "A biblioteca não tem evidência sobre isso. Nenhuma resposta foi gerada.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )
        is AskLibraryUseCase.Answer.Failed -> Text(answer.message, style = MaterialTheme.typography.bodySmall, color = SemanticError)
    }
    TextButton(onClick = onDismiss, contentPadding = PaddingValues(0.dp)) { Text("Fechar", style = MaterialTheme.typography.labelSmall) }
}

@Composable
private fun ArticleCard(article: MtcArticle, isFavorite: Boolean, onOpen: () -> Unit, onToggleFavorite: () -> Unit) {
    val category = runCatching { MtcCategory.valueOf(article.category) }.getOrNull()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
            .clickable(onClick = onOpen)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(category?.emoji ?: "📄", style = MaterialTheme.typography.titleMedium)
            Text(article.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                Icon(
                    if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    null,
                    tint = if (isFavorite) Accent else TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(article.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
        if (article.tags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FlowRowCompat(article.tags.take(4))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowCompat(tags: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        tags.forEach { tag ->
            Box(modifier = Modifier.clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 10.dp, vertical = 2.dp)) {
                Text(tag, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Primary)
            }
        }
    }
}

@Composable
private fun HybridResultCard(item: HybridResultItem, onClick: (HybridResultItem) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Primary.copy(alpha = 0.3f), MaterialTheme.shapes.large)
            .clickable { onClick(item) }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.TravelExplore, null, tint = Primary, modifier = Modifier.size(16.dp))
            }
            Text(item.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            // Score badge
            val scorePct = (item.score * 100).toInt().coerceIn(0, 99)
            val scoreColor = when {
                scorePct >= 70 -> SemanticSuccess
                scorePct >= 40 -> Accent
                else -> TextMuted
            }
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(scoreColor.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text("$scorePct%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = scoreColor)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(item.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TripOrigin, null, tint = Primary.copy(alpha = 0.5f), modifier = Modifier.size(10.dp))
            Spacer(Modifier.width(4.dp))
            Text("MKIS · busca híbrida", style = MaterialTheme.typography.labelSmall, color = Primary.copy(alpha = 0.7f))
        }
    }
}

/** Small real-data helpers over the 16 reviewed articles — no fabricated numbers. */
private object MtcKnowledgeCount {
    val totalArticles: Int get() = com.bioacupunt.biblioteca.data.MtcKnowledgeBase.articles.size
    val totalTags: Int get() = com.bioacupunt.biblioteca.data.MtcKnowledgeBase.articles.flatMap { it.tags }.distinct().size
}

private object MtcKnowledgeBaseCategories {
    val usedCategories: Set<String> get() = com.bioacupunt.biblioteca.data.MtcKnowledgeBase.articles.map { it.category }.toSet()
    fun countFor(category: MtcCategory): Int = com.bioacupunt.biblioteca.data.MtcKnowledgeBase.articles.count { it.category == category.name }
}
