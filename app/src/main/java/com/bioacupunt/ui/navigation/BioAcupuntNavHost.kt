package com.bioacupunt.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bioacupunt.di.AppContainer
import com.bioacupunt.ui.screens.*
import com.bioacupunt.ui.lock.BiometricLockScreen

private data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val iconSelected: ImageVector = icon
)

private val bottomItems = listOf(
    BottomNavItem(Screen.Dashboard,  Icons.Default.Home,          Icons.Default.Home),
    BottomNavItem(Screen.Agenda,     Icons.Default.CalendarMonth, Icons.Default.CalendarMonth),
    BottomNavItem(Screen.CRM,        Icons.Default.Group,         Icons.Default.Group),
    BottomNavItem(Screen.Biblioteca, Icons.Default.AutoStories,   Icons.Default.AutoStories),
    BottomNavItem(Screen.Ajustes,    Icons.Default.Settings,      Icons.Default.Settings)
)

@Composable
fun BioAcupuntNavHost(
    navController: NavHostController = rememberNavController()
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showBottomBar = currentRoute != null && currentRoute != Screen.Login.route && currentRoute != Screen.BiometricLock.route

    // Biometric lock is opt-in: only gate startup with it when the user has
    // actually enabled it in Settings AND the device supports it AND there is a
    // logged-in session to protect. A fresh/logged-out launch always goes to
    // Login, and a logged-in user who never turned biometrics on goes straight
    // to the Dashboard — the fingerprint prompt is never forced on hardware
    // presence alone.
    val startDestination = remember {
        when {
            !AppContainer.authRepository.isLoggedIn() -> Screen.Login.route
            AppContainer.securePreferences.biometricEnabled && AppContainer.isBiometricAvailable() -> Screen.BiometricLock.route
            else -> Screen.Dashboard.route
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) item.iconSelected else item.icon,
                                    contentDescription = item.screen.label
                                )
                            },
                            label = { Text(item.screen.label) },
                            alwaysShowLabel = false
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.BiometricLock.route) {
                BiometricLockScreen(onUnlocked = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.BiometricLock.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToAgenda     = { navController.navigate(Screen.Agenda.route) },
                    onNavigateToCRM        = { navController.navigate(Screen.CRM.route) },
                    onNavigateToRelatorios = { navController.navigate(Screen.Relatorios.route) },
                    onNavigateToAI         = { navController.navigate(Screen.AiAssistant.route) }
                )
            }
            composable(Screen.CRM.route) {
                // No explicit viewModel passed: CrmScreen's own default wires
                // AppContainer.crmViewModelFactory, which (unlike a manual
                // construction here) actually supplies repository/tenantManager.
                // A previous inline factory here omitted both, so every created
                // CrmPatient got tenantId=0 while the repository validated
                // against the real tenant (1) — every patient save silently
                // threw "Tenant mismatch" and the new-patient dialog just closed
                // with nothing saved.
                CrmScreen(
                    onNavigateToProntuario = { pid -> navController.navigate(Screen.Prontuario.routeFor(pid)) }
                )
            }
            composable(Screen.Agenda.route) {
                // Same reasoning as CRM above: let AgendaScreen's own default
                // resolve AppContainer.agendaViewModelFactory instead of
                // duplicating its construction here.
                AgendaScreen()
            }
            composable(Screen.Biblioteca.route) {
                BibliotecaScreen(
                    onNavigateToFlashcards = { navController.navigate(Screen.Flashcards.route) },
                    onNavigateToSimulador  = { navController.navigate(Screen.Simulador.route) },
                    onNavigateToAnalytics  = { navController.navigate(Screen.Analytics.route) }
                )
            }
            composable(Screen.Ajustes.route)     {
                AjustesScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            // Secondary screens
            composable(
                route = Screen.Prontuario.route,
                arguments = listOf(androidx.navigation.navArgument("patientId") { type = androidx.navigation.NavType.LongType })
            ) { entry ->
                // Without the explicit LongType above, Navigation Compose infers
                // {patientId} as a String argument. Bundle.getLong() on a String
                // value doesn't throw — it swallows the ClassCastException and
                // returns 0, so every patient's prontuário silently opened
                // patient id 0 instead of the one actually tapped.
                val pid = entry.arguments?.getLong("patientId") ?: 0L
                ProntuarioScreen(onBack = { navController.popBackStack() }, patientId = pid)
            }
            composable(Screen.Flashcards.route)  { FlashcardsScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.Analytics.route)   { AnalyticsScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.Simulador.route)   { SimuladorScreen() }
            composable(Screen.AiAssistant.route) { AiAssistantScreen() }
            composable(Screen.Relatorios.route)  { RelatoriosScreen() }
        }
    }
}
