package com.bioacupunt.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.bioacupunt.ui.theme.Primary

private data class Flashcard(
    val frente: String,
    val verso: String,
    val categoria: String,
    val dificuldade: String = "médio"
)

private val builtInFlashcards = listOf(
    Flashcard("O que é o De Qi?", "Sensação de peso, distensão, dormência ou formigamento ao redor do ponto durante o agulhamento, indicando que o Qi chegou.", "Técnicas"),
    Flashcard("Quais são os Oito Princípios (Ba Gang)?", "Yin/Yang, Interior/Exterior, Frio/Calor, Deficiência/Excesso. São a base do diagnóstico diferencial em MTC.", "Ba Gang"),
    Flashcard("Qual a função principal do Fígado em MTC?", "Promover o livre fluxo do Qi (疏泄). Também armazena o Sangue, controla tendões e abre para os olhos.", "Órgãos"),
    Flashcard("Localização do Zusanli (E36)?", "4 cun abaixo da patela, 1 cun lateral à crista da tíbia. Principal ponto de tonificação geral.", "Pontos"),
    Flashcard("Qual o horário de pico do meridiano do Fígado?", "1h às 3h da manhã (hora do Boi). Ideal para tratar condições do Fígado com moxibustão neste horário.", "Meridianos"),
    Flashcard("O que indica uma língua vermelha sem saburra?", "Deficiência de Yin com calor vazio. Comum em deficiência de Yin do Rim ou Fígado.", "Semiologia"),
    Flashcard("Quais são os Cinco Elementos e seus órgãos?", "Madeira→Fígado/VB, Fogo→Coração/ID, Terra→Baço/E, Metal→Pulmão/IG, Água→Rim/B", "Cinco Elementos"),
    Flashcard("O que é o pulso Xian (弦)?", "Pulso tenso como corda de arco. Indica estagnação de Qi do Fígado, dor, ou presença de Fleuma.", "Pulso"),
    Flashcard("O que são os pontos Yuan-Fonte?", "Pontos que acumulam Yuan Qi (Qi Original). Usados para diagnosticar e tratar os órgãos internos correspondentes.", "Pontos Especiais"),
    Flashcard("Qual o Mu do Fígado?", "F14 - Qimen, localizado no 6° espaço intercostal, na linha medioclavicular. Tratar diretamente o Fígado.", "Pontos Especiais"),
    Flashcard("Diferença entre Qi e Sangue (Xue)?", "Qi é Yang, impalpável, move e transforma. Sangue é Yin, nutre e umidifica. O Qi move o Sangue; o Sangue é a mãe do Qi.", "Teoria"),
    Flashcard("O que é o Shen?", "O Espírito/Mente armazenado no Coração. Controla a consciência, cognição, memória e sono. Reflete-se nos olhos.", "Teoria")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(onBack: (() -> Unit)? = null) {
    var cards by remember { mutableStateOf(builtInFlashcards) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var knownCount by remember { mutableIntStateOf(0) }
    var unknownCount by remember { mutableIntStateOf(0) }
    var showAiGen by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories = cards.map { it.categoria }.distinct()
    val filteredCards = if (selectedCategory == null) cards
    else cards.filter { it.categoria == selectedCategory }

    val safeIndex = if (filteredCards.isEmpty()) 0 else currentIndex.coerceIn(0, filteredCards.size - 1)
    val card = if (filteredCards.isNotEmpty()) filteredCards[safeIndex] else null

    Column(modifier = Modifier.fillMaxSize()) {
        // Progress bar
        if (filteredCards.isNotEmpty()) {
            LinearProgressIndicator(
                progress = { (safeIndex + 1f) / filteredCards.size },
                modifier = Modifier.fillMaxWidth(),
                color = Primary
            )
        }

        // Header stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ArrowBack, "Voltar", modifier = Modifier.size(20.dp))
                }
            }
            Text(
                "${safeIndex + 1} / ${filteredCards.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    Text("$knownCount", style = MaterialTheme.typography.labelMedium, color = Color(0xFF4CAF50))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                    Text("$unknownCount", style = MaterialTheme.typography.labelMedium, color = Color(0xFFEF5350))
                }
            }
            IconButton(onClick = { showAiGen = true }) {
                Icon(Icons.Default.AutoAwesome, "Gerar com IA", tint = Primary)
            }
        }

        // Category chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null; currentIndex = 0; isFlipped = false },
                    label = { Text("Todos (${cards.size})") }
                )
            }
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat; currentIndex = 0; isFlipped = false },
                    label = { Text(cat) }
                )
            }
        }

        // Card display
        if (card == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum card disponível.")
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                FlipCard(card = card, isFlipped = isFlipped, onClick = { isFlipped = !isFlipped })
            }

            // Difficulty badge
            val diffColor = when (card.dificuldade) {
                "fácil" -> Color(0xFF4CAF50)
                "difícil" -> Color(0xFFEF5350)
                else -> Color(0xFFFFB300)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                AssistChip(
                    onClick = {},
                    label = { Text(card.categoria) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Primary.copy(0.1f))
                )
                Spacer(Modifier.width(8.dp))
                AssistChip(
                    onClick = {},
                    label = { Text(card.dificuldade) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = diffColor.copy(0.1f))
                )
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Não sei
                Button(
                    onClick = {
                        if (!isFlipped) { isFlipped = true; return@Button }
                        unknownCount++
                        isFlipped = false
                        if (safeIndex < filteredCards.size - 1) currentIndex++ else currentIndex = 0
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Não sei")
                }
                // Sei
                Button(
                    onClick = {
                        if (!isFlipped) { isFlipped = true; return@Button }
                        knownCount++
                        isFlipped = false
                        if (safeIndex < filteredCards.size - 1) currentIndex++ else currentIndex = 0
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sei!")
                }
            }

            // Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { if (safeIndex > 0) { currentIndex--; isFlipped = false } },
                    enabled = safeIndex > 0
                ) { Icon(Icons.Default.ArrowBack, null); Text("Anterior") }

                TextButton(onClick = { currentIndex = 0; knownCount = 0; unknownCount = 0; isFlipped = false }) {
                    Text("Reiniciar")
                }

                TextButton(
                    onClick = { if (safeIndex < filteredCards.size - 1) { currentIndex++; isFlipped = false } },
                    enabled = safeIndex < filteredCards.size - 1
                ) { Text("Próximo"); Icon(Icons.Default.ArrowForward, null) }
            }
        }
    }

    if (showAiGen) {
        AiFlashcardDialog(
            onDismiss = { showAiGen = false },
            onGenerated = { newCards ->
                cards = cards + newCards
                showAiGen = false
            }
        )
    }
}

