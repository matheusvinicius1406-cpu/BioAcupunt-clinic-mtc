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
import com.bioacupunt.ai.local.LocalModelCatalog
import com.bioacupunt.di.AppContainer
import kotlinx.coroutines.launch

/**
 * IA LOCAL (no aparelho) — baixar / apagar o modelo ativo do catálogo
 * ([LocalModelManager.MODEL_ID], hoje Phi-4 Mini Instruct — MIT, sem licença
 * restrita, contexto 4096 tokens).
 *
 * Quando o modelo está presente, o orquestrador prefere rodar no dispositivo
 * (fallbackOrder 0): de graça, offline e o dado clínico não sai do aparelho. Enquanto
 * ausente, o app cai para a nuvem (ou diz que a IA não está configurada). Este cartão
 * é a única forma de a médica trazer o modelo pra dentro.
 *
 * O arquivo NÃO cabe no APK — é baixado uma vez, da URL padrão embutida (Hugging
 * Face, sem token, confirmado com `curl -I` sem autenticação) ou de uma URL própria
 * configurada em Ajustes. O aviso de `!urlConfigured` abaixo é defensivo, para
 * qualquer modelo futuro gated que volte a ser considerado — hoje não deveria
 * aparecer, já que [LocalModelManager.DEFAULT_MODEL_URL] funciona sozinho.
 */
@Composable
fun LocalModelCard() {
    val scope = rememberCoroutineScope()
    val manager = remember { AppContainer.localModelManager }
    val securePrefs = remember { AppContainer.securePreferences }
    val state by manager.state.collectAsState(initial = LocalModelManager.State.Absent)
    val catalogModel = remember { LocalModelCatalog.byId(LocalModelManager.MODEL_ID) }
    val urlConfigured = securePrefs.localModelUrl.isNotBlank() || LocalModelManager.DEFAULT_MODEL_URL.isNotBlank()

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
                        "${catalogModel?.displayName ?: LocalModelManager.MODEL_ID}" +
                            (catalogModel?.sizeMb?.takeIf { it > 0 }?.let { " · ~%.1fGB".format(it / 1024f) } ?: "") +
                            " · roda offline, sem cota, dado clínico não sai do celular",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when (val s = state) {
                is LocalModelManager.State.Absent -> {
                    if (!urlConfigured) {
                        Text(
                            "Este modelo exige licença aceita no Hugging Face. Configure a URL do " +
                                "arquivo (hospedado por você) em Ajustes > IA antes de baixar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        Text("Modelo não baixado. A IA usa a nuvem até você baixá-lo.", style = MaterialTheme.typography.bodySmall)
                    }
                    DownloadButton(scope, manager, securePrefs, enabled = urlConfigured)
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
                        Text("Apagar modelo (liberar espaço)")
                    }
                }
                is LocalModelManager.State.Failed -> {
                    Text("Falha: ${s.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    DownloadButton(scope, manager, securePrefs, enabled = urlConfigured)
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
    enabled: Boolean = true,
) {
    Button(
        onClick = {
            scope.launch {
                val url = securePrefs.localModelUrl.ifBlank { LocalModelManager.DEFAULT_MODEL_URL }
                manager.download(url)
            }
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Baixar modelo (uma vez)")
    }
}
