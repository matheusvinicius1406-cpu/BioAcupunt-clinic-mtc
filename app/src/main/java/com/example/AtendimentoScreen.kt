package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AtendimentoScreen() {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Nova Ficha de Anamnese Integrada", style = MaterialTheme.typography.titleLarge)
            Text("Preencha detalhadamente os dados psicossomáticos, as zonas álgicas e organize os síndromes de acordo com as teorias do Yin-Yang, Substâncias Vitais e Meridianos de Energia.", style = MaterialTheme.typography.bodyMedium)
        }
        
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Bloco I • Identificação Cadastral", style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(value = "", onValueChange = {}, label = { Text("Nome Completo") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = "", onValueChange = {}, label = { Text("Data de Nascimento") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = "", onValueChange = {}, label = { Text("Queixa Principal / Motivo da Consulta") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                }
            }
        }
        
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Bloco II • Inspeção Visual", style = MaterialTheme.typography.titleMedium)
                    Text("FORMA CORPORAL", style = MaterialTheme.typography.labelSmall)
                    listOf("Abundante em Yang (Forma 1)", "Abundante em Yin (Forma 2)", "Deficiente em Yang (Forma 3)", "Deficiente em Yin (Forma 4)", "Equilíbrio Yin/Yang (Forma 5)").forEach {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(selected = it.startsWith("Equilíbrio"), onClick = {})
                            Text(it)
                        }
                    }
                }
            }
        }
        
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Bloco III • Avaliação Detalhada da Dor", style = MaterialTheme.typography.titleMedium)
                    Text("ESCALA VISUAL ANALÓGICA (EVA) DE INTENSIDADE DA DOR", style = MaterialTheme.typography.labelSmall)
                    Slider(value = 5f, onValueChange = {}, valueRange = 0f..10f, steps = 9)
                }
            }
        }
    }
}
