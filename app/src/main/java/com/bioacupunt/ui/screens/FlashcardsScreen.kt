package com.bioacupunt.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.biblioteca.domain.model.MtcArticle
import com.bioacupunt.core.util.MarkdownSections
import com.bioacupunt.educacao.domain.model.Flashcard
import com.bioacupunt.educacao.presentation.FlashcardsUiState
import com.bioacupunt.educacao.presentation.FlashcardsViewModel
import com.bioacupunt.ui.theme.FlashcardBack
import com.bioacupunt.ui.theme.FlashcardFront
import com.bioacupunt.ui.theme.FlashcardGold
import com.bioacupunt.ui.theme.FlashcardGreen
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.StatusColorScheme
import com.bioacupunt.ui.theme.statusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(onBack: (() -> Unit)? = null, viewModel: FlashcardsViewModel? = null) {
    val vm = viewModel ?: viewModel(factory = com.bioacupunt.di.AppContainer.flashcardsViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()
    var isFlipped by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    // Vira a carta de novo sempre que a fila avança ou é reconstruída.
    LaunchedEffect(state.queueIndex, state.queue) { isFlipped = false }

    val status = statusColors()
    val categories = state.deck.map { it.card.category }.distinct()
    val filteredDeck = state.deckForCategory()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", modifier = Modifier.size(20.dp))
                }
            }
            AssistChip(
                onClick = {},
                label = { Text("${state.dueCount} para revisar hoje") },
                leadingIcon = { Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp)) },
            )
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Add, "Novo card")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Novo card") },
                        leadingIcon = { Icon(Icons.Default.EditNote, null) },
                        onClick = { menuExpanded = false; vm.openNewCardEditor() }
                    )
                    DropdownMenuItem(
                        text = { Text("Criar de artigo") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, null) },
                        onClick = { menuExpanded = false; vm.startCreateFromArticle() }
                    )
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            item {
                FilterChip(
                    selected = state.selectedCategory == null,
                    onClick = { vm.onCategorySelected(null) },
                    label = { Text("Todos (${state.deck.size})") }
                )
            }
            items(categories) { cat ->
                FilterChip(
                    selected = state.selectedCategory == cat,
                    onClick = { vm.onCategorySelected(cat) },
                    label = { Text(cat) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                filteredDeck.isEmpty() -> EmptyState(
                    icon = Icons.Default.Inbox,
                    title = "Nenhum card disponível",
                    subtitle = "Crie um card novo ou a partir de um artigo aprovado.",
                )

                state.sessionComplete -> SessionSummary(state = state, onRestart = vm::restartSession)

                state.queue.isEmpty() -> NothingDueState(
                    nextDueAtEpochMs = state.nextDueAtEpochMs,
                    onStudyAnyway = vm::studyAnyway,
                )

                else -> StudyArea(
                    state = state,
                    status = status,
                    isFlipped = isFlipped,
                    onFlip = { isFlipped = !isFlipped },
                    onAnswer = { remembered ->
                        if (!isFlipped) isFlipped = true else vm.onAnswer(remembered)
                    },
                    onEdit = vm::openEditCardEditor,
                    onDelete = vm::deleteCard,
                )
            }
        }
    }

    state.editor?.let { draft ->
        FlashcardEditorDialog(
            draft = draft,
            onFrontChange = { front -> vm.updateEditor { it.copy(front = front) } },
            onBackChange = { back -> vm.updateEditor { it.copy(back = back) } },
            onCategoryChange = { category -> vm.updateEditor { it.copy(category = category) } },
            onConfirm = vm::confirmEditor,
            onCancel = vm::cancelEditor,
        )
    }

    state.createFromArticle?.let { picker ->
        if (picker.selectedArticle == null) {
            ArticlePickerDialog(
                articles = picker.articles,
                isLoading = picker.isLoading,
                onSelect = vm::selectArticleForCreate,
                onDismiss = vm::cancelCreateFromArticle,
            )
        } else {
            SectionPickerDialog(
                articleTitle = picker.selectedArticle.title,
                sections = picker.sections,
                onSelect = vm::selectSectionForCreate,
                onDismiss = vm::cancelCreateFromArticle,
            )
        }
    }

    state.message?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3000)
            vm.dismissMessage()
        }
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
            Snackbar { Text(msg) }
        }
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun NothingDueState(nextDueAtEpochMs: Long?, onStudyAnyway: () -> Unit) {
    val nextDateText = remember(nextDueAtEpochMs) {
        nextDueAtEpochMs?.let {
            val date = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            date.format(
                java.time.format.DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy",
                    java.util.Locale.Builder().setLanguage("pt").setRegion("BR").build()
                )
            )
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.EventAvailable, null, modifier = Modifier.size(48.dp), tint = FlashcardGreen)
        Spacer(Modifier.height(16.dp))
        Text("Nenhum card vencido", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            if (nextDateText != null) "Próxima revisão em $nextDateText" else "Tudo em dia por aqui.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onStudyAnyway) { Text("Estudar mesmo assim") }
    }
}

