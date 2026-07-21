package com.bioacupunt.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bioacupunt.di.AppContainer
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.PrimaryDark
import com.bioacupunt.ui.theme.SemanticError
import com.bioacupunt.ui.theme.TextMuted
import kotlinx.coroutines.launch

/**
 * LOGIN 100% LOCAL — sem servidor, sem Render, funciona offline.
 *
 * Primeiro uso: a médica cria a conta local (nome + PIN). O PIN vive só como hash
 * PBKDF2 (ver LocalPinAuth), nunca em texto puro. Depois, o app trava a cada abertura
 * e ela destrava com PIN ou biometria. Nada disso depende de internet.
 */
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val localAuth = remember { AppContainer.localAuthManager }
    val throttle = remember { AppContainer.authThrottle }
    val securePrefs = remember { AppContainer.securePreferences }

    val isFirstRun = remember { !localAuth.hasPin() }
    var fullName by remember { mutableStateOf(securePrefs.professionalName) }
    var pin by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }
    var enableBiometric by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val biometricAvailable = remember { AppContainer.isBiometricAvailable() }
    val biometricEnabled = remember { securePrefs.biometricEnabled }

    // ── Biometria (destrava sem digitar o PIN) ─────────────────────────────
    val activity = context as? androidx.fragment.app.FragmentActivity
    val executor = remember { androidx.core.content.ContextCompat.getMainExecutor(context) }
    val biometricPrompt = remember(activity, executor) {
        activity?.let {
            androidx.biometric.BiometricPrompt(it, executor,
                object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                        securePrefs.isLoggedIn = true
                        throttle.recordSuccess()
                        onLoginSuccess()
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                            errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED
                        ) errorMsg = errString.toString()
                    }
                })
        }
    }

    fun promptBiometric() {
        val p = biometricPrompt ?: return
        errorMsg = null
        p.authenticate(
            androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle("Desbloquear BioAcupunt")
                .setSubtitle("Use a biometria cadastrada no aparelho")
                .setNegativeButtonText("Usar PIN")
                .build()
        )
    }

    fun doCreate() {
        errorMsg = null
        if (fullName.isBlank()) { errorMsg = "Informe seu nome."; return }
        if (!com.bioacupunt.security.LocalPinAuth.isValidPin(pin)) {
            errorMsg = "O PIN deve ter ao menos 4 dígitos."; return
        }
        if (pin != pinConfirm) { errorMsg = "Os PINs não coincidem."; return }
        focusManager.clearFocus()
        loading = true
        if (!localAuth.setPin(pin)) { loading = false; errorMsg = "PIN inválido."; return }
        securePrefs.professionalName = fullName.trim()
        securePrefs.biometricEnabled = enableBiometric && biometricAvailable
        securePrefs.hasOnboarded = true
        securePrefs.isLoggedIn = true
        loading = false
        onLoginSuccess()
    }

    fun doUnlock() {
        errorMsg = null
        if (!throttle.blockOrAllow()) {
            errorMsg = throttle.status.value.message ?: "Muitas tentativas. Aguarde."
            return
        }
        focusManager.clearFocus()
        loading = true
        scope.launch {
            val ok = localAuth.verifyPin(pin)
            loading = false
            if (ok) {
                securePrefs.isLoggedIn = true
                throttle.recordSuccess()
                onLoginSuccess()
            } else {
                pin = ""
                val status = throttle.recordFailure()
                errorMsg = status.message ?: "PIN incorreto."
            }
        }
    }

    // Ao abrir a tela de destravar com biometria já habilitada, oferece o prompt na hora.
    LaunchedEffect(Unit) {
        if (!isFirstRun && biometricEnabled && biometricAvailable) promptBiometric()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraLarge)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(Brush.linearGradient(listOf(Primary, PrimaryDark))),
                contentAlignment = Alignment.Center,
            ) {
                Text("☯", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "BioAcupunt",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                if (isFirstRun) "Crie sua conta — funciona offline, sem servidor"
                else "Bem-vinda de volta${securePrefs.professionalName.takeIf { it.isNotBlank() }?.let { ", ${it.substringBefore(' ')}" } ?: ""}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            if (isFirstRun) {
                LabeledField("NOME PROFISSIONAL") {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it; errorMsg = null },
                        placeholder = { Text("Dra. Camila Souza") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = TextMuted) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            LabeledField(if (isFirstRun) "CRIE UM PIN (mín. 4 dígitos)" else "PIN") {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { new -> pin = new.filter { it.isDigit() }; errorMsg = null },
                    placeholder = { Text("••••") },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = Primary) },
                    trailingIcon = {
                        IconButton(onClick = { pinVisible = !pinVisible }) {
                            Icon(if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TextMuted)
                        }
                    },
                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = if (isFirstRun) ImeAction.Next else ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        onDone = { doUnlock() },
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
            }

            if (isFirstRun) {
                Spacer(Modifier.height(10.dp))
                LabeledField("CONFIRME O PIN") {
                    OutlinedTextField(
                        value = pinConfirm,
                        onValueChange = { new -> pinConfirm = new.filter { it.isDigit() }; errorMsg = null },
                        placeholder = { Text("••••") },
                        leadingIcon = { Icon(Icons.Default.LockReset, null, tint = Primary) },
                        visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { doCreate() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                }
                if (biometricAvailable) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = enableBiometric, onCheckedChange = { enableBiometric = it }, colors = CheckboxDefaults.colors(checkedColor = Primary))
                        Text("Ativar biometria para destravar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            AnimatedVisibility(visible = errorMsg != null) {
                Text(errorMsg ?: "", color = SemanticError, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { if (isFirstRun) doCreate() else doUnlock() },
                enabled = !loading && pin.isNotBlank() && (!isFirstRun || (fullName.isNotBlank() && pinConfirm.isNotBlank())),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(if (isFirstRun) "Criar conta e entrar" else "Entrar", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }

            if (!isFirstRun && biometricEnabled && biometricAvailable) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { promptBiometric() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Default.Fingerprint, null, tint = Primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Entrar com biometria")
                }
            }

            Spacer(Modifier.height(20.dp))
            Box(modifier = Modifier.clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text("🔒 Dados criptografados no aparelho", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = Primary)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "v${com.bioacupunt.BuildConfig.VERSION_NAME} · offline-first",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun LabeledField(label: String, field: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = TextMuted)
        field()
    }
}
