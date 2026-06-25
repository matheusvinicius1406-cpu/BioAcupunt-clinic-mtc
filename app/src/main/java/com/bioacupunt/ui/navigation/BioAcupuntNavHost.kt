package com.bioacupunt.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun BioAcupuntNavHost(
    navController: NavHostController = rememberNavController()
) {
    val start = "biblioteca"
    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(
                    Screen("biblioteca", "Biblioteca", Icons.Default.Book),
                    Screen("simulador", "Simulador", Icons.Default.Science),
                    Screen("prontuario", "Prontu\u00e1rio", Icons.Default.MedicalServices),
                    Screen("flashcards", "Flashcards", Icons.Default.Dashboard),
                    Screen("analytics", "Analytics", Icons.Default.Analytics)
                ).forEach { screen ->
                    NavigationBarItem(
                        selected = navController.currentDestination?.route == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            composable(Screen.Biblioteca.route) { com.bioacupunt.ui.screens.BibliotecaScreen() }
            composable(Screen.Simulador.route) { com.bioacupunt.ui.screens.SimuladorScreen() }
            composable(Screen.Prontuario.route) { com.bioacupunt.ui.screens.ProntuarioScreen() }
            composable(Screen.Flashcards.route) { com.bioacupunt.ui.screens.FlashcardsScreen() }
            composable(Screen.Analytics.route) { com.bioacupunt.ui.screens.AnalyticsScreen() }
        }
    }
}

private data class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
