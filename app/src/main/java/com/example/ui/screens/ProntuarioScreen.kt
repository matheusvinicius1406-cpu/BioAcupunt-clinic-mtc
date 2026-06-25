package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProntuarioScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Prontuário", style = MaterialTheme.typography.headlineMedium)
    }
}
