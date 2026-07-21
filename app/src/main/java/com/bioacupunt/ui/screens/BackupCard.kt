package com.bioacupunt.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bioacupunt.backup.AppRestarter
import com.bioacupunt.backup.BackupManager
import com.bioacupunt.di.AppContainer
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.launch

/**
 * Backup e restauração no Google Drive da própria médica.
 *
 * Fluxo real: ela loga na conta Google (escopo mínimo `drive.file`), o app empacota o
 * banco e sobe para o Drive dela; restaurar baixa o backup mais recente e reinicia o app.
 * Autocontido de propósito — o launcher do Google Sign-In vive dentro deste Composable,
 * sem exigir mudança de assinatura da AjustesScreen.
 *
 * PRÉ-REQUISITO (só a dona do projeto Google Cloud faz): cliente OAuth Android com o
 * applicationId + SHA-1 da chave de assinatura, API do Drive ativada. Sem isso o login
 * falha com DEVELOPER_ERROR (código 10) — ver docs/backup-google-drive.md.
 */
@Composable
fun BackupCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drive = remember { AppContainer.googleDriveClient }
    val backup = remember { AppContainer.backupManager }

    var account by remember { mutableStateOf<GoogleSignInAccount?>(drive.lastAccount()) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    fun report(message: String, error: Boolean = false) {
        status = message
        isError = error
    }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        drive.accountFromResult(result.data)
            .onSuccess { account = it; report("Conectada como ${it.email}.") }
            .onFailure { report(it.localizedMessage ?: "Não foi possível conectar ao Google.", error = true) }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudUpload, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Backup no Google Drive", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        account?.email?.let { "Conta: $it" } ?: "Nenhuma conta conectada",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (account == null) {
                Button(
                    onClick = { report(""); signInLauncher.launch(drive.signInIntent()) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Login, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Entrar com Google")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val acc = account ?: return@Button
                            busy = true; report("Fazendo backup…")
                            scope.launch {
                                backup.createBackupBytes()
                                    .mapCatching { bytes ->
                                        drive.uploadBackup(acc, BackupManager.suggestedFileName(), bytes).getOrThrow()
                                    }
                                    .onSuccess { report("Backup enviado ao Drive com sucesso.") }
                                    .onFailure { report(it.localizedMessage ?: "Falha no backup.", error = true) }
                                busy = false
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Backup, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Backup")
                    }
                    OutlinedButton(
                        onClick = {
                            val acc = account ?: return@OutlinedButton
                            busy = true; report("Restaurando o backup mais recente…")
                            scope.launch {
                                drive.listBackups(acc)
                                    .mapCatching { files ->
                                        val latest = files.firstOrNull() ?: error("Nenhum backup encontrado no Drive.")
                                        val bytes = drive.downloadFile(acc, latest.id).getOrThrow()
                                        backup.restoreBackupBytes(bytes).getOrThrow()
                                    }
                                    .onSuccess {
                                        report("Backup restaurado. Reiniciando…")
                                        AppRestarter.restart(context)
                                    }
                                    .onFailure { report(it.localizedMessage ?: "Falha ao restaurar.", error = true) }
                                busy = false
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Restore, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Restaurar")
                    }
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            drive.signOut()
                            account = null
                            report("Desconectada do Google.")
                        }
                    },
                    enabled = !busy,
                ) { Text("Desconectar conta Google") }
            }

            if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            status?.takeIf { it.isNotBlank() }?.let { msg ->
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
