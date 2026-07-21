package com.bioacupunt.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.bioacupunt.ui.theme.Accent
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.PrimaryDark
import com.bioacupunt.ui.theme.SemanticError
import com.bioacupunt.ui.theme.TextMuted
import kotlinx.coroutines.launch

/** LOGIN — cartão claro sobre creme, seguindo Canvas.dc.html. A aba "Criar conta"
 * cadastra de verdade contra POST /api/v1/auth/register: cria a clínica + usuária admin
 * e já entra. Ver AuthRepository.register. */
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    val isSignup = tab == 1
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var keepSignedIn by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val biometricAvailable = remember { AppContainer.isBiometricAvailable() }

    fun doLogin() {
        if (email.isBlank() || password.isBlank()) return
        focusManager.clearFocus()
        errorMsg = null
        loading = true
        scope.launch {
            val result = AppContainer.authRepository.login(email.trim(), password)
            loading = false
            result.onSuccess { onLoginSuccess() }
                .onFailure { errorMsg = it.localizedMessage ?: "Erro ao entrar. Tente novamente." }
        }
    }

    fun doRegister() {
        if (fullName.isBlank() || email.isBlank() || password.isBlank()) return
        focusManager.clearFocus()
        errorMsg = null
        loading = true
        scope.launch {
            val result = AppContainer.authRepository.register(email.trim(), password, fullName.trim())
            loading = false
            result.onSuccess { onLoginSuccess() }
                .onFailure { errorMsg = it.localizedMessage ?: "Não foi possível criar a conta." }
        }
    }

    fun submit() = if (isSignup) doRegister() else doLogin()

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
                buildString { append("Bio"); append("Acupunt") },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                "Clinical OS · centro de comando do terapeuta MTC",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.background)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                    .padding(4.dp),
            ) {
                listOf("Entrar", "Criar conta").forEachIndexed { i, label ->
                    val selected = tab == i
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (selected) Primary else Color.Transparent)
                            .clickable { tab = i; errorMsg = null }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(visible = isSignup) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("NOME PROFISSIONAL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = TextMuted)
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it; errorMsg = null },
                        placeholder = { Text("Dra. Camila Souza") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        shape = MaterialTheme.shapes.medium,
                    )
                }
            }
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("E-MAIL PROFISSIONAL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = TextMuted)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMsg = null },
                    placeholder = { Text("dra.camila@bioacupunt.com") },
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
            }
            Spacer(Modifier.height(10.dp))
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("SENHA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = TextMuted)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMsg = null },
                    placeholder = { Text("••••••••••") },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = Primary) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TextMuted)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    supportingText = if (isSignup) {
                        { Text("Mínimo de 8 caracteres.", style = MaterialTheme.typography.labelSmall, color = TextMuted) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
            }

            AnimatedVisibility(visible = errorMsg != null) {
                Text(errorMsg ?: "", color = SemanticError, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { keepSignedIn = !keepSignedIn }) {
                    Checkbox(checked = keepSignedIn, onCheckedChange = { keepSignedIn = it }, colors = CheckboxDefaults.colors(checkedColor = Primary))
                    Text("Manter conectado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Esqueceu a senha?", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = Primary)
            }

            Button(
                onClick = ::submit,
                enabled = email.isNotBlank() && password.isNotBlank() && (!isSignup || fullName.isNotBlank()) && !loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(if (isSignup) "Criar conta e entrar" else "Entrar na clínica", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text("ou acesse com", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.padding(horizontal = 10.dp))
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            loading = true
                            val result = AppContainer.authRepository.biometricLogin()
                            loading = false
                            result.onSuccess { onLoginSuccess() }
                                .onFailure { errorMsg = it.localizedMessage ?: "Biometria não reconhecida." }
                        }
                    },
                    enabled = biometricAvailable && !loading,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Default.Fingerprint, null, tint = if (biometricAvailable) Primary else TextMuted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Biometria", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                }
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Default.Badge, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Google Drive", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                }
            }
            Spacer(Modifier.height(20.dp))

            Box(modifier = Modifier.clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text("✓ Selo de Excelência Clínica", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = Primary)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "v${com.bioacupunt.BuildConfig.VERSION_NAME} · Sincronizado com Supabase",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
    }
}
