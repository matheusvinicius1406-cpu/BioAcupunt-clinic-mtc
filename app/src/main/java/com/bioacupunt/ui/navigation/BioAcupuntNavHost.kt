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
import com.bioacupunt.security.AppHardening
import com.bioacupunt.security.AuthThrottle
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
                val vm: com.bioacupunt.crm.presentation.CrmViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.bioacupunt.crm.presentation.CrmViewModelFactory(
                        saveCrmPatient = com.bioacupunt.crm.domain.usecase.SaveCrmPatient(
                            com.bioacupunt.di.AppContainer.crmPatientRepository
                        ),
                        updateCrmStage = com.bioacupunt.crm.domain.usecase.UpdateCrmStage(
                            com.bioacupunt.di.AppContainer.crmPatientRepository
                        ),
                        searchCrmPatients = com.bioacupunt.crm.domain.usecase.SearchCrmPatients(
                            com.bioacupunt.di.AppContainer.crmPatientRepository
                        )
                    )
                )
                CrmScreen(
                    onNavigateToProntuario = { pid -> navController.navigate("${Screen.Prontuario.route}/$pid") },
                    viewModel = vm
                )
            }
            composable(Screen.Agenda.route) {
                val vm: com.bioacupunt.agenda.presentation.AgendaViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.bioacupunt.agenda.presentation.AgendaViewModelFactory(
                        getAppointmentsByDate = com.bioacupunt.agenda.domain.usecase.GetAppointmentsByDate(
                            com.bioacupunt.di.AppContainer.appointmentRepository
                        ),
                        saveAppointment = com.bioacupunt.agenda.domain.usecase.SaveAppointment(
                            com.bioacupunt.di.AppContainer.appointmentRepository
                        ),
                        updateStatus = com.bioacupunt.agenda.domain.usecase.UpdateAppointmentStatus(
                            com.bioacupunt.di.AppContainer.appointmentRepository
                        ),
                        calculateDayStats = com.bioacupunt.agenda.domain.usecase.CalculateDayStats(
                            com.bioacupunt.di.AppContainer.appointmentRepository
                        )
                    )
                )
                AgendaScreen(viewModel = vm)
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
            composable(Screen.Prontuario.route) {
                val entry = navController.currentBackStackEntry
                val pid = entry?.arguments?.getLong("patientId") ?: 0L
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
