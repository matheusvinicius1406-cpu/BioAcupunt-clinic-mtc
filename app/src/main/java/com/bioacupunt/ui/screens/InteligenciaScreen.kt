package com.bioacupunt.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.bioacupunt.ai.presentation.UnifiedAiChatViewModel
import com.bioacupunt.ai.presentation.UnifiedChatRole
import com.bioacupunt.ai.presentation.UnifiedChatTurn
import com.bioacupunt.di.AppContainer
import com.bioacupunt.ui.theme.Accent
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.TextMuted
import kotlinx.coroutines.launch

private val quickSuggestions = listOf(
    "Diferença entre Qi e Xue",
    "Quando usar moxibustão?",
    "Resumo do meu dia",
    "Como usar o prontuário?",
)

/**
 * INTELIGÊNCIA — chat único de IA do BioAcupunt.
 *
 * Antes eram duas abas ("Consultar IA" e "Chat Geral"); a médica pediu um chat só. A fusão é
 * feita inteiramente em [UnifiedAiChatViewModel]: toda pergunta passa primeiro pelo gate R2
 * (`AskLibraryUseCase`) e só cai no assistente livre quando a biblioteca não tem evidência —
 * ver a documentação do ViewModel para os detalhes de por que isso não abre brecha na R2.
 *
 * Esta tela é só a apresentação: uma lista de mensagens (com fontes quando a resposta veio da
 * biblioteca) e uma caixa de input. Não decide roteamento — o ViewModel decide.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteligenciaScreen(
    onNavigateToCRM: () -> Unit = {},
) {
    val vm = viewModel<UnifiedAiChatViewModel>(factory = AppContainer.unifiedAiChatViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) scope.launch { listState.animateScrollToItem(state.messages.size - 1) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
                        Text("BioAI", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text("MTC com fontes · dúvidas do app", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))

                // ── Context banner ────────────────────────────────────
                AnimatedVisibility(
                    visible = state.contextLoaded != null && state.contextLoaded!!.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    state.contextLoaded?.let { ctx ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, tint = Primary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Contexto do app carregado",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Primary,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(ctx, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.messages) { turn -> ChatBubble(turn) }
                    if (state.thinking) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Text("pensando...", style = MaterialTheme.typography.labelSmall, color = TextMuted)
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
                    .clickable { vm.refreshContext() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.Refresh, null, tint = Primary, modifier = Modifier.size(20.dp))
                Text("Atualizar contexto do app", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                Spacer(Modifier.weight(1f))
                Text("toque para recarregar", style = MaterialTheme.typography.labelSmall, color = TextMuted)
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
                    buildAnnotatedStringCompat(
                        "Auditoria da IA: ",
                        "perguntas de MTC com evidência na biblioteca respondem só com base nos artigos revisados, " +
                            "com fontes que você pode conferir. Perguntas sem evidência (clínicas ou gerais sobre o " +
                            "app) usam o assistente livre, que recusa dar diagnóstico ou conselho clínico.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(turn: UnifiedChatTurn) {
    val isUser = turn.role == UnifiedChatRole.USER
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
