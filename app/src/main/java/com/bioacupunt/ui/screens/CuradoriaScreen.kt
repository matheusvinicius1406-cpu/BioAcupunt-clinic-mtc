package com.bioacupunt.ui.screens

import android.content.Intent
import android.net.Uri
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
import com.bioacupunt.biblioteca.domain.ingestion.Provenance
import com.bioacupunt.biblioteca.domain.ingestion.ReviewMeta
import com.bioacupunt.biblioteca.domain.model.MtcCategory
import com.bioacupunt.biblioteca.presentation.LibraryReviewViewModel
import com.bioacupunt.core.util.AppJson
import com.bioacupunt.di.AppContainer
import com.bioacupunt.ui.theme.Accent
import com.bioacupunt.ui.theme.Primary
import kotlinx.serialization.encodeToString

/**
 * CURADORIA — a fila de revisão da médica para o pipeline de ingestão (R4).
 *
 * Conteúdo entra por um **arquivo de pacote curado** que a curadora escolhe (import via
 * SAF), ou por **pacotes embutidos** que já vêm com o app. Nunca escrito por IA.
 * O import só *encena* (fila pendente); é o toque em "Aprovar" que autoriza o item
 * a entrar no acervo consultável e no RAG.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuradoriaScreen(onBack: () -> Unit = {}) {
    val vm = viewModel<LibraryReviewViewModel>(factory = AppContainer.libraryReviewViewModelFactory)
    val rawPending by vm.pending.collectAsStateWithLifecycle()
    val pending by vm.filteredPending.collectAsStateWithLifecycle()
    val categoryFilter by vm.categoryFilter.collectAsStateWithLifecycle()
    val provenanceFilter by vm.provenanceFilter.collectAsStateWithLifecycle()
    val searchText by vm.searchText.collectAsStateWithLifecycle()
    val feedback by vm.feedback.collectAsStateWithLifecycle()
    val showBuiltinPacks = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val pcdtPacks = remember(context) { com.bioacupunt.biblioteca.data.packs.PcdtAssetPack.load(context) }
    val openAccessPacks = remember(context) { com.bioacupunt.biblioteca.data.packs.OpenAccessPacks.load(context) }

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
                            "Importe um pacote curado (.json) de fonte revisada, ou use os pacotes " +
                                "embutidos. Cada item precisa de citação. Nada entra no acervo nem na " +
                                "busca clínica sem a sua aprovação aqui.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp)); Text("Importar .json")
                            }
                            OutlinedButton(
                                onClick = { showBuiltinPacks.value = !showBuiltinPacks.value },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Default.LibraryBooks, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (showBuiltinPacks.value) "Ocultar pacotes" else "Pacotes embutidos")
                            }
                        }
                    }
                }
            }

            // ── Built-in packs list ────────────────────────────
            if (showBuiltinPacks.value) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("PACOTES EMBUTIDOS", style = MaterialTheme.typography.labelMedium, color = Primary)
                            Spacer(Modifier.height(8.dp))
                            val mtcPacks = com.bioacupunt.biblioteca.data.packs.MtcContentPacks.allPacks
                            val clinPacks = com.bioacupunt.biblioteca.data.packs.ClinicalMedicinePacks.allPacks
                            val specialtyPacks = com.bioacupunt.biblioteca.data.packs.SpecialtyMedicinePacks.allPacks
                            val emergencyPacks = com.bioacupunt.biblioteca.data.packs.EmergencyPacks.allPacks
                            val pharmacoPacks = com.bioacupunt.biblioteca.data.packs.PharmacologyPacks.allPacks
                            val medicalRefPacks = com.bioacupunt.biblioteca.data.packs.MedicalReferencesPacks.allPacks

                            val packs = mtcPacks + clinPacks + specialtyPacks + emergencyPacks + pharmacoPacks + medicalRefPacks + pcdtPacks + openAccessPacks
                            packs.forEach { pack ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(Icons.Default.AutoStories, null, tint = Accent, modifier = Modifier.size(20.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            pack.source.take(50),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        )
                                        Text(
                                            "${pack.items.size} itens",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            val json = runCatching {
                                                AppJson.encodeToString(pack)
                                            }.getOrNull()
                                            if (json != null) vm.importPackJson(json)
                                        },
                                    ) { Text("Importar", color = Primary) }
                                }
                                if (pack != packs.last()) HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                            }
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
                ReviewQueueFilters(
                    categoryFilter = categoryFilter,
                    provenanceFilter = provenanceFilter,
                    searchText = searchText,
                    onCategoryChange = vm::setCategoryFilter,
                    onProvenanceChange = vm::setProvenanceFilter,
                    onSearchChange = vm::setSearchText,
                )
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
                        if (rawPending.isEmpty()) {
                            "Nenhum conteúdo pendente. Importe um pacote curado para começar."
                        } else {
                            "Nenhum item pendente corresponde ao filtro aplicado."
                        },
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
                    meta = staged.meta,
                    onApprove = { vm.approve(staged.article.id) },
                    onReject = { vm.reject(staged.article.id) },
                )
            }
        }
    }
}

/**
 * Filtros da fila de curadoria — só de EXIBIÇÃO (não afetam o que é aceito no portão
 * de ingestão). Categoria e proveniência com "Todas" como default; busca por título.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewQueueFilters(
    categoryFilter: String?,
    provenanceFilter: Provenance?,
    searchText: String,
    onCategoryChange: (String?) -> Unit,
    onProvenanceChange: (Provenance?) -> Unit,
    onSearchChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar por título…") },
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpar busca", modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
        )

        var categoryMenuExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = categoryMenuExpanded,
            onExpandedChange = { categoryMenuExpanded = it },
        ) {
            OutlinedTextField(
                value = categoryFilter?.let { name -> MtcCategory.entries.find { it.name == name }?.label ?: name } ?: "Todas as categorias",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                label = { Text("Categoria") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                textStyle = MaterialTheme.typography.bodySmall,
            )
            ExposedDropdownMenu(
                expanded = categoryMenuExpanded,
                onDismissRequest = { categoryMenuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Todas as categorias") },
                    onClick = { onCategoryChange(null); categoryMenuExpanded = false },
                )
                MtcCategory.entries.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text("${cat.emoji} ${cat.label}") },
                        onClick = { onCategoryChange(cat.name); categoryMenuExpanded = false },
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = provenanceFilter == null,
                onClick = { onProvenanceChange(null) },
                label = { Text("Todas") },
            )
            FilterChip(
                selected = provenanceFilter == Provenance.VERIFICAVEL,
                onClick = { onProvenanceChange(Provenance.VERIFICAVEL) },
                label = { Text("Verificável") },
            )
            FilterChip(
                selected = provenanceFilter == Provenance.RASCUNHO,
                onClick = { onProvenanceChange(Provenance.RASCUNHO) },
                label = { Text("Rascunho") },
            )
        }
    }
}

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
private fun StagedCard(
    title: String,
    category: String,
    source: String,
    citation: String,
    preview: String,
    meta: ReviewMeta,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Card(
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                ProvenanceBadge(meta.provenance)
                Spacer(Modifier.width(6.dp))
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
            if (meta.sourceRef.isNotBlank()) {
                Text("Localizador: ${meta.sourceRef}", style = MaterialTheme.typography.labelSmall, color = Primary)
            }
            Text("Citação: $citation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (meta.sourceUrl.isNotBlank()) {
                val context = LocalContext.current
                TextButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(meta.sourceUrl)))
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp),
                ) {
                    Icon(Icons.Default.OpenInNew, null, tint = Primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Abrir fonte", style = MaterialTheme.typography.labelSmall, color = Primary)
                }
            }
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
