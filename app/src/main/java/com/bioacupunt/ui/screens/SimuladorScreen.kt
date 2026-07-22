package com.bioacupunt.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.bioacupunt.ui.theme.Primary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val category: String
)

private val quizBank = listOf(
    QuizQuestion(
        "Qual é a função PRINCIPAL do Fígado em MTC?",
        listOf("Armazenar o Sangue", "Promover o livre fluxo do Qi", "Governar a Água", "Controlar o Qi de defesa"),
        1,
        "A função mais importante do Fígado (Gan) é 'Shu Xie' — promover o livre fluxo do Qi por todo o corpo. Quando esta função falha, ocorre Estagnação de Qi do Fígado.",
        "Órgãos"
    ),
    QuizQuestion(
        "O ponto E36 (Zusanli) está localizado a:",
        listOf("2 cun abaixo do joelho, lado medial", "4 cun abaixo da patela, 1 cun lateral à crista da tíbia", "3 cun acima do tornozelo, lado medial", "No meio do músculo gastrocnêmio"),
        1,
        "Zusanli (E36) fica 4 cun abaixo da borda inferior da patela e 1 cun lateral à crista anterior da tíbia. É o ponto He-Mar do meridiano do Estômago e um dos mais poderosos para tonificação geral.",
        "Pontos"
    ),
    QuizQuestion(
        "No Ciclo Sheng (Geração), qual elemento NUTRE o Fogo?",
        listOf("Terra", "Metal", "Madeira", "Água"),
        2,
        "No ciclo de geração (Sheng): Madeira → Fogo → Terra → Metal → Água → Madeira. A Madeira alimenta o Fogo, assim como a lenha alimenta as chamas.",
        "Cinco Elementos"
    ),
    QuizQuestion(
        "Uma língua VERMELHA sem saburra indica:",
        listOf("Frio interior com Umidade", "Deficiência de Yin com calor vazio", "Calor pleno com Fleuma", "Estagnação de Sangue"),
        1,
        "Língua vermelha (excesso de calor) sem saburra (fluidos/Yin insuficientes para produzir saburra) aponta para Deficiência de Yin com calor vazio — comum na deficiência de Yin do Rim ou Fígado.",
        "Semiologia"
    ),
    QuizQuestion(
        "O De Qi é descrito como:",
        listOf("Dor aguda durante o agulhamento", "Sensação de peso, distensão ou formigamento ao redor do ponto", "Sangramento leve após retirada da agulha", "Contratura muscular involuntária"),
        1,
        "De Qi (得气) — a chegada do Qi — é descrito como sensação de peso, distensão, dormência, formigamento ou propagação ao longo do meridiano. Indica que o ponto foi ativado corretamente.",
        "Técnicas"
    ),
    QuizQuestion(
        "Qual órgão 'governa a Água' em MTC?",
        listOf("Baço-Pâncreas", "Pulmão", "Fígado", "Rim"),
        3,
        "O Rim (Shen) governa a Água — responsável pelo metabolismo dos fluidos, filtração, formação de urina e equilíbrio hídrico geral. Também armazena a Essência (Jing).",
        "Órgãos"
    ),
    QuizQuestion(
        "O pulso Xian (弦 — como corda de arco) indica:",
        listOf("Deficiência de Qi e Sangue", "Estagnação de Qi do Fígado, dor ou Fleuma", "Calor intenso nos órgãos", "Deficiência de Yang do Rim"),
        1,
        "O pulso Xian é longo, firme e tenso como uma corda de arco. Está associado a Estagnação de Qi do Fígado, dor (em qualquer localização) e Fleuma. Em grávidas, pode ser sinal de saúde.",
        "Pulso"
    ),
    QuizQuestion(
        "Qual é o ponto Mu (alarme frontal) do Fígado?",
        listOf("F13 — Zhangmen", "F14 — Qimen", "B18 — Ganshu", "VB24 — Riyue"),
        1,
        "F14 — Qimen é o ponto Mu do Fígado, localizado no 6° espaço intercostal, na linha medioclavicular. Usado para tratar diretamente o Fígado, especialmente dor no hipocôndrio e estagnação.",
        "Pontos Especiais"
    ),
    QuizQuestion(
        "Na teoria Ba Gang, qual par identifica a profundidade da doença?",
        listOf("Frio / Calor", "Interior / Exterior", "Deficiência / Excesso", "Yin / Yang"),
        1,
        "O par Interior (里 Lǐ) / Exterior (表 Biǎo) identifica a profundidade da doença. Exterior: afeta pele e meridianos (agudo). Interior: afeta órgãos Zang-Fu (crônico ou mais grave).",
        "Ba Gang"
    ),
    QuizQuestion(
        "A emoção associada ao elemento Metal é:",
        listOf("Raiva", "Alegria excessiva", "Tristeza / Pesar", "Medo"),
        2,
        "O elemento Metal corresponde ao Pulmão e Intestino Grosso. A emoção associada é a Tristeza (Bei) e o Pesar (You). Tristeza excessiva consume o Qi do Pulmão.",
        "Cinco Elementos"
    )
)

