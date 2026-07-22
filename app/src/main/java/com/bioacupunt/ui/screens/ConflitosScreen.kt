package com.bioacupunt.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.sync.presentation.ConflictItem
import com.bioacupunt.sync.presentation.ConflictViewModel
import com.bioacupunt.ui.theme.Elevation
import com.bioacupunt.ui.theme.Motion
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.SemanticWarning
import com.bioacupunt.ui.theme.SemanticWarningBg
import com.bioacupunt.ui.theme.TextMuted
import com.bioacupunt.ui.theme.supremeShadow

/**
 * Conflitos de sincronização.
 *
 * Shown when the same record was edited on two devices. The app has deliberately
 * not chosen between them: both versions are here, side by side, and she decides.
 *
 * Three deliberate choices in this screen:
 *
 * - **Differing fields are marked.** Two near-identical records with one changed
 *   word is exactly the case where a person picks wrong. The differences are
 *   called out rather than left to be spotted.
 * - **Nothing is pre-selected and nothing times out.** A default would become
 *   the answer by accident, which is the automatic overwrite this design exists
 *   to avoid.
 * - **The empty state is explicit.** "Nenhum conflito" is stated, never implied
 *   by a blank screen — "nothing to resolve" and "failed to load" must not look
 *   the same.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflitosScreen(
    onBack: () -> Unit = {},
    viewModel: ConflictViewModel? = null,
) {
    val vm = viewModel ?: viewModel(factory = com.bioacupunt.di.AppContainer.conflictViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Conflitos de sincronização", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                },
            )
        },
    ) { padding ->
        if (state.items.isEmpty()) {
            EmptyConflicts(Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = SemanticWarningBg,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Estes registros foram editados em mais de um aparelho. " +
                            "Nada foi sobrescrito — escolha qual versão manter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SemanticWarning,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }

            itemsIndexed(state.items) { index, item ->
                AnimatedVisibility(
                    visibleState = remember {
                        MutableTransitionState(false).apply { targetState = true }
                    },
                    enter = Motion.listItemEnter(index),
                ) {
                    ConflictCard(
                        item = item,
                        enabled = !state.isResolving,
                        onKeepLocal = { vm.keepLocal(item.id) },
                        onKeepServer = { vm.keepServer(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConflictCard(
    item: ConflictItem,
    enabled: Boolean,
    onKeepLocal: () -> Unit,
    onKeepServer: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .supremeShadow(shape = MaterialTheme.shapes.large, elevation = Elevation.Card)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text(item.entityLabel.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(item.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        }

        // Only the fields that actually disagree. Listing identical fields too
        // would bury the one word that changed in a wall of matching text.
        val differing = item.fields.filter { it.differs }
        if (differing.isEmpty()) {
            Text(
                "As duas versões têm o mesmo conteúdo. Pode manter qualquer uma.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        } else {
            differing.forEach { field ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        field.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    VersionRow(
                        icon = { Icon(Icons.Default.PhoneAndroid, null, tint = Primary, modifier = Modifier.size(16.dp)) },
                        label = "Neste aparelho",
                        value = field.localValue.ifBlank { "—" },
                    )
                    VersionRow(
                        icon = { Icon(Icons.Default.Cloud, null, tint = TextMuted, modifier = Modifier.size(16.dp)) },
                        label = "No servidor",
                        value = field.serverValue.ifBlank { "—" },
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onKeepLocal,
                enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) { Text("Manter deste aparelho") }
            OutlinedButton(
                onClick = onKeepServer,
                enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text("Manter do servidor") }
        }
    }
}

@Composable
private fun VersionRow(icon: @Composable () -> Unit, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        icon()
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyConflicts(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.CheckCircle, null, tint = Primary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            "Nenhum conflito pendente",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Todos os registros estão iguais no aparelho e no servidor.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
    }
}
