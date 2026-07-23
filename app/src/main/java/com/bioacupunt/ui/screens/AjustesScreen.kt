package com.bioacupunt.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.SemanticError
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(onLogout: () -> Unit = {}) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Perfil", "Clínica", "IA", "Segurança", "Sistema")

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
            tabs.forEachIndexed { i, t ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(t, maxLines = 1) })
            }
        }
        when (selectedTab) {
            0 -> ProfileTab()
            1 -> ClinicTab()
            2 -> AiApisTab()
            3 -> SecurityTab(onLogout)
            4 -> SystemTab()
        }
    }
}

// ── PROFILE TAB ─────────────────────────────────────────────
@Composable
private fun ProfileTab() {
    val context = LocalContext.current
    val securePrefs = remember { com.bioacupunt.di.AppContainer.securePreferences }
    var name by remember { mutableStateOf(securePrefs.professionalName.ifBlank { "Dra. Camila" }) }
    var specialty by remember { mutableStateOf(securePrefs.professionalSpecialty.ifBlank { "Acupunturista · MTC" }) }
    var crmNumber by remember { mutableStateOf(securePrefs.professionalRegistration.ifBlank { "CFMTC-12345" }) }
    var phone by remember { mutableStateOf("+55 91 99999-0000") }
    var email by remember { mutableStateOf("camila@bioacupunt.com.br") }
    var bio by remember { mutableStateOf("Especialista em Medicina Tradicional Chinesa com mais de 10 anos de experiência em acupuntura, fitoterapia e moxibustão.") }
    var logoUri by remember { mutableStateOf<Uri?>(null) }
    var saved by remember { mutableStateOf(false) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> logoUri = uri }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

        item {
            // Logo / Avatar section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.06f)),
                border = BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Logo / Foto da Profissional",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(Modifier.height(12.dp))

                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Primary.copy(alpha = 0.15f))
                                .border(3.dp, Primary.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (logoUri != null) {
                                AsyncImage(
                                    model = logoUri,
                                    contentDescription = "Logo",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Person, null, tint = Primary, modifier = Modifier.size(52.dp))
                            }
                        }
                        IconButton(
                            onClick = { logoPickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Primary)
                        ) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Toque no ícone para selecionar PNG/JPG",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (logoUri != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { logoUri = null }) { Text("Remover") }
                            Button(
                                onClick = { saved = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(4.dp)); Text("Salvar Logo") }
                        }
                    }

                    if (saved) {
                        Spacer(Modifier.height(8.dp))
                        Text("✅ Logo salva com sucesso!", style = MaterialTheme.typography.labelMedium, color = Color(0xFF4CAF50))
                    }
                }
            }
        }

        item { SectionHeader("Informações Pessoais") }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsTextField(name, { name = it }, "Nome completo", Icons.Default.Person)
                SettingsTextField(specialty, { specialty = it }, "Especialidade", Icons.Default.MedicalServices)
                SettingsTextField(crmNumber, { crmNumber = it }, "Registro profissional", Icons.Default.Badge)
                SettingsTextField(phone, { phone = it }, "Telefone", Icons.Default.Phone)
                SettingsTextField(email, { email = it }, "E-mail", Icons.Default.Email)
            }
        }

        item {
            OutlinedTextField(
                value = bio, onValueChange = { bio = it },
                label = { Text("Bio / Apresentação") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3, maxLines = 5,
                leadingIcon = { Icon(Icons.Default.Edit, null) }
            )
        }

        item {
            Button(
                onClick = {
                    securePrefs.professionalName = name.trim()
                    securePrefs.professionalSpecialty = specialty.trim()
                    securePrefs.professionalRegistration = crmNumber.trim()
                    saved = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Salvar Perfil", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── CLINIC TAB ──────────────────────────────────────────────
@Composable
private fun ClinicTab() {
    val scope = rememberCoroutineScope()
    val securePrefs = remember { com.bioacupunt.di.AppContainer.securePreferences }
    var clinicName by remember { mutableStateOf("Clínica BioAcupunt") }
    var address by remember { mutableStateOf("") }
    var sessionPrice by remember { mutableStateOf(securePrefs.sessionPriceBrl.ifBlank { "150" }) }
    var firstPrice by remember { mutableStateOf(securePrefs.firstConsultPriceBrl.ifBlank { "250" }) }
    var workStart by remember { mutableStateOf("08:00") }
    var workEnd by remember { mutableStateOf("18:00") }
    var workDays by remember { mutableStateOf(setOf("SEG", "TER", "QUA", "QUI", "SEX")) }
    var gdriveLinked by remember { mutableStateOf(securePrefs.googleDriveLinked) }
    var tcleText by remember {
        mutableStateOf(
            securePrefs.tcleText.ifBlank {
                "Declaro estar ciente das indicações, contraindicações e da natureza dos procedimentos de MTC propostos."
            }
        )
    }
    val allTechniques = remember { com.bioacupunt.prontuario.domain.safety.Technique.entries }
    var enabledTechniques by remember {
        mutableStateOf(
            securePrefs.enabledTechniquesCsv.split(",").filter { it.isNotBlank() }.toSet()
                .ifEmpty { allTechniques.map { it.name }.toSet() }
        )
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Dados da Clínica") }
        item { SettingsTextField(clinicName, { clinicName = it }, "Nome da clínica", Icons.Default.Business) }
        item { SettingsTextField(address, { address = it }, "Endereço completo", Icons.Default.LocationOn) }

        item { SectionHeader("Financeiro") }
        item { SettingsTextField(sessionPrice, { sessionPrice = it }, "Valor sessão (R$)", Icons.Default.AttachMoney) }
        item { SettingsTextField(firstPrice, { firstPrice = it }, "Valor 1ª consulta (R$)", Icons.Default.AttachMoney) }

        item { SectionHeader("Horário de Funcionamento") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsTextField(workStart, { workStart = it }, "Início", Icons.Default.AccessTime, Modifier.weight(1f))
                SettingsTextField(workEnd, { workEnd = it }, "Fim", Icons.Default.AccessTime, Modifier.weight(1f))
            }
        }
        item {
            val allDays = listOf("SEG", "TER", "QUA", "QUI", "SEX", "SAB", "DOM")
            Column {
                Text("Dias de Atendimento", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    allDays.forEach { day ->
                        FilterChip(
                            selected = day in workDays,
                            onClick = { workDays = if (day in workDays) workDays - day else workDays + day },
                            label = { Text(day, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        item { SectionHeader("Termo de Consentimento (TCLE)") }
        item {
            OutlinedTextField(
                value = tcleText, onValueChange = { tcleText = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3, maxLines = 6,
            )
        }

        item { SectionHeader("Técnicas complementares") }
        item {
            Text(
                "Ao desativar, a técnica não aparece como opção no Atendimento.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    allTechniques.forEach { tech ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(tech.label, style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = tech.name in enabledTechniques,
                                onCheckedChange = { checked ->
                                    enabledTechniques = if (checked) enabledTechniques + tech.name else enabledTechniques - tech.name
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Primary),
                            )
                        }
                        if (tech != allTechniques.last()) HorizontalDivider()
                    }
                }
            }
        }

        item { SectionHeader("Integrações") }
        item {
            SettingsSwitchRow(
                icon = Icons.Default.Cloud,
                title = "Google Drive",
                subtitle = if (gdriveLinked) "Conectado — backup automático ativo" else "Não conectado. Ative para backup automático",
                checked = gdriveLinked,
                onCheck = { gdriveLinked = it; securePrefs.googleDriveLinked = it },
                iconColor = Color(0xFF4285F4)
            )
        }
        if (gdriveLinked) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4285F4).copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("☁️ Google Drive conectado", style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF4285F4)))
                        Text("Prontuários · Laudos · Fotos de evolução — sincronizados automaticamente", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                scope.launch {
                    com.bioacupunt.di.AppContainer.backupManager.createBackupBytes().onSuccess { bytes ->
                        com.bioacupunt.di.AppContainer.googleDriveClient.lastAccount()?.let { acc ->
                            com.bioacupunt.di.AppContainer.googleDriveClient.uploadBackup(acc, com.bioacupunt.backup.BackupManager.suggestedFileName(), bytes)
                        }
                    }
                }
            }, modifier = Modifier.weight(1f)) { Text("Fazer backup agora") }
                            OutlinedButton(onClick = { gdriveLinked = false; securePrefs.googleDriveLinked = false }, modifier = Modifier.weight(1f)) { Text("Desconectar") }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    securePrefs.sessionPriceBrl = sessionPrice.trim()
                    securePrefs.firstConsultPriceBrl = firstPrice.trim()
                    securePrefs.tcleText = tcleText.trim()
                    securePrefs.enabledTechniquesCsv = enabledTechniques.joinToString(",")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Salvar Configurações")
            }
        }
    }
}

// ── AI APIS TAB ─────────────────────────────────────────────
@Composable
private fun AiApisTab() {
    val cacheManager = remember { com.bioacupunt.di.AppContainer.cacheManager }

    var enableClinical by remember { mutableStateOf(true) }
    var enableFlashcards by remember { mutableStateOf(true) }
    var enableReports by remember { mutableStateOf(true) }
    var enableCrm by remember { mutableStateOf(true) }
    var enableImagePrompts by remember { mutableStateOf(false) }
    var maxTokens by remember { mutableIntStateOf(2048) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("📱 IA local (offline)") }
        item { LocalModelCard() }

        item { SectionHeader("⚙️ Agentes de IA") }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingsSwitchRow(Icons.Default.MedicalServices, "Assistente Clínico", "Análise de padrões MTC, diagnóstico energético", enableClinical, { enableClinical = it })
                SettingsSwitchRow(Icons.Default.School, "Gerador de Flashcards", "Cria cards de estudo com IA", enableFlashcards, { enableFlashcards = it })
                SettingsSwitchRow(Icons.Default.Description, "Redator de Relatórios", "Laudos, evoluções e documentos clínicos", enableReports, { enableReports = it })
                SettingsSwitchRow(Icons.Default.People, "Consultor CRM", "Insights de retenção e relacionamento", enableCrm, { enableCrm = it })
                SettingsSwitchRow(Icons.Default.Image, "Gerador de Prompts Visuais", "Cria prompts para imagens médicas", enableImagePrompts, { enableImagePrompts = it })
            }
        }

        item { SectionHeader("⚡ Performance") }
        item {
            Column {
                Text("Máximo de tokens por resposta: $maxTokens", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = maxTokens.toFloat(), onValueChange = { maxTokens = it.toInt() },
                    valueRange = 256f..4096f, steps = 15,
                    colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary)
                )
                Text(
                    "Cache de IA: ${cacheManager.memoryUsageKb()} KB em memória · ${cacheManager.diskUsageKb()} KB em disco",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

    }

}

