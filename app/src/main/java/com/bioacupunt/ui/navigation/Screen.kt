package com.bioacupunt.ui.navigation

sealed class Screen(val route: String, val label: String) {
    data object Biblioteca : Screen("biblioteca", "Biblioteca")
    data object Simulador : Screen("simulador", "Simulador")
    data object Prontuario : Screen("prontuario", "Prontuário")
    data object Flashcards : Screen("flashcards", "Flashcards")
    data object Analytics : Screen("analytics", "Analytics")
}
