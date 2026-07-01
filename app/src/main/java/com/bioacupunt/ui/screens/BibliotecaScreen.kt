package com.bioacupunt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.biblioteca.domain.model.BibliotecaNode
import com.bioacupunt.di.AppContainer
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibliotecaScreen(
    onNavigateToFlashcards: () -> Unit = {},
    onNavigateToSimulador: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {}
) {
    val vm = viewModel(factory = AppContainer.bibliotecaViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(vm) {
        vm.state.collectLatest { /* ensure loading/routing */ }
    }

    val filtered = remember(state.results, selectedCategory) {
        val base = state.results
        if (selectedCategory == null) base else base.filter { it.type == selectedCategory }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = onNavigateToFlashcards, label = { Text("🃏 Flashcards", style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.weight(1f))
            AssistChip(onClick = onNavigateToSimulador, label = { Text("🧪 Simulador", style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.weight(1f))
            AssistChip(onClick = onNavigateToAnalytics, label = { Text("📊 Analytics", style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.weight(1f))
        }

        OutlinedTextField(
            value = state.query,
            onValueChange = { vm.onQueryChanged(it) },
            placeholder = { Text("Buscar na biblioteca…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (state.query.isNotBlank()) {
                    IconButton(onClick = { vm.onQueryChanged("") }) { Icon(Icons.Default.Clear, null) }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        if (state.query.isBlank() && filtered.isNotEmpty()) {
            val types = state.results.map { it.type }.distinct()
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Todos", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (selectedCategory == null) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null
                    )
                }
                items(types) { type ->
                    FilterChip(
                        selected = selectedCategory == type,
                        onClick = { selectedCategory = if (selectedCategory == type) null else type },
                        label = { Text(type, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (selectedCategory == type) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null
                    )
                }
            }
        }

        Text(
            text = "${filtered.size} ${if (filtered.size == 1) "artigo" else "artigos"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nenhum resultado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("A biblioteca está vazia.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { node ->
                    BibliotecaCard(node)
                }
            }
        }
    }
}

@Composable
private fun BibliotecaCard(node: BibliotecaNode) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = {}, elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Text(node.type.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onPrimaryContainer))
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(node.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    Text(node.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
            if (node.tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    node.tags.take(3).forEach { tag ->
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)).padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
