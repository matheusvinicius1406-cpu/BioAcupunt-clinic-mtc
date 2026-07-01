package com.bioacupunt.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.bioacupunt.crm.domain.model.CrmPatient
import com.bioacupunt.crm.domain.model.PatientStage
import com.bioacupunt.crm.presentation.CrmViewModel
import com.bioacupunt.ui.theme.Primary
import kotlinx.coroutines.launch

@Composable
fun CrmScreen(
    onNavigateToProntuario: (Long) -> Unit = {},
    viewModel: CrmViewModel? = null
) {
    val vm = viewModel ?: androidx.lifecycle.viewmodel.compose.viewModel(factory = com.bioacupunt.di.AppContainer.crmViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val tabs = listOf("Pipeline", "Pacientes", "Relatórios")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { i, t ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(t) })
            }
        }

        when (selectedTab) {
            0 -> PipelineTab(
                patients = state.filteredPatients,
                stages = state.stages,
                viewModel = vm
            )
            1 -> PatientsListTab(
                patients = state.filteredPatients,
                searchQuery = searchQuery,
                onSearch = { query ->
                    searchQuery = query
                    vm.onQueryChanged(query)
                },
                onOpenProntuario = onNavigateToProntuario,
                onDelete = { id -> vm.clearError() },
                onStatusChange = { id, stage -> vm.updateStage(id, stage) }
            )
            2 -> CrmReportsTab(summary = state.reportSummary)
        }
    }

    LaunchedEffect(state.error) {
        val msg = state.error
        if (!msg.isNullOrBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            vm.clearError()
        }
    }
}
