package com.example.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AvaliacaoScreen() {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Avaliação Diagnóstica", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Resultados do Motor de Inferência", fontWeight = FontWeight.Bold)
                    
                    Text("Padrão Identificado: P002 (Estagnação de Qi)", color = MaterialTheme.colorScheme.primary)
                    Text("Confiança: 0.85")
                    
                    Divider()
                    
                    Text("Sintomas Correlacionados:", fontWeight = FontWeight.Bold)
                    Text("- S001: Dor em hipocôndrio")
                    Text("- S003: Irritabilidade")
                    
                    Divider()
                    
                    Button(onClick = { /* TODO */ }, modifier = Modifier.fillMaxWidth()) {
                        Text("Confirmar Diagnóstico e Gerar Tratamento")
                    }
                }
            }
        }
    }
}
