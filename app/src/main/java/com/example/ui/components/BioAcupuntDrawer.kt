package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.Screen
import com.example.ui.theme.DarkBlue
import com.example.ui.theme.Gold
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.SwissGreenLight
import com.example.ui.theme.SwissWhite

@Composable
fun BioAcupuntDrawer(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SwissWhite)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                "Olá, Dra. Camila Silva",
                color = Gold,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "terça-feira, 23 de junho de 2026",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Menu items
        listOf(
            Screen.Dashboard,
            Screen.Pacientes,
            Screen.Agenda,
            Screen.Atendimento,
            Screen.Evolucao,
            Screen.Inteligencia,
            Screen.Sinergia,
            Screen.Financeiro,
            Screen.Ajustes
        ).forEach { screen ->
            DrawerItem(
                icon = screen.icon,
                label = screen.label,
                isSelected = currentScreen == screen,
                onClick = { onScreenSelected(screen) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("BIOACUPUNT • SUPREMO EDITION", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("v2.1.0", color = TextSecondary, fontSize = 10.sp)
            Text("Sincronizado com Supabase", color = TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
fun DrawerItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) SwissGreenLight else Color.Transparent)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isSelected) Gold else TextSecondary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            label,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
