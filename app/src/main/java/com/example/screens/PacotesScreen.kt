package com.example.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PacotesScreen() {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Gestão de Pacotes (CRM)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        val packages = listOf(
            Pair("Pacote Tratamento Base (10 sessões)", "R$ 1.200,00"),
            Pair("Acupuntura + Fitoterapia (Mensal)", "R$ 450,00"),
            Pair("Harmonização Facial MTC (5 sessões)", "R$ 800,00")
        )

        for (pkg in packages) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(pkg.first, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Ativo - 8/10 sessões disponíveis", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(pkg.second, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
