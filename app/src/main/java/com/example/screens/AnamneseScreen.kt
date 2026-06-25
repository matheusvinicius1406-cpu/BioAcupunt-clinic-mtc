package com.example.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AnamneseScreen() {
    var mainComplaint by remember { mutableStateOf("") }
    var bagangDetails by remember { mutableStateOf("") }
    var zangfuDetails by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Anamnese Clínica MTC", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Queixa Principal", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = mainComplaint,
                        onValueChange = { mainComplaint = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Sintomas principais (S001-S006)") },
                        minLines = 3
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Oito Princípios (Ba Gang)", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = bagangDetails,
                        onValueChange = { bagangDetails = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Yin/Yang, Interior/Exterior, Frio/Calor, Deficiência/Excesso") },
                        minLines = 3
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Órgãos e Vísceras (Zang Fu)", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = zangfuDetails,
                        onValueChange = { zangfuDetails = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Desarmonias identificadas") },
                        minLines = 3
                    )
                }
            }
        }

        item {
            Button(onClick = { /* TODO: Submit Anamnesis */ }, modifier = Modifier.fillMaxWidth()) {
                Text("Salvar Anamnese e Iniciar Avaliação")
            }
        }
    }
}
