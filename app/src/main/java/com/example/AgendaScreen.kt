package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AgendaScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Agenda de Atendimento", style = MaterialTheme.typography.titleLarge)
        Text("Organize os horários de acupuntura clínica, marque novas consultas e inicie os prontuários eletrônicos com apenas um clique.", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        val times = listOf("08:00", "09:00", "10:00", "11:00", "13:00", "14:00", "15:00", "16:00", "17:00")
        
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(times) { time ->
                TimeSlotCard(time = time)
            }
        }
    }
}

@Composable
fun TimeSlotCard(time: String) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(time, style = MaterialTheme.typography.titleMedium)
                Text("LIVRE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Nome completo do paciente...") },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FilterChip(selected = false, onClick = { /*TODO*/ }, label = { Text("Avaliação") })
                FilterChip(selected = false, onClick = { /*TODO*/ }, label = { Text("Retorno") })
                FilterChip(selected = true, onClick = { /*TODO*/ }, label = { Text("Sessão") })
            }
            Button(onClick = { /*TODO*/ }, modifier = Modifier.fillMaxWidth()) {
                Text("Confirmar Agendamento")
            }
        }
    }
}
