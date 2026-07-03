package com.bioacupunt.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bioacupunt.di.AppContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BiometricLockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Toque para autenticar") }
    var error by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var hasPrompted by remember { mutableStateOf(false) }

    val executor = remember { androidx.core.os.HandlerCompat.createAsync(context.mainLooper) }
    val activity = context as? androidx.fragment.app.FragmentActivity
    val prompt = remember(activity, executor) {
        activity?.let {
            androidx.biometric.BiometricPrompt(it, executor, object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == android.hardware.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == android.hardware.biometric.BiometricPrompt.ERROR_USER_CANCELED) {
                        scope.launch { status = "Autenticação cancelada" }
                    } else {
                        scope.launch { error = errString.toString(); status = "Falha na autenticação" }
                    }
                    isProcessing = false
                }

                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    scope.launch { onUnlocked() }
                }

                override fun onAuthenticationFailed() {
                    scope.launch { status = "Não reconhecido" }
                }
            })
        }
    }

    LaunchedEffect(Unit) {
        if (activity != null && prompt != null && !hasPrompted) {
            hasPrompted = true
            status = "Aguardando leitura biométrica"
            isProcessing = true
            prompt.authenticate(
                androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Desbloquear BioAcupunt")
                    .setSubtitle("Autenticação por impressão digital ou credencial do dispositivo")
                    .setNegativeButtonText("Sair")
                    .build()
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF112211)),
            shape = MaterialTheme.shapes.large
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF87B344).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = Color(0xFF87B344), modifier = Modifier.size(32.dp))
                }

                Text("Desbloqueio do app", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
                Text(status, style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f)), textAlign = TextAlign.Center)
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedButton(onClick = {
                    if (isProcessing.not() && activity != null && prompt != null) {
                        status = "Autenticando..."
                        error = null
                        isProcessing = true
                        prompt.authenticate(
                            androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                                .setTitle("Desbloquear BioAcupunt")
                                .setSubtitle("Use a impressão digital cadastrada")
                                .setNegativeButtonText("Cancelar")
                                .build()
                        )
                    }
                }, enabled = activity != null && prompt != null && isProcessing.not()) {
                    Text(if (activity == null) "Biometria indisponível" else "Autenticar")
                }

                TextButton(onClick = { onUnlocked() }) { Text("Acessar sem biometria", color = Color.White.copy(alpha = 0.75f)) }
            }
        }
    }
}
