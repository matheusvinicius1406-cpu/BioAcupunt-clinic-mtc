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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.ai.data.provider.LocalModelManager
import com.bioacupunt.ai.data.provider.ModelDownloadWorker
import com.bioacupunt.ai.presentation.UnifiedAiChatViewModel
import com.bioacupunt.ai.presentation.UnifiedChatRole
import com.bioacupunt.ai.presentation.UnifiedChatTurn
import com.bioacupunt.di.AppContainer
import com.bioacupunt.ui.theme.Accent
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.SemanticError
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
 *
 * ## Modo patient-aware
 *
 * `patientId > 0` (alcançado a partir do botão "Perguntar à IA sobre este caso" no
 * Prontuário) escopa o ViewModel por paciente e mostra uma faixa com o nome real. O
 * chat NÃO reimplementa a síntese diagnóstica — [onOpenSynthesis] leva de volta ao
 * `ClinicalSynthesisUseCase` já existente (aba Plano do Prontuário), evitando duplicar
 * a lógica de gerar/aceitar sugestão em dois lugares.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteligenciaScreen(
    patientId: Long = 0L,
    onBack: (() -> Unit)? = null,
    onOpenSynthesis: (Long) -> Unit = {},
) {
    val vm = viewModel<UnifiedAiChatViewModel>(
        key = "chat-$patientId",
        factory = AppContainer.unifiedAiChatViewModelFactory(patientId),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val modelManager = remember { AppContainer.localModelManager }
    val modelState by modelManager.state.collectAsState(initial = LocalModelManager.State.Absent)

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) scope.launch { listState.animateScrollToItem(state.messages.size - 1) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Header compacto ──────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = TextMuted)
                }
                Spacer(Modifier.width(4.dp))
            }
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

        // ── Prontidão do modelo local — a médica nunca deveria descobrir que a IA
        // não está pronta só ao mandar uma pergunta e ver "IA não configurada".
        // Baixar daqui usa o mesmo WorkManager de Ajustes > IA (ModelDownloadWorker),
        // então sobrevive a sair desta tela no meio do download.
        AnimatedVisibility(visible = modelState !is LocalModelManager.State.Ready, enter = fadeIn(), exit = fadeOut()) {
            ModelReadinessBanner(state = modelState, context = context)
        }

        // ── Paciente em foco (só quando o chat foi aberto a partir do Prontuário) ──
        AnimatedVisibility(
            visible = patientId > 0L && !state.patientName.isNullOrBlank(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Default.Person, null, tint = Primary, modifier = Modifier.size(14.dp))
                Text(
                    "Conversando sobre: ${state.patientName}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onOpenSynthesis(patientId) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Ver síntese diagnóstica", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

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
            items(state.messages, key = { it.id }) { turn -> ChatBubble(turn) }
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

/**
 * Faixa compacta de prontidão do modelo local. Antes disto, a médica só descobria que
 * a IA não tinha modelo baixado ao receber "assistente não configurado" no meio de uma
 * pergunta — a única forma de agir era navegar até Ajustes > IA, um passo a mais que
 * ela não sabia que precisava dar. Aqui ela vê e resolve sem sair do chat.
 */
@Composable
private fun ModelReadinessBanner(state: LocalModelManager.State, context: android.content.Context) {
    val securePrefs = remember { AppContainer.securePreferences }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        when (state) {
            is LocalModelManager.State.Downloading -> {
                val pct = (state.progress * 100).toInt().coerceIn(0, 100)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Download, null, tint = Primary, modifier = Modifier.size(16.dp))
                    Text("Baixando modelo local… $pct% (pode sair desta tela, o download continua)", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { state.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            }
            is LocalModelManager.State.Failed -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ErrorOutline, null, tint = SemanticError, modifier = Modifier.size(16.dp))
                    Text("Falha ao baixar o modelo: ${state.message}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { ModelDownloadWorker.enqueue(context, LocalModelManager.resolveUrl(securePrefs.localModelUrl)) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) { Text("Tentar de novo") }
                }
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, null, tint = Primary, modifier = Modifier.size(16.dp))
                    Text("IA local ainda não baixada — respostas indisponíveis até baixar.", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { ModelDownloadWorker.enqueue(context, LocalModelManager.resolveUrl(securePrefs.localModelUrl)) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) { Text("Baixar agora") }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(turn: UnifiedChatTurn) {
    val isUser = turn.role == UnifiedChatRole.USER
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        // Balão largo: 280dp fixo espremia a resposta em telefones grandes e
        // pior, em tablets. Proporcional à tela, com teto razoável para leitura.
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start, modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(0.92f)) {
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
