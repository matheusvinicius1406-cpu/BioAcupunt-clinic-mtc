package com.bioacupunt.ui.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.bioacupunt.di.AppContainer
import com.bioacupunt.security.LocalPinAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Gate de entrada OFFLINE. Dois modos, decididos pelo estado local:
 *  - **Setup** (primeiro uso, sem PIN): a médica cria um PIN e, opcionalmente, liga a
 *    biometria.
 *  - **Desbloqueio**: PIN ou biometria. Sem internet, sem backend.
 *
 * Nunca há um caminho "entrar sem nada": mesmo o botão de biometria só desbloqueia via
 * [BiometricPrompt] de verdade. Se a biometria falhar, o PIN continua disponível — não
 * trava a médica do lado de fora do próprio consultório.
 */
@Composable
fun PinLockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { AppContainer.localAuthManager }
    val isSetup = remember { !auth.hasPin() }

    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var enableBiometric by remember { mutableStateOf(auth.biometricEnabled) }
    var error by remember { mutableStateOf<String?>(null) }
    // O PBKDF2 (120k iterações, de propósito) não pode rodar na thread de UI: é o gate
    // de abertura do app e travaria/ANR num aparelho fraco. Roda em Default.
    var verifying by remember { mutableStateOf(false) }

    val biometricAvailable = remember {
        runCatching {
            BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
        }.getOrDefault(false)
    }

    fun promptBiometric() {
        val activity = context as? FragmentActivity ?: run { error = "Biometria indisponível neste aparelho"; return }
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { onUnlocked() }
            override fun onAuthenticationError(code: Int, msg: CharSequence) {
                if (code != BiometricPrompt.ERROR_NEGATIVE_BUTTON && code != BiometricPrompt.ERROR_USER_CANCELED) {
                    error = msg.toString()
                }
            }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Desbloquear BioAcupunt")
                .setSubtitle("Use a biometria cadastrada")
                .setNegativeButtonText("Usar PIN")
                .build()
        )
    }

    // No desbloqueio com biometria ligada, oferece o prompt automaticamente.
    LaunchedEffect(Unit) {
        if (!isSetup && auth.biometricEnabled && biometricAvailable) promptBiometric()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0B160B)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF112211)),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier.size(60.dp).clip(CircleShape).background(Color(0xFF87B344).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (isSetup) Icons.Default.Lock else Icons.Default.LockOpen, null, tint = Color(0xFF87B344), modifier = Modifier.size(30.dp))
                }

                Text(
                    if (isSetup) "Criar PIN de acesso" else "Desbloquear",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                )
                Text(
                    if (isSetup) "Defina um PIN de ao menos ${LocalPinAuth.MIN_PIN_LENGTH} dígitos. Ele fica só no aparelho, como hash."
                    else "Digite seu PIN.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f)),
                    textAlign = TextAlign.Center,
                )

                PinField(pin, { pin = it.filter { c -> c.isDigit() }; error = null }, if (isSetup) "PIN" else "Seu PIN")

                if (isSetup) {
                    PinField(confirm, { confirm = it.filter { c -> c.isDigit() }; error = null }, "Confirmar PIN")
                    if (biometricAvailable) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Fingerprint, null, tint = Color(0xFF87B344), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Também desbloquear com biometria", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Switch(checked = enableBiometric, onCheckedChange = { enableBiometric = it })
                        }
                    }
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center) }

                Button(
                    onClick = {
                        if (verifying) return@Button
                        if (isSetup) {
                            when {
                                !LocalPinAuth.isValidPin(pin) -> error = "PIN inválido (mín. ${LocalPinAuth.MIN_PIN_LENGTH} dígitos)."
                                pin != confirm -> error = "Os PINs não conferem."
                                else -> {
                                    verifying = true; error = null
                                    scope.launch {
                                        withContext(Dispatchers.Default) { auth.setPin(pin) }
                                        auth.biometricEnabled = enableBiometric && biometricAvailable
                                        verifying = false
                                        onUnlocked()
                                    }
                                }
                            }
                        } else {
                            verifying = true; error = null
                            scope.launch {
                                val ok = withContext(Dispatchers.Default) { auth.verifyPin(pin) }
                                verifying = false
                                if (ok) onUnlocked() else error = "PIN incorreto."
                            }
                        }
                    },
                    enabled = !verifying,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF87B344)),
                ) { Text(if (verifying) "Verificando…" else if (isSetup) "Definir PIN e entrar" else "Entrar") }

                if (!isSetup && auth.biometricEnabled && biometricAvailable) {
                    TextButton(onClick = { promptBiometric() }) {
                        Icon(Icons.Default.Fingerprint, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Usar biometria", color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PinField(value: String, onValue: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 12) onValue(it) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF87B344),
            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
            focusedLabelColor = Color(0xFF87B344),
            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
        ),
    )
}
