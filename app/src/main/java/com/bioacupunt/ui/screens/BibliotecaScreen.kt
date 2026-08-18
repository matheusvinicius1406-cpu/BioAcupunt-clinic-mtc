package com.bioacupunt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.biblioteca.domain.model.MtcArticle
import com.bioacupunt.biblioteca.domain.model.MtcCategory
import com.bioacupunt.biblioteca.domain.usecase.AskLibraryUseCase
import com.bioacupunt.biblioteca.presentation.HybridResultItem
import com.bioacupunt.biblioteca.presentation.SearchMode
import com.bioacupunt.di.AppContainer
import com.bioacupunt.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BibliotecaScreen(
    onNavigateToFlashcards: () -> Unit = {},
    onNavigateToSimulador: () -> Unit = {},
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

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Text("Biblioteca", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(2.dp))
            Text(
                if (state.articlesUnavailable) {
                    "Conhecimento Ativo · falha ao carregar aprovados da Curadoria (mostrando só os fixos)"
                } else {
                    "Conhecimento Ativo · ${state.allArticles.size} artigos revisados"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (state.articlesUnavailable) statusColors().danger else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AssistChip(
                    onClick = onNavigateToFlashcards,
                    label = { Text("Flashcards", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.Style, null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f),
                )
                AssistChip(
                    onClick = onNavigateToSimulador,
                    label = { Text("Simulador", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.Science, null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ── Ask the library — the one sanctioned AI path, gated by AskLibraryUseCase ──
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .supremeShadow(shape = MaterialTheme.shapes.extraLarge)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, AccentHairline, MaterialTheme.shapes.extraLarge)
                    .padding(horizontal = 18.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Brush.linearGradient(listOf(Primary, Accent))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Psychology, null, tint = Color.White, modifier = Modifier.size(19.dp))
                    }
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

        // ── Search bar ─────────────────────────────────────
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar artigos…") },
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

        // ── Search mode toggle: acervo local (fixo + curadoria) vs busca
        // semântica sobre os nós do pipeline MKIS (FTS5 + sqlite-vec + RRF) ──
        item {
            val mkisActive = state.searchMode == SearchMode.MKIS_HYBRID
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, if (mkisActive) AccentHairline else MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Default.Hub,
                    null,
                    tint = if (mkisActive) Primary else TextMuted,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "Busca semântica avançada (MKIS)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (mkisActive) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = mkisActive,
                    onCheckedChange = { vm.toggleSearchMode() },
                )
            }
        }

        if (state.searchMode == SearchMode.MKIS_HYBRID) {
            item {
                Text(
                    "Busca sobre os nós de conhecimento ingeridos pelo pipeline (Curadoria › Pipeline). " +
                        "Complementa o acervo abaixo — não substitui o gate de evidência da IA (R2).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                state.query.isBlank() -> item {
                    Text(
                        "Digite algo na busca acima para consultar a base MKIS.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 20.dp),
                    )
                }
                state.hybridSearchFailed -> item {
                    Text(
                        "Falha ao consultar a base MKIS. Tente novamente.",
                        color = statusColors().danger,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 20.dp),
                    )
                }
                state.hybridResults.isEmpty() && !state.isSearching -> item {
                    Text(
                        "Nenhum nó de conhecimento encontrado para esta busca.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 20.dp),
                    )
                }
                else -> items(state.hybridResults, key = { it.id }) { result ->
                    HybridResultCard(result, onOpen = { vm.onHybridResultClick(result) })
                }
            }
        } else {

        // ── Stats row ──────────────────────────────────────
        item {
            // Sempre computado sobre state.allArticles (fixos + aprovados via Curadoria,
            // já reativo no ViewModel) — nunca sobre MtcKnowledgeBase.articles sozinho,
            // que só enxerga os 16 fixos e nunca mudava quando a médica aprovava algo novo.
            val favCount = state.favoriteIds.size
            val categoryCount = state.allArticles.map { it.category }.distinct().size
            val totalTags = state.allArticles.flatMap { it.tags }.distinct().size
            // Quando a query de artigos aprovados falhou, allArticles está degradado (só os
            // 16 fixos) — mostrar "—" em vez de um número plausível porém incompleto, para
            // não passar a impressão de "acervo completo" quando na verdade é "carregamento
            // parcial silencioso".
            val degraded = state.articlesUnavailable
            val stats = listOf(
                (if (degraded) "—" else "${state.allArticles.size}") to "Artigos",
                (if (degraded) "—" else "$categoryCount") to "Categorias",
                "$favCount" to "Favoritos",
                (if (degraded) "—" else "$totalTags") to "Tags",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                stats.forEach { (value, label) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                            .padding(vertical = 12.dp, horizontal = 4.dp),
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
            Spacer(Modifier.height(10.dp))
            MtcCategory.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    row.forEach { cat ->
                        val count = state.allArticles.count { it.category == cat.name }
                        val selected = state.category == cat.name
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.large)
                                .background(if (selected) Primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface)
                                .border(1.dp, if (selected) Primary else MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                                .clickable { vm.onCategorySelected(if (selected) null else cat.name) }
                                .padding(vertical = 16.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(cat.icon(), null, tint = if (selected) Primary else TextMuted, modifier = Modifier.size(20.dp))
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
                Text("ARTIGOS", style = MaterialTheme.typography.labelMedium, color = TextMuted, modifier = Modifier.weight(1f))
                Text("${state.articles.size}", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            }
        }

        // ── Articles list ────────────────────────────────
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

    // ── Article detail sheet ──────────────────────────────
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

    // ── Search result detail sheet ────────────────────────
    state.selectedResultItem?.let { item ->
        SearchResultDetailSheet(
            item = item,
            onDismiss = vm::clearSelectedNode,
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
private fun HybridResultCard(result: HybridResultItem, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .supremeShadow(shape = MaterialTheme.shapes.large, elevation = Elevation.Card)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
            .clickable(onClick = onOpen)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(30.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Hub, null, tint = Primary, modifier = Modifier.size(16.dp))
            }
            Text(result.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            val pct = (result.score * 100).toInt().coerceIn(0, 99)
            Box(modifier = Modifier.clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text("$pct%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Primary)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(result.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ArticleCard(article: MtcArticle, isFavorite: Boolean, onOpen: () -> Unit, onToggleFavorite: () -> Unit) {
    val category = runCatching { MtcCategory.valueOf(article.category) }.getOrNull()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .supremeShadow(shape = MaterialTheme.shapes.large, elevation = Elevation.Card)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
            .clickable(onClick = onOpen)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(32.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(category?.icon() ?: Icons.AutoMirrored.Filled.Article, null, tint = Primary, modifier = Modifier.size(18.dp))
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultDetailSheet(
    item: com.bioacupunt.biblioteca.presentation.HybridResultItem,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            if (item.summary.isNotBlank()) {
                Text(item.summary, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "Score: ${"%.2f".format(item.score)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

