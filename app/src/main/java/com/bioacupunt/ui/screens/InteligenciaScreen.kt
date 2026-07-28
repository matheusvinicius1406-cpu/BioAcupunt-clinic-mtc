package com.bioacupunt.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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

/**
 * INTELIGÊNCIA — chat único de IA do BioAcupunt.
 *
 * Toda pergunta passa primeiro pelo gate R2 (`AskLibraryUseCase`) e só cai no assistente
 * livre quando a biblioteca não tem evidência — ver a documentação do ViewModel para os
 * detalhes de por que isso não abre brecha na R2.
 *
 * Layout de chat padrão: cabeçalho compacto, mensagens ocupando o espaço disponível,
 * caixa de entrada fixa embaixo. Sem cards promocionais nem sugestões pré-definidas —
 * a médica digita a pergunta dela.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteligenciaScreen() {
    val vm = viewModel<UnifiedAiChatViewModel>(factory = AppContainer.unifiedAiChatViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) scope.launch { listState.animateScrollToItem(state.messages.size - 1) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Header compacto ──────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(Primary, Accent))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.SmartToy, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text("Inteligência", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.refreshContext() }) {
                Icon(Icons.Default.Refresh, "Atualizar contexto do app", tint = TextMuted, modifier = Modifier.size(20.dp))
            }
        }
        HorizontalDivider()

        // ── Contexto do app (só quando carregado) ────────────
        AnimatedVisibility(
            visible = !state.contextLoaded.isNullOrBlank(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Default.Info, null, tint = Primary, modifier = Modifier.size(13.dp))
                Text(
                    "Contexto do app carregado",
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary,
                )
            }
        }

        // ── Mensagens — ocupa o espaço disponível ────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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

        HorizontalDivider()

        // ── Entrada ───────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White)
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
                // Mensagem da médica é texto puro (não precisa de markdown); a resposta da IA
                // frequentemente vem com **negrito**/listas/títulos — sem isto, o balão mostrava
                // os caracteres crus (`**`, `#`, `-`) em vez de formatação de verdade.
                if (isUser) {
                    Text(turn.text, style = MaterialTheme.typography.bodySmall, color = Color.White)
                } else {
                    MarkdownText(turn.text)
                }
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
