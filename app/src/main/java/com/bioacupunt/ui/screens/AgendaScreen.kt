package com.bioacupunt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.TextMuted
import com.bioacupunt.ui.theme.premiumShadow
import com.bioacupunt.agenda.domain.model.Appointment
import com.bioacupunt.agenda.domain.model.AppointmentStatus
import com.bioacupunt.agenda.domain.model.AppointmentType
import com.bioacupunt.agenda.presentation.AgendaViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class AgendaView(val label: String) { DIA("Dia"), SEMANA("Semana"), MES("Mês") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(viewModel: AgendaViewModel? = null, onOpenAtendimento: (Long) -> Unit = {}) {
    val vm = viewModel ?: viewModel(factory = com.bioacupunt.di.AppContainer.agendaViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()
    val selectedDate = remember(state.selectedDate) { LocalDate.parse(state.selectedDate) }
    var showNewAppointment by remember { mutableStateOf(false) }
    var view by remember { mutableStateOf(AgendaView.SEMANA) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewAppointment = true },
                containerColor = Primary,
                modifier = Modifier.premiumShadow(shape = MaterialTheme.shapes.large, elevationDp = 18.dp)
            ) { Icon(Icons.Default.Add, null, tint = Color.White) }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Text(
                    "Agenda",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AgendaView.entries.forEach { v ->
                        val selected = view == v
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.medium)
                                .background(if (selected) Primary else MaterialTheme.colorScheme.surface)
                                .border(1.dp, if (selected) Primary else MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                                .clickable { view = v }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(v.label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            when (view) {
                AgendaView.DIA -> item {
                    DayHeader(selectedDate, onPrev = { vm.onDateSelected(selectedDate.minusDays(1).toString()) }, onNext = { vm.onDateSelected(selectedDate.plusDays(1).toString()) })
                }
                AgendaView.SEMANA -> item {
                    WeekStrip(selectedDate = selectedDate, onDateSelected = { vm.onDateSelected(it.toString()) })
                }
                AgendaView.MES -> item {
                    MonthCalendarCard(
                        month = YearMonth.parse(state.visibleMonth),
                        selectedDate = selectedDate,
                        monthAppointments = state.monthAppointments,
                        showFreeSlots = state.showFreeSlots,
                        onToggleFreeSlots = vm::toggleFreeSlots,
                        onPrevMonth = { vm.onMonthChanged(YearMonth.parse(state.visibleMonth).minusMonths(1)) },
                        onNextMonth = { vm.onMonthChanged(YearMonth.parse(state.visibleMonth).plusMonths(1)) },
                        onDaySelected = { vm.onDateSelected(it.toString()) },
                    )
                }
            }

            val dayAppointments = state.appointments
            item {
                Text(
                    if (selectedDate == LocalDate.now()) "HOJE" else selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).uppercase() + " · ${selectedDate.dayOfMonth}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            if (dayAppointments.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Default.EventAvailable, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("Nenhuma consulta agendada", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { showNewAppointment = true }) { Text("Agendar agora") }
                    }
                }
            } else {
                val total = dayAppointments.size
                val confirmed = dayAppointments.count { it.status == AppointmentStatus.CONFIRMED.name }
                val revenue = dayAppointments.filter { it.paid }.sumOf { it.valueBrl }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("$total consultas", style = MaterialTheme.typography.labelMedium, color = Primary)
                        Text("$confirmed confirmadas", style = MaterialTheme.typography.labelMedium, color = Color(0xFF64B5F6))
                        Text("R$ %.0f recebidos".format(revenue), style = MaterialTheme.typography.labelMedium, color = Color(0xFF81C784))
                    }
                }
                items(dayAppointments, key = { it.id }) { appt ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        AppointmentCard(
                            appt,
                            onStatusChange = { newStatus -> vm.onStatusChange(appt.id, newStatus) },
                            onAttend = { onOpenAtendimento(appt.id) },
                        )
                    }
                }
            }
        }
    }

    if (showNewAppointment) {
        NewAppointmentDialog(
            onDismiss = { showNewAppointment = false },
            onSave = { patientId, patientName, time, value, type ->
                showNewAppointment = false
                vm.createAppointment(patientId, patientName, time, value, type)
            }
        )
    }
}

@Composable
private fun DayHeader(date: LocalDate, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) { Icon(Icons.Default.ChevronLeft, null) }
        Text(
            date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt", "BR")))
                .replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, null) }
    }
}

