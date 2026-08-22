package com.bioacupunt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioacupunt.crm.domain.model.CrmTask
import com.bioacupunt.crm.domain.model.CrmActivity
import com.bioacupunt.crm.domain.model.Patient360
import com.bioacupunt.crm.domain.model.PatientOperationalStatus
import com.bioacupunt.crm.domain.model.TaskStatus
import com.bioacupunt.crm.domain.model.CrmActivityType
import com.bioacupunt.ui.theme.Primary

/**
 * Patient 360 Screen — unified view combining clinical + CRM data.
 *
 * Structure:
 * - Patient Header (name, status, tags, next appointment)
 * - Tabs: Overview | Clinical | CRM | Tasks | Timeline
 * - Each tab shows relevant data from both clinical and CRM domains
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Patient360Screen(
    patient: Patient360,
    tasks: List<CrmTask> = emptyList(),
    activities: List<CrmActivity> = emptyList(),
    onNavigateToProntuario: (Long) -> Unit = {},
    onNavigateToTask: (Long) -> Unit = {},
    onBack: () -> Unit = {},
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Resumo", "Clínico", "CRM", "Tarefas", "Timeline")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(patient.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToProntuario(patient.patientId) }) {
                        Icon(Icons.Default.MedicalServices, contentDescription = "Prontuário")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Patient Header
            PatientHeader(patient)

            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> OverviewTab(patient)
                1 -> ClinicalTab(patient)
                2 -> CrmTab(patient, activities)
                3 -> TasksTab(tasks, onNavigateToTask)
                4 -> TimelineTab(patient, tasks, activities)
            }
        }
    }
}

@Composable
private fun PatientHeader(patient: Patient360) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Text(
                        text = patient.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (patient.phone.isNotEmpty()) {
                        Text(
                            text = patient.phone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Operational Status Badge
                val statusColor = when (patient.operationalStatus) {
                    PatientOperationalStatus.ACTIVE -> Color(0xFF4CAF50)
                    PatientOperationalStatus.AT_RISK -> Color(0xFFFF9800)
                    PatientOperationalStatus.INACTIVE -> Color(0xFFF44336)
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = statusColor.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = patient.operationalStatus.label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick stats
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                StatItem("Sessões", patient.sessionCount.toString())
                StatItem("Tarefas", patient.pendingTasks.toString())
                StatItem("Atividades", patient.totalActivities.toString())
                if (patient.overdueTasks > 0) {
                    StatItem("Atrasadas", patient.overdueTasks.toString(), Color(0xFFF44336))
                }
            }

            // Tags
            if (patient.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    patient.tags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color = MaterialTheme.colorScheme.primary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OverviewTab(patient: Patient360) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Last encounter
        item {
            InfoCard(
                icon = Icons.Default.MedicalServices,
                title = "Último atendimento",
                value = patient.lastEncounterDate ?: "Nenhum",
            )
        }

        // Current assessment
        if (patient.currentAssessment.isNotEmpty()) {
            item {
                InfoCard(
                    icon = Icons.Default.Assessment,
                    title = "Avaliação atual",
                    value = patient.currentAssessment,
                )
            }
        }

        // Chief complaint
        if (patient.chiefComplaint.isNotEmpty()) {
            item {
                InfoCard(
                    icon = Icons.Default.Chat,
                    title = "Queixa principal",
                    value = patient.chiefComplaint,
                )
            }
        }

        // Missing data
        if (patient.missingData.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Dados faltando",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        patient.missingData.forEach { item ->
                            Text("• $item", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Longitudinal summary
        if (patient.longitudinalSummary.isNotEmpty()) {
            item {
                InfoCard(
                    icon = Icons.Default.Timeline,
                    title = "Resumo longitudinal",
                    value = patient.longitudinalSummary,
                )
            }
        }
    }
}

@Composable
private fun ClinicalTab(patient: Patient360) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            InfoCard(
                icon = Icons.Default.MedicalServices,
                title = "Total de sessões",
                value = "${patient.sessionCount} sessões registradas",
            )
        }

        item {
            InfoCard(
                icon = Icons.Default.Assessment,
                title = "Avaliação atual",
                value = patient.currentAssessment.takeIf { it.isNotEmpty() } ?: "Não registrada",
            )
        }

        item {
            InfoCard(
                icon = Icons.Default.Chat,
                title = "Queixa principal",
                value = patient.chiefComplaint.takeIf { it.isNotEmpty() } ?: "Não registrada",
            )
        }
    }
}

@Composable
private fun CrmTab(patient: Patient360, activities: List<CrmActivity>) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                "Atividades recentes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        if (activities.isEmpty()) {
            item {
                Text(
                    "Nenhuma atividade registrada",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(activities.take(10)) { activity ->
                ActivityItem(activity)
            }
        }
    }
}

@Composable
private fun TasksTab(tasks: List<CrmTask>, onNavigateToTask: (Long) -> Unit) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                "Tarefas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        if (tasks.isEmpty()) {
            item {
                Text(
                    "Nenhuma tarefa",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(tasks) { task ->
                TaskItem(task, onClick = { onNavigateToTask(task.id) })
            }
        }
    }
}

@Composable
private fun TimelineTab(patient: Patient360, tasks: List<CrmTask>, activities: List<CrmActivity>) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                "Linha do tempo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        // Merge and sort by date
        val allEvents = buildList {
            tasks.forEach { add(TimelineEvent.TaskEvent(it)) }
            activities.forEach { add(TimelineEvent.ActivityEvent(it)) }
        }.sortedByDescending { it.timestamp }

        if (allEvents.isEmpty()) {
            item {
                Text(
                    "Nenhum evento",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(allEvents) { event ->
                when (event) {
                    is TimelineEvent.TaskEvent -> TaskItem(event.task, onClick = {})
                    is TimelineEvent.ActivityEvent -> ActivityItem(event.activity)
                }
            }
        }
    }
}

@Composable
private fun InfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ActivityItem(activity: CrmActivity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = when (activity.type) {
                CrmActivityType.CALL -> Icons.Default.Phone
                CrmActivityType.EMAIL -> Icons.Default.Email
                CrmActivityType.MESSAGE -> Icons.AutoMirrored.Filled.Message
                CrmActivityType.MEETING -> Icons.Default.People
                CrmActivityType.NOTE -> Icons.AutoMirrored.Filled.Note
                CrmActivityType.APPOINTMENT -> Icons.Default.CalendarMonth
                CrmActivityType.ENCOUNTER -> Icons.Default.MedicalServices
                CrmActivityType.FOLLOW_UP -> Icons.AutoMirrored.Filled.Assignment
                CrmActivityType.REFERRAL -> Icons.Default.Share
                CrmActivityType.SYSTEM -> Icons.Default.Settings
                CrmActivityType.TASK -> Icons.Default.Task
            }
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(activity.title, style = MaterialTheme.typography.bodyMedium)
                if (activity.timestamp.isNotEmpty()) {
                    Text(
                        activity.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskItem(task: CrmTask, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = when (task.status) {
                TaskStatus.COMPLETED -> Icons.Default.CheckCircle
                TaskStatus.CANCELLED -> Icons.Default.Cancel
                TaskStatus.OVERDOWN -> Icons.Default.Warning
                TaskStatus.IN_PROGRESS -> Icons.Default.PlayArrow
                TaskStatus.PENDING -> Icons.Default.RadioButtonUnchecked
            }
            val tint = when (task.status) {
                TaskStatus.COMPLETED -> Color(0xFF4CAF50)
                TaskStatus.OVERDOWN -> Color(0xFFF44336)
                else -> MaterialTheme.colorScheme.primary
            }
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.bodyMedium)
                if (task.dueDate.isNotEmpty()) {
                    Text(
                        "Prazo: ${task.dueDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    task.priority.label,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

private sealed class TimelineEvent(val timestamp: String) {
    class TaskEvent(val task: CrmTask) : TimelineEvent(task.createdAt)
    class ActivityEvent(val activity: CrmActivity) : TimelineEvent(activity.timestamp)
}