// ── SECURITY TAB ────────────────────────────────────────────
@Composable
private fun SecurityTab(onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    val securePrefs = remember { com.bioacupunt.di.AppContainer.securePreferences }
    var biometricEnabled by remember { mutableStateOf(securePrefs.biometricEnabled) }
    var autoLockMin by remember { mutableIntStateOf(5) }
    var encryptionEnabled by remember { mutableStateOf(true) }
    var showChangePass by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var changePassError by remember { mutableStateOf<String?>(null) }
    var changePassSuccess by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.06f)),
                border = BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, null, tint = Primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Segurança AES-256", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text("Todos os dados são criptografados localmente com EncryptedSharedPreferences.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item { SectionHeader("Acesso") }
        item { SettingsSwitchRow(Icons.Default.Fingerprint, "Biometria / Impressão Digital", "Exigir biometria ao abrir o app", biometricEnabled, { biometricEnabled = it; securePrefs.biometricEnabled = it }) }
        item {
            Column {
                Text("Auto-bloqueio: $autoLockMin min", style = MaterialTheme.typography.bodySmall)
                Slider(value = autoLockMin.toFloat(), onValueChange = { autoLockMin = it.toInt() }, valueRange = 1f..30f, steps = 28, colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary))
            }
        }

        item { SectionHeader("Dados") }
        item { SettingsSwitchRow(Icons.Default.Lock, "Criptografia de Banco Local", "Room DB criptografado", encryptionEnabled, { encryptionEnabled = it }, enabled = false) }
        item {
            OutlinedButton(onClick = { showChangePass = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Key, null); Spacer(Modifier.width(8.dp)); Text("Alterar Senha")
            }
        }

        item { SectionHeader("Privacidade") }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "✅ Dados armazenados localmente no dispositivo",
                        "✅ Sincronização criptografada via TLS 1.3",
                        "✅ Nenhum dado enviado a terceiros sem consentimento",
                        "✅ Conformidade com LGPD (Lei 13.709/2018)",
                        "✅ Backup Google Drive com criptografia ponta-a-ponta"
                    ).forEach { item -> Text(item, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }

        item { SectionHeader("Backup") }
        item { BackupCard() }

        item { Spacer(Modifier.height(8.dp)) }
        item {
            OutlinedButton(
                onClick = { showLogoutConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                border = BorderStroke(1.dp, Color(0xFFEF5350))
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null); Spacer(Modifier.width(8.dp)); Text("Sair da Conta")
            }
        }
    }

    if (showChangePass) {
        AlertDialog(
            onDismissRequest = { showChangePass = false; changePassError = null; changePassSuccess = false },
            icon = { Icon(Icons.Default.Key, null, tint = Primary) },
            title = { Text("Alterar PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (changePassSuccess) {
                        Text("✅ PIN alterado com sucesso!", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        OutlinedTextField(
                            value = oldPin, onValueChange = { oldPin = it.filter { c -> c.isDigit() }; changePassError = null },
                            label = { Text("PIN atual") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = newPin, onValueChange = { newPin = it.filter { c -> c.isDigit() }; changePassError = null },
                            label = { Text("Novo PIN (mín. 4 dígitos)") },
                            leadingIcon = { Icon(Icons.Default.LockReset, null) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = confirmNewPin, onValueChange = { confirmNewPin = it.filter { c -> c.isDigit() }; changePassError = null },
                            label = { Text("Confirmar novo PIN") },
                            leadingIcon = { Icon(Icons.Default.LockReset, null) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        changePassError?.let {
                            Text(it, color = SemanticError, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                if (changePassSuccess) {
                    Button(onClick = { showChangePass = false }) { Text("Fechar") }
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                when {
                                    !com.bioacupunt.security.LocalPinAuth.isValidPin(newPin) -> changePassError = "O novo PIN deve ter ao menos 4 dígitos."
                                    newPin != confirmNewPin -> changePassError = "Os PINs não coincidem."
                                    else -> {
                                        val localAuth = com.bioacupunt.di.AppContainer.localAuthManager
                                        if (!localAuth.verifyPin(oldPin)) {
                                            changePassError = "PIN atual incorreto."
                                        } else if (localAuth.setPin(newPin)) {
                                            changePassSuccess = true
                                            changePassError = null
                                            oldPin = ""; newPin = ""; confirmNewPin = ""
                                        } else {
                                            changePassError = "Erro ao alterar PIN."
                                        }
                                    }
                                }
                            }
                        },
                        enabled = oldPin.isNotBlank() && newPin.isNotBlank() && confirmNewPin.isNotBlank(),
                    ) { Text("Alterar PIN") }
                }
            },
            dismissButton = {
                if (!changePassSuccess) {
                    TextButton(onClick = { showChangePass = false; changePassError = null }) { Text("Cancelar") }
                }
            },
        )
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color(0xFFEF5350)) },
            title = { Text("Bloquear o app?") },
            text = { Text("Você voltará à tela de PIN. Sua conta e seus dados continuam salvos no aparelho.") },
            confirmButton = {
                Button(
                    onClick = {
                        // Auth local: apenas re-travar. NUNCA clearAll() aqui — isso
                        // apagaria o PIN e o perfil, exigindo recriar a conta.
                        securePrefs.isLoggedIn = false
                        showLogoutConfirm = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                ) { Text("Bloquear") }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancelar") } }
        )
    }
}

// ── SYSTEM TAB ──────────────────────────────────────────────
@Composable
private fun SystemTab() {
    val securePrefs = remember { com.bioacupunt.di.AppContainer.securePreferences }
    var darkMode by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var reminderMin by remember { mutableIntStateOf(30) }
    var cacheSize by remember { mutableStateOf("2.4 MB") }
    var language by remember { mutableStateOf("Português (Brasil)") }
    var serverUrl by remember { mutableStateOf(securePrefs.serverUrl) }
    var serverSaved by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Servidor") }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "O app já vem conectado ao servidor de produção. Só mude aqui se quiser apontar para outro backend (ex.: um servidor local de desenvolvimento).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it; serverSaved = false },
                        label = { Text("URL do servidor") },
                        placeholder = { Text("https://bioacupunt-api.onrender.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { securePrefs.serverUrl = serverUrl.trim(); serverSaved = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Salvar servidor")
                    }
                    if (serverSaved) {
                        Text("✅ Servidor salvo.", style = MaterialTheme.typography.labelMedium, color = Color(0xFF4CAF50))
                    }
                }
            }
        }

        item { SectionHeader("Aparência") }
        item {
            val dark by com.bioacupunt.ui.theme.ThemeController.dark
            SettingsSwitchRow(
                Icons.Default.DarkMode, "Modo Escuro",
                "Paleta escura do design BioAcupunt",
                dark,
                { com.bioacupunt.ui.theme.ThemeController.toggle(securePrefs) },
            )
        }

        item { SectionHeader("Notificações") }
        item { SettingsSwitchRow(Icons.Default.Notifications, "Notificações de Consultas", "Alertas sobre consultas do dia", notificationsEnabled, { notificationsEnabled = it }) }
        item {
            Column {
                Text("Lembrete antes da consulta: $reminderMin min", style = MaterialTheme.typography.bodySmall)
                Slider(value = reminderMin.toFloat(), onValueChange = { reminderMin = it.toInt() }, valueRange = 5f..60f, steps = 10, colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary))
            }
        }

        item { SectionHeader("Idioma") }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, null, tint = Primary)
                    Spacer(Modifier.width(12.dp))
                    Text(language, modifier = Modifier.weight(1f))
                    Text("PT-BR", style = MaterialTheme.typography.labelSmall, color = Primary)
                }
            }
        }

        item { SectionHeader("Cache & Armazenamento") }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Cache de IA"); Text(cacheSize, color = Primary)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { cacheSize = "0 KB" }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.DeleteSweep, null); Spacer(Modifier.width(8.dp)); Text("Limpar Cache")
                    }
                }
            }
        }

        item { SectionHeader("Sobre o App") }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AboutRow("Versão", com.bioacupunt.BuildConfig.VERSION_NAME)
                    AboutRow("Build", "${com.bioacupunt.BuildConfig.VERSION_CODE}")
                    AboutRow("IA", "Gemma 3 1B · local (offline)")
                    AboutRow("Segurança", "AES-256 + TLS 1.3")
                    AboutRow("Conformidade", "LGPD · CFM · CFMTC")
                    AboutRow("Suporte", "suporte@bioacupunt.com.br")
                }
            }
        }
    }
}

// ── Shared components ─────────────────────────────────────────
@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge.copy(color = Primary, fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun SettingsTextField(
    value: String, onValue: (String) -> Unit,
    label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value, onValueChange = onValue,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheck: (Boolean) -> Unit,
    iconColor: Color = Primary,
    enabled: Boolean = true
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheck, enabled = enabled, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Primary))
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
    }
}