@Composable
private fun MonthCalendarCard(
    month: YearMonth,
    selectedDate: LocalDate,
    monthAppointments: List<Appointment>,
    showFreeSlots: Boolean,
    onToggleFreeSlots: () -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDaySelected: (LocalDate) -> Unit,
) {
    val countsByDay = remember(monthAppointments) {
        monthAppointments.groupingBy { runCatching { LocalDate.parse(it.date) }.getOrNull() }.eachCount()
    }
    val today = LocalDate.now()
    val firstOfMonth = month.atDay(1)
    val leadingBlanks = (firstOfMonth.dayOfWeek.value % 7) // Monday=1..Sunday=7 -> Sunday-first grid
    val totalDays = month.lengthOfMonth()
    val weekdayLabels = listOf("D", "S", "T", "Q", "Q", "S", "S")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
            .padding(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevMonth, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(18.dp)) }
                Text(
                    month.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() } + " ${month.year}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                IconButton(onClick = onNextMonth, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp)) }
            }
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(if (showFreeSlots) Primary.copy(alpha = 0.12f) else Color.Transparent)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraLarge)
                    .clickable(onClick = onToggleFreeSlots)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Horários livres", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = if (showFreeSlots) Primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { w ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(w, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = TextMuted)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        val cells = (List(leadingBlanks) { null } + (1..totalDays).map { firstOfMonth.plusDays((it - 1).toLong()) })
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day != null) {
                            val isSelected = day == selectedDate
                            val isToday = day == today
                            val hasAppts = (countsByDay[day] ?: 0) > 0
                            val showAsFree = showFreeSlots && !hasAppts
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> Primary
                                            showAsFree -> Primary.copy(alpha = 0.10f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .then(if (isToday && !isSelected) Modifier.border(1.dp, Primary, CircleShape) else Modifier)
                                    .clickable { onDaySelected(day) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "${day.dayOfMonth}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal),
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (hasAppts) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 1.dp)
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color.White else Primary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun WeekStrip(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val weekStart = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() - 1)
    val days = (0..6).map { weekStart.plusDays(it.toLong()) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            days.forEach { date ->
                val isSelected = date == selectedDate
                val isToday = date == LocalDate.now()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Primary else Color.Transparent)
                        .clickable { onDateSelected(date) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("pt", "BR")).take(3),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isToday && !isSelected) Primary.copy(alpha = 0.15f) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal),
                            color = if (isSelected) Color.White else if (isToday) Primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        HorizontalDivider()
    }
}

/** Status changes a doctor can make from a given state. Terminal states (cancelled,
 * completed, no-show) offer none — reopening one is a deliberate re-booking, not a
 * tap on a chip. */
private fun nextStatusOptions(current: AppointmentStatus): List<AppointmentStatus> = when (current) {
    AppointmentStatus.SCHEDULED -> listOf(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED)
    AppointmentStatus.CONFIRMED -> listOf(AppointmentStatus.IN_PROGRESS, AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW)
    AppointmentStatus.IN_PROGRESS -> listOf(AppointmentStatus.COMPLETED)
    else -> emptyList()
}

@Composable
private fun AppointmentCard(appt: Appointment, onStatusChange: (AppointmentStatus) -> Unit, onAttend: () -> Unit) {
    val status = AppointmentStatus.entries.find { it.name == appt.status } ?: AppointmentStatus.SCHEDULED
    val type = AppointmentType.entries.find { it.name == appt.type } ?: AppointmentType.ACUPUNCTURE
    val statusColor = Color(status.color)
    val options = nextStatusOptions(status)
    var menuExpanded by remember { mutableStateOf(false) }
    val canAttend = status == AppointmentStatus.SCHEDULED || status == AppointmentStatus.CONFIRMED || status == AppointmentStatus.IN_PROGRESS
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.width(52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(appt.time, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Primary))
                Text("${appt.durationMin}min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(modifier = Modifier.padding(horizontal = 8.dp).width(4.dp).height(48.dp).clip(RoundedCornerShape(2.dp)).background(statusColor))
            Column(Modifier.weight(1f)) {
                Text(appt.patientName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(type.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Sessão ${appt.sessionNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (canAttend) {
                IconButton(onClick = onAttend) {
                    Icon(Icons.Default.PlayCircle, contentDescription = "Atender", tint = Primary)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Box {
                    SuggestionChip(
                        onClick = { if (options.isNotEmpty()) menuExpanded = true },
                        label = { Text(status.label, style = MaterialTheme.typography.labelSmall) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = statusColor.copy(alpha = 0.1f))
                    )
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    menuExpanded = false
                                    onStatusChange(option)
                                }
                            )
                        }
                    }
                }
                if (appt.valueBrl > 0) Text("R$ %.0f".format(appt.valueBrl), style = MaterialTheme.typography.labelSmall, color = if (appt.paid) Color(0xFF4CAF50) else Color(0xFFFF8A65))
            }
        }
    }
}

@Composable
private fun NewAppointmentDialog(onDismiss: () -> Unit, onSave: (Long, String, String, Double, AppointmentType) -> Unit) {
    var patientIdText by remember { mutableStateOf("") }
    var patientName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AppointmentType.ACUPUNCTURE) }
    var time by remember { mutableStateOf("09:00") }
    var value by remember { mutableStateOf("150") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Consulta", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = patientIdText, onValueChange = { patientIdText = it.filter { ch -> ch.isDigit() } }, label = { Text("ID Paciente") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = patientName, onValueChange = { patientName = it }, label = { Text("Paciente") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Horário (HH:mm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = value, onValueChange = { value = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Valor (R$)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Tipo:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AppointmentType.entries) { type ->
                        FilterChip(selected = selectedType == type, onClick = { selectedType = type }, label = { Text(type.label, style = MaterialTheme.typography.labelSmall) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val pid = patientIdText.toLongOrNull() ?: 0L
                val valueBrl = value.toDoubleOrNull() ?: 0.0
                onSave(pid, patientName, time, valueBrl, selectedType)
            }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
