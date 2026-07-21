package com.bioacupunt.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bioacupunt.ai.data.provider.LocalModelManager
import com.bioacupunt.di.AppContainer
import kotlinx.coroutines.launch

/**
 * IA LOCAL (no aparelho) — baixar / apagar o modelo Gemma.
 *
 * Quando o modelo está presente, o orquestrador prefere rodar no dispositivo
 * (fallbackOrder 0): de graça, offline e o dado clínico não sai do aparelho. Enquanto
 * ausente, o app cai para a nuvem (ou diz que a IA não está configurada). Este cartão
 * é a única forma de a médica trazer o modelo pra dentro.
 *
 * O arquivo (~1GB) NÃO cabe no APK — é baixado uma vez da URL configurada em Ajustes
 * (ou sideloadado via adb para testes). Gemma é license-bound: hospede você mesmo o
 * `.task` depois de aceitar a licença.
 */
@Composable
fun LocalModelCard() {
    val scope = rememberCoroutineScope()
    val manager = remember { AppContainer.localModelManager }
    val securePrefs = remember { AppContainer.securePreferences }
    val state by manager.state.collectAsState(initial = LocalModelManager.State.Absent)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("IA local no aparelho", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "Gemma 3 1B · ~1GB · roda offline, sem cota, dado clínico não sai do celular",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when (val s = state) {
                is LocalModelManager.State.Absent -> {
                    Text("Modelo não baixado. A IA usa a nuvem até você baixá-lo.", style = MaterialTheme.typography.bodySmall)
                    DownloadButton(scope, manager, securePrefs)
                }
                is LocalModelManager.State.Downloading -> {
                    val pct = (s.progress * 100).toInt().coerceIn(0, 100)
                    Text("Baixando modelo… $pct%", style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(
                        progress = { s.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is LocalModelManager.State.Ready -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Modelo pronto — IA roda no aparelho.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    OutlinedButton(
                        onClick = { scope.launch { manager.delete() } },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Apagar modelo (liberar ~1GB)")
                    }
                }
                is LocalModelManager.State.Failed -> {
                    Text("Falha: ${s.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    DownloadButton(scope, manager, securePrefs)
                }
            }
        }
    }
}

@Composable
private fun DownloadButton(
    scope: kotlinx.coroutines.CoroutineScope,
    manager: LocalModelManager,
    securePrefs: com.bioacupunt.security.SecurePreferences,
) {
    Button(
        onClick = {
            scope.launch {
                val url = securePrefs.localModelUrl.ifBlank { LocalModelManager.DEFAULT_MODEL_URL }
                manager.download(url)
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Baixar modelo (~1GB, uma vez)")
    }
}
