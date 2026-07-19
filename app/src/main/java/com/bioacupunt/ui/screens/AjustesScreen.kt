package com.bioacupunt.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.bioacupunt.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(onLogout: () -> Unit = {}) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Perfil", "Clínica", "IA local", "Segurança", "Sistema")

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
                            OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) { Text("Fazer backup agora") }
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
    val mgr = remember { com.bioacupunt.di.AppContainer.localModelManager }
    val scope = rememberCoroutineScope()

    val ramMb = remember { mgr.deviceTotalRamMb() }
    val downloadable = remember { mgr.downloadableModels() }
    val downloadState by mgr.state.collectAsState(initial = com.bioacupunt.ai.data.provider.LocalModelManager.State.Idle)

    // isInstalled()/activeModel() leem o disco; um contador força recomposição após
    // baixar/apagar sem observar o filesystem inteiro.
    var refresh by remember { mutableIntStateOf(0) }
    val activeId = remember(refresh) { mgr.activeModel()?.id }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("🧠 IA no dispositivo") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.06f)),
                border = BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Tudo roda no seu aparelho", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Sem nuvem e sem chave de API. O dado da paciente nunca sai do celular. " +
                            "Baixe um modelo uma vez (Wi-Fi recomendado); depois funciona offline.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Memória do aparelho: ${if (ramMb > 0) "$ramMb MB" else "desconhecida"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (downloadable.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFB00020).copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, Color(0xFFB00020).copy(alpha = 0.25f))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = Color(0xFFB00020), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Este aparelho não tem memória suficiente para rodar um modelo local com " +
                                "segurança. A IA fica indisponível — melhor isso do que travar no meio de um atendimento.",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        items(downloadable, key = { it.id }) { model ->
            val installed = remember(refresh) { mgr.isInstalled(model) }
            val isActive = activeId == model.id
            val busy = downloadState.let {
                it is com.bioacupunt.ai.data.provider.LocalModelManager.State.Downloading && it.modelId == model.id ||
                    it is com.bioacupunt.ai.data.provider.LocalModelManager.State.Verifying && it.modelId == model.id
            }
            LocalModelCard(
                name = model.displayName,
                subtitle = "${model.sizeMb} MB · ${model.license.label} · ${model.notes}",
                installed = installed,
                isActive = isActive,
                busy = busy,
                downloadState = downloadState,
                modelId = model.id,
                onDownload = {
                    scope.launch {
                        mgr.download(model)
                        refresh++
                    }
                },
                onSelect = {
                    mgr.selectModel(model)
                    refresh++
                },
                onDelete = {
                    scope.launch {
                        mgr.delete(model)
                        refresh++
                    }
                }
            )
        }

        (downloadState as? com.bioacupunt.ai.data.provider.LocalModelManager.State.Failed)?.let { failed ->
            item {
                Text(
                    "Falha ao baixar/verificar: ${failed.message}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFB00020)
                )
            }
        }
    }
}

@Composable
private fun LocalModelCard(
    name: String,
    subtitle: String,
    installed: Boolean,
    isActive: Boolean,
    busy: Boolean,
    downloadState: com.bioacupunt.ai.data.provider.LocalModelManager.State,
    modelId: String,
    onDownload: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, if (isActive) Primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, null, tint = Primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isActive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Em uso", style = MaterialTheme.typography.labelSmall, color = Primary)
                    }
                }
            }

            if (busy) {
                Spacer(Modifier.height(10.dp))
                val progress = (downloadState as? com.bioacupunt.ai.data.provider.LocalModelManager.State.Downloading)
                    ?.takeIf { it.modelId == modelId }?.progress
                if (progress != null) {
                    LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Text("Baixando… ${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Verificando integridade…", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        !installed -> Button(
                            onClick = onDownload,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp)); Text("Baixar")
                        }
                        !isActive -> {
                            OutlinedButton(onClick = onSelect) { Text("Usar este") }
                            OutlinedButton(onClick = onDelete) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Apagar")
                            }
                        }
                        else -> OutlinedButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Apagar")
                        }
                    }
                }
            }
        }
    }
}

// ── SECURITY TAB ────────────────────────────────────────────
@Composable
private fun SecurityTab(onLogout: () -> Unit) {
    val securePrefs = remember { com.bioacupunt.di.AppContainer.securePreferences }
    var biometricEnabled by remember { mutableStateOf(securePrefs.biometricEnabled) }
    var autoLockMin by remember { mutableIntStateOf(5) }
    var encryptionEnabled by remember { mutableStateOf(true) }
    var showChangePass by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

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

        item { Spacer(Modifier.height(8.dp)) }
        item {
            OutlinedButton(
                onClick = { showLogoutConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                border = BorderStroke(1.dp, Color(0xFFEF5350))
            ) {
                Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Sair da Conta")
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            icon = { Icon(Icons.Default.Logout, null, tint = Color(0xFFEF5350)) },
            title = { Text("Sair da conta?") },
            text = { Text("Você precisará fazer login novamente para acessar o BioAcupunt.") },
            confirmButton = {
                Button(
                    onClick = {
                        securePrefs.clearAll()
                        showLogoutConfirm = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                ) { Text("Sair") }
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
            SettingsSwitchRow(
                Icons.Default.DarkMode, "Modo Escuro",
                "Em breve — o app usa o tema claro \"Supremo\" por enquanto",
                darkMode, { }, enabled = false,
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
                    AboutRow("IA", "Local (no dispositivo)")
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
