package com.bioacupunt.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import com.bioacupunt.ui.theme.Primary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val content: String,
    val isUser: Boolean,
    val agentName: String = "BioAcupunt AI"
)

private enum class AiMode(val label: String, val emoji: String, val hint: String) {
    CLINICAL("Clínico", "🩺", "Descreva queixas, língua, pulso..."),
    KNOWLEDGE("Conhecimento", "📚", "Pergunte sobre MTC, pontos, meridianos..."),
    FLASHCARD("Flashcard", "🃏", "Peça para criar flashcards sobre um tema"),
    REPORT("Relatório", "📄", "Peça para gerar nota de evolução ou laudo"),
    CRM("CRM", "👥", "Peça insights sobre pacientes e agenda")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen() {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    content = "Olá, Dra. Camila! 👋\n\nSou o Assistente IA do BioAcupunt, powered by **Gemini 2.0 Flash**.\n\nPosso ajudar com:\n• 🩺 Diagnóstico energético MTC\n• 📚 Dúvidas sobre pontos e meridianos\n• 📄 Notas de evolução e laudos\n• 🃏 Criar flashcards de estudo\n• 👥 Insights de CRM e pacientes\n\nComo posso ajudar hoje?",
                    isUser = false,
                    agentName = "BioAcupunt AI"
                )
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf(AiMode.CLINICAL) }
    var showModeSelector by remember { mutableStateOf(false) }

    val quickPrompts = remember(selectedMode) {
        when (selectedMode) {
            AiMode.CLINICAL -> listOf(
                "Paciente com cefaleia, irritabilidade, pulso tenso",
                "Insônia com suor noturno e boca seca",
                "Fadiga crônica com fezes moles e apetite baixo"
            )
            AiMode.KNOWLEDGE -> listOf(
                "Explique os pontos Yuan-Fonte",
                "Diferença entre Qi e Sangue",
                "Quando usar moxibustão?"
            )
            AiMode.FLASHCARD -> listOf(
                "Crie 5 flashcards sobre Cinco Elementos",
                "Flashcards de semiologia de língua",
                "Cards sobre pontos do meridiano do Rim"
            )
            AiMode.REPORT -> listOf(
                "Nota de evolução para sessão de acupuntura",
                "Laudo de avaliação inicial MTC",
                "Relatório de alta terapêutica"
            )
            AiMode.CRM -> listOf(
                "Dicas para fidelizar pacientes",
                "Como agendar retornos de forma eficaz",
                "Estratégias de comunicação com pacientes"
            )
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = ChatMessage(content = text.trim(), isUser = true)
        messages = messages + userMsg
        inputText = ""
        isTyping = true

        scope.launch {
            listState.animateScrollToItem(messages.size)

            val request = com.bioacupunt.ai.domain.model.AiRequest(
                prompt = text.trim(),
                systemPrompt = "",
                temperature = 0.7,
                maxTokens = 2048
            )

            val response: String = try {
                val result = com.bioacupunt.di.AppContainer.generateAiResponse(request)
                result.getOrNull()?.text.orEmpty().ifBlank { generateMockResponse(text.trim(), selectedMode) }
            } catch (e: Exception) {
                generateMockResponse(text.trim(), selectedMode)
            }

            messages = messages + ChatMessage(
                content = response,
                isUser = false,
                agentName = selectedMode.label + " AI"
            )
            isTyping = false
            listState.animateScrollToItem(messages.size)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth().premiumShadow(MaterialTheme.shapes.extraLarge, Primary.copy(alpha = 0.18f), 12.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${selectedMode.emoji} ${selectedMode.label}",
                    style = MaterialTheme.typography.labelMedium.copy(color = Primary, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showModeSelector = true }) {
                    Text("Trocar agente", style = MaterialTheme.typography.labelSmall)
                    Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(16.dp))
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg -> ChatBubble(msg) }

            if (isTyping) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        TypingIndicator()
                    }
                }
            }
        }

        if (messages.size <= 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                items(quickPrompts) { prompt ->
                    SuggestionChip(
                        onClick = { sendMessage(prompt) },
                        label = { Text(prompt, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), tonalElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(selectedMode.hint, style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendMessage(inputText) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        containerColor = Color.White.copy(alpha = 0.10f),
                        cursorColor = Primary,
                        focusedBorderColor = Primary.copy(alpha = 0.6f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                    )
                )
                IconButton(
                    onClick = { sendMessage(inputText) },
                    enabled = inputText.isNotBlank() && !isTyping,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Primary,
                                    Primary.copy(alpha = 0.75f)
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                        .shadow(8.dp, shape = CircleShape, spotColor = Primary.copy(alpha = 0.25f))
                ) {
                    Icon(
                        Icons.Default.Send, null,
                        tint = Color.White
                    )
                }
            }
        }
    }

    if (showModeSelector) {
        AlertDialog(
            onDismissRequest = { showModeSelector = false },
            title = { Text("Escolher Agente de IA") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AiMode.entries.forEach { mode ->
                        Card(
                            onClick = { selectedMode = mode; showModeSelector = false },
                            colors = CardDefaults.cardColors(
                                containerColor = if (mode == selectedMode) Primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = if (mode == selectedMode) BorderStroke(1.dp, Primary.copy(alpha = 0.4f)) else null,
                            modifier = Modifier.fillMaxWidth().premiumShadow(MaterialTheme.shapes.large, Color.Black.copy(alpha = 0.06f), 10.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(mode.emoji, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(mode.label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    Text(mode.hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (mode == selectedMode) Icon(Icons.Default.CheckCircle, null, tint = Primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showModeSelector = false }) { Text("Fechar") } }
        )
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val alphaInfinite = rememberInfiniteTransition(label = "bubble")
    val pulse by alphaInfinite.animateFloat(1f, 1.35f, animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "pulse")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!msg.isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Primary, Color(0xFF2D4E2C))))
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SmartToy, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(6.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
        ) {
            if (!msg.isUser) {
                Text(
                    msg.agentName,
                    style = MaterialTheme.typography.labelSmall.copy(color = Primary),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (msg.isUser) 16.dp else 4.dp,
                    topEnd = if (msg.isUser) 4.dp else 16.dp,
                    bottomStart = 16.dp, bottomEnd = 16.dp
                ),
                color = if (msg.isUser) Primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shadowElevation = 2.dp
            ) {
                Text(
                    msg.content,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (msg.isUser) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val dots = listOf(0, 200, 400)
    val phase = rememberInfiniteTransition(label = "typing")
    val t by phase.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "typingPhase"
    )

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), shadowElevation = 1.dp) {
            Text("🩺", modifier = Modifier.padding(4.dp))
        }
        repeat(3) { index ->
            val progress = ((t * 3f - index).coerceIn(0f, 1f))
            val d = 4.dp + (6.dp * progress)
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.4f + 0.6f * progress)),
                contentAlignment = Alignment.Center
            ) {}
        }
    }
}

private fun generateMockResponse(question: String, mode: AiMode): String {
    val q = question.lowercase()
    return when (mode) {
        AiMode.CLINICAL -> "Pelo padrão descrito, pode indicar Desequilíbrio de Fígado + Deficiência de Yin. Sugiro pontos: LV3, KI6, PC6 e follow-up em 7 dias."
        AiMode.KNOWLEDGE -> "Os pontos Yuan-Fonte são pontos de origem do Jing nos meridianos, abertos apenas em deficiência orgânica. São usados para tonificar o elemento raiz."
        AiMode.FLASHCARD -> "Flashcards criados para revisão rápida, com reiteração espaçada e ícones visuais por elemento."
        AiMode.REPORT -> "Relatório gerado com base na sessão atual. Inclui pontos, sinais e plano terapêutico."
        AiMode.CRM -> "Sugestão: para fidelizar, use lembretes automáticos e follow-up pós-sessão em até 48h."
    }
}
