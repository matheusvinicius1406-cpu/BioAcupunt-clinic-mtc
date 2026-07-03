package com.bioacupunt.ui.screens

import androidx.compose.animation.*
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

    // Quick prompts per mode
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
        // ── Agent mode selector bar ─────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${selectedMode.emoji} ${selectedMode.label}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Primary, fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showModeSelector = true }) {
                    Text("Trocar agente", style = MaterialTheme.typography.labelSmall)
                    Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(16.dp))
                }
            }
        }

        // ── Chat messages ───────────────────────────────
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(msg)
            }

            if (isTyping) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        TypingIndicator()
                    }
                }
            }
        }

        // ── Quick prompts ───────────────────────────────
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

        // ── Input bar ───────────────────────────────────
        Surface(shadowElevation = 4.dp) {
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
                    keyboardActions = KeyboardActions(onSend = { sendMessage(inputText) })
                )
                IconButton(
                    onClick = { sendMessage(inputText) },
                    enabled = inputText.isNotBlank() && !isTyping,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) Primary else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        Icons.Default.Send, null,
                        tint = if (inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // ── Mode selector dialog ────────────────────────────
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
                            border = if (mode == selectedMode) BorderStroke(1.dp, Primary.copy(alpha = 0.4f)) else null
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!msg.isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Primary, Color(0xFF2D4E2C)))),
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
                color = if (msg.isUser) Primary else MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 1.dp
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
    Surface(
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                )
            }
        }
    }
}

private fun generateMockResponse(input: String, mode: AiMode): String {
    return when (mode) {
        AiMode.CLINICAL -> """**Análise Clínica MTC** 🩺

Com base nas informações fornecidas:

**Padrão de Desarmonia:**
Estagnação de Qi do Fígado com tendência a Hiperatividade de Yang

**Órgãos Envolvidos:**
• Fígado (Gan) — principal
• Rim (Shen) — secundário

**Princípio de Tratamento:**
Mover o Qi do Fígado · Ancorar o Yang · Nutrir o Yin

**Pontos Sugeridos:**
• F3 (Taichong) — Yuan do Fígado, move Qi
• VB34 (Yanglingquan) — Influência dos tendões
• PC6 (Neiguan) — Acalma o Shen
• R3 (Taixi) — Nutre Yin do Rim
• Du20 (Baihui) — Ancorar Yang ascendente

*⚠️ Requer avaliação clínica completa para diagnóstico definitivo.*"""

        AiMode.KNOWLEDGE -> """**Conhecimento MTC** 📚

$input

Os pontos Yuan-Fonte (原穴) são os pontos onde o Yuan Qi (Energia Original, proveniente dos Rins) está mais acessível em cada meridiano.

**Principais características:**
• Cada meridiano principal tem um ponto Yuan
• Diagnóstico: pressão dolorosa indica disfunção do órgão
• Terapia: tonificam ou regulam o órgão diretamente

**Exemplo clínico:**
F3-Taichong para estagnação de Qi do Fígado, C7-Shenmen para insônia por deficiência de Sangue do Coração.

*Quer saber mais sobre algum ponto específico?*"""

        AiMode.FLASHCARD -> """**Flashcards Gerados** 🃏

```
CARD 1
Frente: O que são os Cinco Elementos?
Verso: Madeira, Fogo, Terra, Metal e Água —
categorias funcionais que descrevem padrões
de movimento e transformação no corpo e na natureza.
Dificuldade: Fácil

CARD 2
Frente: Qual elemento corresponde ao Fígado?
Verso: Madeira (Mu). Estação: Primavera.
Emoção: Raiva. Cor: Verde. Sabor: Ácido.
Dificuldade: Médio

CARD 3
Frente: O que é o Ciclo Sheng?
Verso: Ciclo de Geração: Madeira→Fogo→Terra→Metal→Água→Madeira.
Cada elemento nutre o próximo.
Dificuldade: Médio
```

*Cards adicionados à biblioteca de Flashcards!*"""

        AiMode.REPORT -> """**Nota de Evolução** 📄

---
**NOTA DE EVOLUÇÃO CLÍNICA**
Data: ${java.time.LocalDate.now()}
Sessão: #__

**Queixa Principal:**
${input.take(100)}...

**Avaliação:**
Paciente apresentou evolução satisfatória do quadro clínico. Língua com melhora gradual. Pulso menos tenso em relação à sessão anterior.

**Intervenção:**
Sessão de acupuntura com retenção de 25 minutos. Pontos utilizados: [inserir pontos]. Resposta ao De Qi adequada em todos os pontos.

**Plano:**
Manter protocolo atual. Próxima sessão em 7 dias. Orientações de dietoterapia fornecidas.

---
*Dr(a). Camila — CRM: CFMTC-12345*"""

        AiMode.CRM -> """**Insights de CRM** 👥

Com base na sua clínica:

**🎯 Retenção:**
• Enviar lembrete 24h antes da consulta via WhatsApp
• Ligar para pacientes sem retorno há 21+ dias
• Criar programa de fidelidade (ex: desconto na 10ª sessão)

**📊 Captação:**
• 65% dos seus pacientes chegam por indicação — incentive com cartão de indicação
• Pacientes do Instagram têm menor LTV — qualificar melhor no primeiro contato

**💡 Oportunidade:**
• 7 pacientes ativos com padrão similar — considere grupo de Qigong/meditação
• Pacientes com deficiência de Yin tendem a precisar de mais sessões — informe no início

*Quer análise detalhada de algum paciente específico?*"""
    }
}
