package com.bioacupunt.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.bioacupunt.di.AppContainer
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.PrimaryDark
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showGDriveInfo by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val gradientAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000), RepeatMode.Reverse),
        label = "gradient"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1B2F1A),
                        Color(0xFF2D4E2C),
                        Color(0xFF1B2F1A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo / Icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.15f))
                    .border(2.dp, Primary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalHospital,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "BioAcupunt",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Text(
                "Gestão Clínica MTC",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f))
            )

            Spacer(Modifier.height(8.dp))

            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMsg = null },
                label = { Text("E-mail") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedTextFieldColors()
            )

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMsg = null },
                label = { Text("Senha") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Mostrar senha"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    isLoading = true
                    errorMsg = null
                    scope.launch {
                        val result = AppContainer.authRepository.login(email.trim(), password)
                        isLoading = false
                        result.onSuccess { onLoginSuccess() }
                            .onFailure { errorMsg = it.localizedMessage ?: "Erro ao entrar. Tente novamente." }
                    }
                }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedTextFieldColors()
            )

            // Error message
            AnimatedVisibility(visible = errorMsg != null) {
                Text(
                    text = errorMsg ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            // Login button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    isLoading = true
                    errorMsg = null
                    scope.launch {
                        val result = AppContainer.authRepository.login(email.trim(), password)
                        isLoading = false
                        result.onSuccess { onLoginSuccess() }
                            .onFailure { errorMsg = it.localizedMessage ?: "Erro ao entrar. Tente novamente." }
                    }
                },
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Login, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Entrar", fontWeight = FontWeight.Bold)
                }
            }

            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color.White.copy(alpha = 0.2f)
            )

            // Google Drive connect section
            Text(
                "Integrações",
                style = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(alpha = 0.6f))
            )

            OutlinedButton(
                onClick = { showGDriveInfo = true },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Icon(Icons.Default.CloudSync, null, tint = Color(0xFF4285F4))
                Spacer(Modifier.width(8.dp))
                Text("Conectar Google Drive", color = Color.White)
            }

            Text(
                "v1.0.0 · BioAcupunt Clinic MTC",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.3f)
                ),
                textAlign = TextAlign.Center
            )
        }

        // Google Drive info dialog
        if (showGDriveInfo) {
            AlertDialog(
                onDismissRequest = { showGDriveInfo = false },
                icon = { Icon(Icons.Default.Cloud, null, tint = Color(0xFF4285F4)) },
                title = { Text("Google Drive") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Ao conectar o Google Drive você pode:")
                        listOf(
                            "☁️ Backup automático dos prontuários",
                            "📂 Sincronizar documentos e laudos",
                            "🔒 Armazenamento seguro e criptografado",
                            "📱 Acesso offline com sync automático"
                        ).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "⚠️ Faça login primeiro para conectar sua conta Google.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showGDriveInfo = false }) { Text("Entendido") }
                }
            )
        }
    }
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White.copy(alpha = 0.9f),
    focusedLabelColor = Primary,
    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
    focusedLeadingIconColor = Primary,
    unfocusedLeadingIconColor = Color.White.copy(alpha = 0.6f),
    focusedBorderColor = Primary,
    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
    cursorColor = Primary,
    focusedTrailingIconColor = Color.White.copy(alpha = 0.8f),
    unfocusedTrailingIconColor = Color.White.copy(alpha = 0.5f)
)