@Composable
fun SimuladorScreen() {
    var mode by remember { mutableStateOf<SimMode>(SimMode.Menu) }

    when (val m = mode) {
        SimMode.Menu       -> SimuladorMenu(
            onStartQuiz  = { mode = SimMode.Quiz(0, 0, 0, null, false) },
            onStartCase  = { mode = SimMode.ClinicalCase }
        )
        is SimMode.Quiz    -> QuizMode(
            state     = m,
            onUpdate  = { mode = it },
            onBack    = { mode = SimMode.Menu }
        )
        SimMode.ClinicalCase -> ClinicalCaseMode(onBack = { mode = SimMode.Menu })
    }
}

private sealed class SimMode {
    data object Menu : SimMode()
    data class Quiz(
        val index: Int,
        val correct: Int,
        val total: Int,
        val selectedAnswer: Int?,
        val showExplanation: Boolean
    ) : SimMode()
    data object ClinicalCase : SimMode()
}

@Composable
private fun SimuladorMenu(onStartQuiz: () -> Unit, onStartCase: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Simulador MTC 🧪", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Text(
            "Teste seus conhecimentos com quiz interativo e casos clínicos de MTC.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        SimModeCard(
            icon = Icons.Default.Quiz,
            title = "Quiz de Conhecimentos",
            description = "${quizBank.size} perguntas sobre pontos, órgãos, cinco elementos, Ba Gang, semiologia e técnicas.",
            color = Primary,
            onClick = onStartQuiz
        )

        SimModeCard(
            icon = Icons.Default.Cases,
            title = "Casos Clínicos",
            description = "Analise casos clínicos reais e pratique o diagnóstico diferencial em MTC.",
            color = Color(0xFF64B5F6),
            onClick = onStartCase
        )

        SimModeCard(
            icon = Icons.Default.AutoAwesome,
            title = "Caso Gerado por IA",
            description = "A IA cria um caso clínico personalizado para você praticar. Requer o modelo local (Ajustes > IA).",
            color = Color(0xFF9575CD),
            onClick = {}
        )
    }
}