@Composable
private fun FlipCard(card: Flashcard, isFlipped: Boolean, onClick: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "flip"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.4f)
            .graphicsLayer { rotationY = rotation; cameraDistance = 12f * density }
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (!isFlipped || rotation < 90f)
                    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF1B2F1A), Color(0xFF2D4E2C)))
                else
                    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF0D1F0C), Color(0xFF1A3019)))
            )
            .clickable(onClick = onClick)
            .graphicsLayer { if (rotation > 90f) rotationY = 180f },
        contentAlignment = Alignment.Center
    ) {
        Column(
            // A contra-rotação da face de trás já é feita uma vez no Box acima (linha
            // do segundo graphicsLayer). Repeti-la aqui somava 180°+180° = texto
            // espelhado ao virar — o bug que a médica via. Uma compensação, não duas.
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                if (!isFlipped || rotation < 90f) Icons.Default.Help else Icons.Default.Lightbulb,
                null, tint = Primary, modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (!isFlipped || rotation < 90f) card.frente else card.verso,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (!isFlipped || rotation < 90f) "Toque para revelar" else "Resposta ✓",
                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f))
            )
        }
    }
}

/**
 * Gera flashcards **a partir do acervo revisado** da biblioteca — não dos pesos de um
 * modelo. Cada card é frente = título do artigo revisado, verso = o resumo revisado.
 * Determinístico, offline e ancorado: se o tópico não casa com nenhum artigo, não há
 * card (o mesmo princípio do RAG — sem evidência, sem invenção). Nada de IA escrevendo
 * conteúdo clínico de estudo do nada (R4).
 */
@Composable
private fun AiFlashcardDialog(onDismiss: () -> Unit, onGenerated: (List<Flashcard>) -> Unit) {
    var topic by remember { mutableStateOf("") }

    val matches = remember(topic) {
        val q = topic.trim().lowercase()
        if (q.isBlank()) emptyList()
        else com.bioacupunt.biblioteca.data.MtcKnowledgeBase.articles.filter { a ->
            a.title.lowercase().contains(q) ||
                a.category.lowercase().contains(q) ||
                a.tags.any { it.lowercase().contains(q) } ||
                a.summary.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.School, null, tint = Primary) },
        title = { Text("Gerar do acervo revisado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Cria cards a partir dos artigos revisados da biblioteca. Sem IA inventando " +
                        "conteúdo — frente e verso vêm do acervo.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = topic, onValueChange = { topic = it },
                    label = { Text("Tópico (ex: Meridianos, Pulso, Ba Gang)") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.School, null) }
                )
                Text(
                    when {
                        topic.isBlank() -> "Digite um tópico para buscar no acervo."
                        matches.isEmpty() -> "Nenhum artigo revisado casa com \"$topic\"."
                        else -> "${matches.size} artigo(s) encontrado(s) — vira(m) ${matches.size} card(s)."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cards = matches.map { a ->
                        Flashcard(
                            frente = a.title,
                            verso = a.summary.ifBlank { a.content.take(240) },
                            categoria = runCatching {
                                com.bioacupunt.biblioteca.domain.model.MtcCategory.valueOf(a.category).label
                            }.getOrDefault(a.category),
                        )
                    }
                    onGenerated(cards)
                },
                enabled = matches.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) { Icon(Icons.Default.School, null); Spacer(Modifier.width(4.dp)); Text("Gerar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