@Composable
private fun SessionSummary(state: FlashcardsUiState, onRestart: () -> Unit) {
    val status = statusColors()
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(48.dp), tint = status.success)
        Spacer(Modifier.height(16.dp))
        Text("Sessão concluída", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${state.knownCount}", style = MaterialTheme.typography.headlineMedium, color = status.success)
                Text("Sei", style = MaterialTheme.typography.labelMedium)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${state.unknownCount}", style = MaterialTheme.typography.headlineMedium, color = status.danger)
                Text("Não sei", style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRestart) { Text("Reiniciar") }
    }
}

@Composable
private fun StudyArea(
    state: FlashcardsUiState,
    status: StatusColorScheme,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onAnswer: (Boolean) -> Unit,
    onEdit: (Flashcard) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val studyCard = state.currentCard ?: return
    Column(modifier = Modifier.fillMaxSize()) {
        LinearProgressIndicator(
            progress = { (state.queueIndex + 1f) / state.queue.size },
            modifier = Modifier.fillMaxWidth(),
            color = Primary
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${state.queueIndex + 1} / ${state.queue.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Check, null, tint = status.success, modifier = Modifier.size(16.dp))
                    Text("${state.knownCount}", style = MaterialTheme.typography.labelMedium, color = status.success)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Close, null, tint = status.danger, modifier = Modifier.size(16.dp))
                    Text("${state.unknownCount}", style = MaterialTheme.typography.labelMedium, color = status.danger)
                }
            }
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            FlipCard(card = studyCard.card, isFlipped = isFlipped, onClick = onFlip)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            AssistChip(onClick = {}, label = { Text(studyCard.card.category) })
            Spacer(Modifier.width(8.dp))
            AssistChip(
                onClick = {},
                label = { Text("Caixa ${studyCard.progress?.box ?: 0}") },
                colors = AssistChipDefaults.assistChipColors(containerColor = Primary.copy(0.1f))
            )
            if (studyCard.card.builtin) {
                Spacer(Modifier.width(8.dp))
                AssistChip(onClick = {}, label = { Text("fixo") })
            } else {
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { onEdit(studyCard.card) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, "Editar", modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { studyCard.card.userRowId?.let(onDelete) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Delete, "Apagar", modifier = Modifier.size(18.dp), tint = status.danger)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { onAnswer(false) },
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = status.danger)
            ) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Não sei")
            }
            Button(
                onClick = { onAnswer(true) },
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = status.success)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Sei!")
            }
        }
    }
}

@Composable
private fun FlipCard(card: Flashcard, isFlipped: Boolean, onClick: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "flip"
    )
    val showFront = rotation < 90f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.4f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(20.dp))
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    if (showFront) FlashcardFront else FlashcardBack
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = showFront,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(100))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.AutoMirrored.Filled.Help, null, tint = Primary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    card.front,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
                    )
                )
                Spacer(Modifier.height(16.dp))
                Text("Toque para revelar", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f)))
            }
        }

        AnimatedVisibility(
            visible = !showFront,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(100))
        ) {
            Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.Lightbulb, null, tint = FlashcardGold, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(16.dp))
                    // O verso costuma vir de uma seção extraída VERBATIM de um artigo da
                    // biblioteca, que é markdown — então `**negrito**` e listas apareciam
                    // crus aqui. Alinhado à esquerda de propósito: uma resposta de várias
                    // linhas centralizada fica difícil de ler.
                    MarkdownText(
                        card.back,
                        bodyStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White, fontWeight = FontWeight.Medium,
                        ),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Resposta ✓", style = MaterialTheme.typography.labelSmall.copy(color = FlashcardGreen))
                }
            }
        }
    }
}

@Composable
private fun FlashcardEditorDialog(
    draft: Flashcard,
    onFrontChange: (String) -> Unit,
    onBackChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (draft.userRowId == null) "Novo card" else "Editar card") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (draft.sourceArticleId.isNotEmpty()) {
                    Text(
                        "De: ${draft.sourceSection}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = draft.front,
                    onValueChange = onFrontChange,
                    label = { Text("Frente") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                OutlinedTextField(
                    value = draft.back,
                    onValueChange = onBackChange,
                    label = { Text("Verso") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                OutlinedTextField(
                    value = draft.category,
                    onValueChange = onCategoryChange,
                    label = { Text("Categoria") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar") } },
    )
}

@Composable
private fun ArticlePickerDialog(
    articles: List<MtcArticle>,
    isLoading: Boolean,
    onSelect: (MtcArticle) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Criar de artigo — escolha o artigo") },
        text = {
            when {
                isLoading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                articles.isEmpty() -> Text("Nenhum artigo aprovado na biblioteca ainda.")
                else -> LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(articles) { article ->
                        Text(
                            article.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(article) }
                                .padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun SectionPickerDialog(
    articleTitle: String,
    sections: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(articleTitle) },
        text = {
            if (sections.isEmpty()) {
                Text("Este artigo não tem seções demarcadas (#/##/###).")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(sections) { section ->
                        Text(
                            MarkdownSections.titleOf(section),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(section) }
                                .padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