@Composable
private fun SimModeCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String, color: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(3.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = color, modifier = Modifier.size(28.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = color, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun QuizMode(state: SimMode.Quiz, onUpdate: (SimMode.Quiz) -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()

    if (state.index >= quizBank.size) {
        // Results screen
        QuizResults(correct = state.correct, total = quizBank.size, onBack = onBack, onRetry = {
            onUpdate(SimMode.Quiz(0, 0, 0, null, false))
        })
        return
    }

    val question = quizBank[state.index]
    val progress = (state.index.toFloat() / quizBank.size)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Progress
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Column(Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = Primary
                )
                Text(
                    "${state.index + 1} / ${quizBank.size}  ·  ✅ ${state.correct} corretas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Category badge
        SuggestionChip(onClick = {}, label = { Text(question.category) })

        Spacer(Modifier.height(10.dp))

        // Question
        Text(question.question, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))

        Spacer(Modifier.height(16.dp))

        // Options
        question.options.forEachIndexed { i, option ->
            val isSelected = state.selectedAnswer == i
            val isCorrect  = i == question.correctIndex
            val bgColor = when {
                !state.showExplanation       -> if (isSelected) Primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                isCorrect                    -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                isSelected && !isCorrect     -> Color(0xFFEF5350).copy(alpha = 0.15f)
                else                         -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
            val borderColor = when {
                !state.showExplanation       -> if (isSelected) Primary.copy(alpha = 0.6f) else Color.Transparent
                isCorrect                    -> Color(0xFF4CAF50)
                isSelected && !isCorrect     -> Color(0xFFEF5350)
                else                         -> Color.Transparent
            }

            Card(
                onClick = {
                    if (state.selectedAnswer == null) {
                        onUpdate(state.copy(selectedAnswer = i, showExplanation = true))
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                border = BorderStroke(1.dp, borderColor),
                enabled = state.selectedAnswer == null
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        ('A' + i).toString(),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Primary),
                        modifier = Modifier.width(24.dp)
                    )
                    Text(option, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    if (state.showExplanation) {
                        when {
                            isCorrect -> Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                            isSelected -> Icon(Icons.Default.Cancel, null, tint = Color(0xFFEF5350), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // Explanation
        AnimatedVisibility(visible = state.showExplanation) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.06f)),
                border = BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Lightbulb, null, tint = Primary, modifier = Modifier.size(16.dp))
                        Text("Explicação", style = MaterialTheme.typography.labelMedium.copy(color = Primary, fontWeight = FontWeight.Bold))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(question.explanation, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Next button
        if (state.showExplanation) {
            Button(
                onClick = {
                    val newCorrect = if (state.selectedAnswer == question.correctIndex) state.correct + 1 else state.correct
                    onUpdate(SimMode.Quiz(state.index + 1, newCorrect, state.total + 1, null, false))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(if (state.index < quizBank.size - 1) "Próxima questão →" else "Ver resultado")
            }
        }
    }
}

@Composable
private fun QuizResults(correct: Int, total: Int, onBack: () -> Unit, onRetry: () -> Unit) {
    val pct = (correct.toFloat() / total * 100).toInt()
    val (emoji, msg) = when {
        pct >= 90 -> "🏆" to "Excelente! Você domina MTC!"
        pct >= 70 -> "⭐" to "Muito bem! Continue estudando!"
        pct >= 50 -> "📚" to "Bom progresso! Revise os tópicos."
        else      -> "💪" to "Continue praticando! Use os flashcards."
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text("$correct / $total corretas", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Primary))
        Text("$pct% de aproveitamento", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text(msg, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
            Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Tentar novamente")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Voltar ao menu")
        }
    }
}

@Composable
private fun ClinicalCaseMode(onBack: () -> Unit) {
    val case = remember {
        """
**Caso Clínico: Paciente F.S., 38 anos**

**Queixa Principal:**
Dor no hipocôndrio direito, distensão abdominal, irritabilidade e suspiros frequentes há 3 meses.

**História:**
Trabalho estressante, conflitos familiares recentes. Ciclos menstruais irregulares com cólicas e coágulos. Insônia com dificuldade para adormecer.

**Semiologia:**
• Língua: Levemente roxa nas bordas, saburra fina branca
• Pulso: Xian (tenso como corda) em guan esquerdo
• Face: Tez levemente acinzentada

**Pergunta 1:** Qual o padrão de desarmonia mais provável?
**Pergunta 2:** Quais são os princípios de tratamento?
**Pergunta 3:** Sugira 5 pontos com justificativa.
        """.trimIndent()
    }

    var showAnswer by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                Text("Caso Clínico", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Text(case, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Button(
                onClick = { showAnswer = !showAnswer },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) { Text(if (showAnswer) "Ocultar resposta" else "Ver resposta comentada") }
        }
        if (showAnswer) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.06f)),
                    border = BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🎯 Resposta Comentada", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Primary))
                        Spacer(Modifier.height(8.dp))
                        Text("""
**Padrão:** Estagnação de Qi do Fígado (肝气郁结)

**Justificativa:**
• Dor no hipocôndrio → região do Fígado/VB
• Irritabilidade e suspiros → Qi do Fígado estagnado
• Pulso Xian no Guan esquerdo → Fígado comprometido
• Língua levemente roxa → início de estagnação de Sangue
• Menstruação irregular com coágulos → Qi estagnado afeta Sangue

**Princípios de Tratamento:**
1. Mover o Qi do Fígado (疏肝理气)
2. Acalmar o Shen
3. Regular o Sangue (se sangue estagnado)

**Pontos Sugeridos:**
• F3 (Taichong) — Yuan do Fígado, principal ponto para mover Qi
• F14 (Qimen) — Mu do Fígado, desobstrui o Qi do Fígado
• VB34 (Yanglingquan) — He da VB, alivia hipocôndrio
• PC6 (Neiguan) — Acalma o Shen, alivia distensão
• SP6 (Sanyinjiao) — Move Qi e Sangue, regula menstruação
                        """.trimIndent(), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
