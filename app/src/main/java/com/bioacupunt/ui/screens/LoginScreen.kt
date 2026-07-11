package com.bioacupunt.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.draw.shadow
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
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showGDriveInfo by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val gradientAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000), RepeatMode.Reverse),
        label = "gradient"
    )

    val surfaceColor = if (MaterialTheme.colorScheme.surface.alpha < 0.8f)
        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    else MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PrimaryDark.copy(alpha = 0.55f + 0.2f * gradientAngle),
                        Color(0xFF07210C),
                        Primary.copy(alpha = 0.45f + 0.15f * gradientAngle)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .shadow(24.dp, shape = MaterialTheme.shapes.extraLarge, spotColor = Primary.copy(alpha = 0.25f))
                .clip(MaterialTheme.shapes.extraLarge)
                .background(surfaceColor.copy(alpha = 0.64f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.18f),
                    shape = MaterialTheme.shapes.extraLarge
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo / Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Primary.copy(alpha = 0.25f),
                                Primary.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(2.dp, Primary.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalHospital,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                "BioAcupunt",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )
            Text(
                "Gestão Clínica MTC",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.75f))
            )

            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMsg = null },
                label = { Text("E-mail") },
                leadingIcon = { Icon(Icons.Default.Email, null, tint = Primary.copy(alpha = 0.9f)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                    focusedContainerColor = Color.White.copy(alpha = 0.08f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                    focusedTextColor = Color(0xFFE8F5E9),
                    unfocusedTextColor = Color(0xFFE8F5E9)
                )
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMsg = null },
                label = { Text("Senha") },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Primary.copy(alpha = 0.9f)) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Mostrar senha",
                            tint = Color(0xFFD7E8D3)
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
                    scope.launch {
                        val result = AppContainer.authRepository.login(email.trim(), password)
                        result.onSuccess { onLoginSuccess() }
                            .onFailure { errorMsg = it.localizedMessage ?: "Erro ao entrar. Tente novamente." }
                    }
                }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                    focusedContainerColor = Color.White.copy(alpha = 0.08f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                    focusedTextColor = Color(0xFFE8F5E9),
                    unfocusedTextColor = Color(0xFFE8F5E9)
                )
            )

            AnimatedVisibility(visible = errorMsg != null) {
                Text(
                    text = errorMsg ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(6.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    errorMsg = null
                    scope.launch {
                        val result = AppContainer.authRepository.login(email.trim(), password)
                        result.onSuccess { onLoginSuccess() }
                            .onFailure { errorMsg = it.localizedMessage ?: "Erro ao entrar. Tente novamente." }
                    }
                },
                enabled = email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(10.dp, shape = MaterialTheme.shapes.large, spotColor = Primary.copy(alpha = 0.32f)),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary.copy(alpha = 0.82f),
                    contentColor = Color.White,
                    disabledContainerColor = Primary.copy(alpha = 0.35f)
                ),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) { Text("Entrar", fontWeight = FontWeight.Bold) }

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.18f))

            Text(
                "Integrações",
                style = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(alpha = 0.6f))
            )

            OutlinedButton(
                onClick = { showGDriveInfo = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD7E8D3))
            ) {
                Icon(Icons.Default.CloudSync, null, tint = Color(0xFFA8C9B8))
                Spacer(Modifier.width(10.dp))
                Text("Conectar Google Drive", color = Color(0xFFE8F5E9))
            }
        }
    }

    if (showGDriveInfo) {
        AlertDialog(
            onDismissRequest = { showGDriveInfo = false },
            icon = { Icon(Icons.Default.CloudSync, null, tint = Primary) },
            title = { Text("Google Drive") },
            text = { Text("Conecte sua conta para sincronizar arquivos clínicos com segurança.") },
            confirmButton = { TextButton(onClick = { showGDriveInfo = false }) { Text("Entendi", color = Primary) } },
            dismissButton = { TextButton(onClick = { showGDriveInfo = false }) { Text("Fechar") } }
        )
    }
}
