package com.bioacupunt.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.biblioteca.presentation.LibraryReviewViewModel
import com.bioacupunt.di.AppContainer
import com.bioacupunt.ui.theme.Primary

/**
 * CURADORIA — a fila de revisão da médica para o pipeline de ingestão (R4).
 *
 * Conteúdo entra por um **arquivo de pacote curado** que a curadora escolhe (import via
 * SAF), nunca escrito por IA. O import só *encena* (fila pendente); é o toque em "Aprovar"
 * que autoriza o item a entrar no acervo consultável e no RAG. "Aprovado" vs "Pendente"
 * é mostrado explicitamente — silêncio é ambíguo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuradoriaScreen(onBack: () -> Unit = {}) {
    val vm = viewModel<LibraryReviewViewModel>(factory = AppContainer.libraryReviewViewModelFactory)
    val pending by vm.pending.collectAsStateWithLifecycle()
    val feedback by vm.feedback.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val json = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (json != null) vm.importPackJson(json)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Curadoria da Biblioteca") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.06f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f)),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Conteúdo revisado, não gerado", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Importe um pacote curado (.json) de fonte revisada. Cada item precisa de " +
                                "citação. Nada entra no acervo nem na busca clínica sem a sua aprovação aqui.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        ) {
                            Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp)); Text("Importar pacote curado")
                        }
                    }
                }
            }

            feedback?.let { msg ->
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = Primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(msg, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.clearFeedback() }) { Text("OK") }
                    }
                }
            }

            item {
                Text(
                    "PENDENTES DE REVISÃO · ${pending.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (pending.isEmpty()) {
                item {
                    Text(
                        "Nenhum conteúdo pendente. Importe um pacote curado para começar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }

            items(pending, key = { it.article.id }) { staged ->
                StagedCard(
                    title = staged.article.title,
                    category = staged.article.category,
                    source = staged.meta.source,
                    citation = staged.meta.citation,
                    preview = staged.article.content.take(280),
                    onApprove = { vm.approve(staged.article.id) },
                    onReject = { vm.reject(staged.article.id) },
                )
            }
        }
    }
}

@Composable
private fun StagedCard(
    title: String,
    category: String,
    source: String,
    citation: String,
    preview: String,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Card(
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFFFA000).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text("Pendente", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB8860B))
                }
            }
            Text(category, style = MaterialTheme.typography.labelSmall, color = Primary)
            Spacer(Modifier.height(6.dp))
            Text(preview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text("Fonte: $source", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Citação: $citation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Aprovar")
                }
                OutlinedButton(onClick = onReject) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Rejeitar")
                }
            }
        }
    }
}
