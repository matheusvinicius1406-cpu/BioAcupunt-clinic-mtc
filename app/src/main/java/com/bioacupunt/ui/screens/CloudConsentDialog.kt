package com.bioacupunt.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.statusColors

/**
 * Consentimento de nuvem do primeiro acesso — R2/LGPD.
 *
 * Mostrado uma única vez (ver [com.bioacupunt.security.SecurePreferences.cloudConsentAsked])
 * antes de a médica poder usar QUALQUER caminho de IA que envolva a nuvem. Não pode ser
 * dispensado tocando fora ou no botão voltar (`onDismissRequest = {}`) — consentimento
 * precisa de uma escolha explícita, não de um diálogo acidentalmente ignorado.
 *
 * A resposta (aceitar ou recusar) grava tanto [com.bioacupunt.security.SecurePreferences.cloudConsentAsked]
 * quanto [com.bioacupunt.ai.config.AiConfigManager.setCloudEnabled] — recusar não deixa a
 * pergunta reaparecendo a cada abertura do app, e continua com a IA local (Gemma
 * on-device), sem nenhum dado saindo do aparelho.
 */
@Composable
fun CloudConsentDialog(onAnswered: (acceptCloud: Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = { /* resposta obrigatória — sem dispensa acidental */ },
        icon = { Icon(Icons.Default.CloudQueue, null, tint = Primary) },
        title = { Text("Usar IA na nuvem (Gemini)?") },
        text = {
            Column {
                Text(
                    "O BioAcupunt já vem com uma IA local (Gemma), que roda 100% no aparelho. " +
                        "Você pode também ativar um complemento na nuvem (Google Gemini) para quando " +
                        "não houver modelo local disponível, e para o assistente livre buscar na web.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.CloudOff,
                        null,
                        tint = statusColors().warning,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Ao ativar, o texto das suas conversas com o assistente pode ser enviado a um " +
                            "servidor externo (Google) — deixa de ser 100% offline. Isso inclui dados " +
                            "administrativos como nome e horário de pacientes quando aparecem no contexto " +
                            "da conversa. Não é usado no caminho clínico/diagnóstico, que continua sempre " +
                            "determinístico e sem IA. Você pode mudar essa escolha a qualquer momento em " +
                            "Ajustes > IA.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAnswered(true) },
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) { Text("Ativar IA na nuvem") }
        },
        dismissButton = {
            OutlinedButton(onClick = { onAnswered(false) }) { Text("Manter só local (offline)") }
        },
    )
}
