package com.bioacupunt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.biblioteca.presentation.ChatRole
import com.bioacupunt.biblioteca.presentation.ChatTurn
import com.bioacupunt.di.AppContainer
import com.bioacupunt.ui.theme.Accent
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.TextMuted
import kotlinx.coroutines.launch

private val quickSuggestions = listOf(
    "Diferença entre Qi e Xue",
    "Quando usar moxibustão?",
    "Pontos Yuan-Fonte",
)

/**
 * INTELIGÊNCIA — "Consultar IA". A única IA clínica no app: RAG ancorado nos 16
 * artigos revisados via [com.bioacupunt.biblioteca.domain.usecase.AskLibraryUseCase].
 * Sem modo "CRM"/"Relatório"/"Flashcard" com chamada solta ao modelo, e sem
 * resposta fabricada quando a IA falha — essa era exatamente a versão anterior
 * desta tela, e foi removida.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(onNavigateToCRM: () -> Unit = {}) {
    val vm = viewModel<com.bioacupunt.biblioteca.presentation.AiAssistantViewModel>(factory = AppContainer.aiAssistantViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) scope.launch { listState.animateScrollToItem(state.messages.size - 1) }
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Inteligência", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold))
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraLarge)
                    .padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                ) {
                    Box(
                        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Brush.linearGradient(listOf(Primary, Accent))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.SmartToy, null, tint = Color.White, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Consultar IA", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text("motor MTC · RAG com referências", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.messages) { turn -> ChatBubble(turn) }
                    if (state.thinking) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Text("consultando a biblioteca...", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    quickSuggestions.forEach { s ->
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraLarge)
                                .background(MaterialTheme.colorScheme.background)
                                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraLarge)
                                .clickable(enabled = !state.thinking) { vm.send(s) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(s, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.input,
                        onValueChange = vm::onInputChanged,
                        placeholder = { Text("Pergunte à IA...") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.extraLarge,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { vm.send() }),
                    )
                    IconButton(
                        onClick = { vm.send() },
                        enabled = state.input.isNotBlank() && !state.thinking,
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Primary),
                    ) {
                        Icon(Icons.Default.Send, null, tint = Color.White)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                    .padding(18.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Análise de prontuário", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        TextButton(onClick = onNavigateToCRM, contentPadding = PaddingValues(0.dp)) { Text("Abrir →", style = MaterialTheme.typography.labelSmall) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Abra o prontuário de um paciente para ver Ba Gang, língua, pulso e evolução — e perguntar sobre o caso aqui.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.VerifiedUser, null, tint = Primary, modifier = Modifier.size(20.dp))
                Text(
                    buildAnnotatedStringCompat("Auditoria da IA: ", "toda resposta vem só de artigos da biblioteca revisados — sem evidência, sem resposta. Você pode conferir a fonte de cada trecho."),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(turn: ChatTurn) {
    val isUser = turn.role == ChatRole.USER
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start, modifier = Modifier.widthIn(max = 280.dp)) {
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
                Text(
                    turn.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                )
            }
            if (turn.sources.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    turn.sources.take(3).forEach { source ->
                        Box(modifier = Modifier.clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("[${source.ordinal}] ${source.articleTitle}", style = MaterialTheme.typography.labelSmall, color = Primary, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

private fun buildAnnotatedStringCompat(bold: String, rest: String) = androidx.compose.ui.text.buildAnnotatedString {
    withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
    append(rest)
}
