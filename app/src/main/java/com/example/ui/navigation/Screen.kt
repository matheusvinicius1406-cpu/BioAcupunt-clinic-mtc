package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Biblioteca : Screen("biblioteca", "Biblioteca", Icons.Default.LibraryBooks)
    object Simulador : Screen("simulador", "Simulador", Icons.Default.Science)
    object Prontuario : Screen("prontuario", "Prontuário", Icons.Default.Healing)
    object Flashcards : Screen("flashcards", "Flashcards", Icons.Default.Style)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.Analytics)
    object Ajustes : Screen("ajustes", "Ajustes", Icons.Default.Settings)
}
