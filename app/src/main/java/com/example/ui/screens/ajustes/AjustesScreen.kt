package com.example.ui.screens.ajustes

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import com.example.MainViewModel
import com.example.data.local.ClinicaConfigEntity
import com.example.ui.theme.*

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AjustesScreen(
    viewModel: MainViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val clinicaConfig by viewModel.clinicaConfig.collectAsState()

    var profissionalNome by remember(clinicaConfig) { mutableStateOf(clinicaConfig.profissionalNome) }
    var crbm by remember(clinicaConfig) { mutableStateOf(clinicaConfig.crbm) }
    var telefone by remember(clinicaConfig) { mutableStateOf(clinicaConfig.telefone) }
    var instagram by remember(clinicaConfig) { mutableStateOf(clinicaConfig.instagram) }
    var endereco by remember(clinicaConfig) { mutableStateOf(clinicaConfig.endereco) }
    var honorarios by remember(clinicaConfig) { mutableStateOf(clinicaConfig.honorarios) }
    var auriculoterapia by remember(clinicaConfig) { mutableStateOf(clinicaConfig.auriculoterapia) }
    var ventosaterapia by remember(clinicaConfig) { mutableStateOf(clinicaConfig.ventosaterapia) }
    var moxabustao by remember(clinicaConfig) { mutableStateOf(clinicaConfig.moxabustao) }

    var showSuccessMessage by remember { mutableStateOf(false) }

    with(sharedTransitionScope) {
        Column(
            modifier = Modifier
                .sharedBounds(
                    rememberSharedContentState(key = "ajustes_container"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
                .fillMaxSize()
                .background(DarkBlue)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    "Preferências Clínicas",
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Configuração da identidade, honorários e técnicas do sistema",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (showSuccessMessage) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "✓ Configurações salvas e aplicadas localmente com sucesso!",
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold,
                unfocusedBorderColor = BorderColor,
                focusedLabelColor = Gold,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )

            AjusteSection("Identidade Visual & Profissional") {
                OutlinedTextField(
                    value = profissionalNome,
                    onValueChange = { profissionalNome = it },
                    label = { Text("Nome do Profissional") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = crbm,
                    onValueChange = { crbm = it },
                    label = { Text("CRBM / Registro") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )
            }

            AjusteSection("Canais de Contato") {
                OutlinedTextField(
                    value = telefone,
                    onValueChange = { telefone = it },
                    label = { Text("Telefone Comercial") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = instagram,
                    onValueChange = { instagram = it },
                    label = { Text("Instagram Profissional") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = endereco,
                    onValueChange = { endereco = it },
                    label = { Text("Endereço Físico") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )
            }

            AjusteSection("Tabela de Honorários") {
                OutlinedTextField(
                    value = honorarios,
                    onValueChange = { honorarios = it },
                    label = { Text("Honorários Clínicos (R$)") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    colors = textFieldColors
                )
            }

            AjusteSection("Técnicas Complementares Ativas") {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text("Auriculoterapia Integrada", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = auriculoterapia,
                        onCheckedChange = { auriculoterapia = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Gold
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text("Ventosaterapia Seca/Húmida", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = ventosaterapia,
                        onCheckedChange = { ventosaterapia = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Gold
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text("Moxabustão Térmica", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = moxabustao,
                        onCheckedChange = { moxabustao = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Gold
                        )
                    )
                }
            }

            Button(
                onClick = {
                    val updatedConfig = clinicaConfig.copy(
                        profissionalNome = profissionalNome,
                        crbm = crbm,
                        telefone = telefone,
                        instagram = instagram,
                        endereco = endereco,
                        honorarios = honorarios,
                        auriculoterapia = auriculoterapia,
                        ventosaterapia = ventosaterapia,
                        moxabustao = moxabustao
                    )
                    viewModel.saveClinicaConfig(updatedConfig)
                    showSuccessMessage = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("SALVAR CONFIGURAÇÕES DA CLÍNICA", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AjusteSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = title,
            color = Gold,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}
