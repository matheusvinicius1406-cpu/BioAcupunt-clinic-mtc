package com.bioacupunt.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bioacupunt.di.AppContainer
import com.bioacupunt.ui.screens.*
import com.bioacupunt.ui.theme.Accent
import com.bioacupunt.ui.theme.Primary
import com.bioacupunt.ui.theme.TextMuted

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

    val showBottomBar = currentRoute != null &&
        currentRoute != Screen.Login.route &&
        currentRoute != Screen.PinLock.route

    // Gate de entrada OFFLINE: o PIN local é a porta do app, sem depender de backend
    // nem de internet. A própria PinLockScreen decide entre criar o PIN (1º uso) e
    // desbloquear (PIN/biometria). O login por e-mail/senha do backend continua
    // existindo como rota, mas não é mais o portão de abertura.
    val startDestination = remember { Screen.PinLock.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
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
                            alwaysShowLabel = false,
                            // No filled pill behind the icon — the mockup's active
                            // indicator is a thin bar above the icon, closer to
                            // "icon/label change color" than a Material default pill.
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Primary,
                                selectedTextColor = Primary,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = Color.Transparent,
                            ),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // Quick access to the one AI surface in the app — same gated RAG chat as
            // the Inteligência tab, just reachable from anywhere in the bottom nav.
            if (showBottomBar && currentRoute != Screen.AiAssistant.route) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.AiAssistant.route) },
                    containerColor = Color.Transparent,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    modifier = androidx.compose.ui.Modifier
                        .size(56.dp)
                        .background(Brush.linearGradient(listOf(Primary, Accent)), androidx.compose.foundation.shape.CircleShape),
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Consultar IA", tint = Color.White)
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding),
            // Sem isto, navigation-compose aplica um cross-fade padrão de 700 ms em TODA
            // troca de tela/aba — a "lentidão entre páginas" que a médica sentia. Um fade
            // curto de 150 ms deixa a navegação instantânea sem cortar a suavidade.
            enterTransition = { fadeIn(animationSpec = tween(150)) },
            exitTransition = { fadeOut(animationSpec = tween(150)) },
            popEnterTransition = { fadeIn(animationSpec = tween(150)) },
            popExitTransition = { fadeOut(animationSpec = tween(150)) },
        ) {
            composable(Screen.Login.route) {
                LoginScreen(onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.PinLock.route) {
                com.bioacupunt.ui.lock.PinLockScreen(onUnlocked = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.PinLock.route) { inclusive = true }
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
                AgendaScreen(onOpenAtendimento = { apptId -> navController.navigate(Screen.Atendimento.routeFor(apptId)) })
            }
            composable(Screen.Biblioteca.route) {
                BibliotecaScreen(
                    onNavigateToFlashcards = { navController.navigate(Screen.Flashcards.route) },
                    onNavigateToSimulador  = { navController.navigate(Screen.Simulador.route) },
                    onNavigateToAnalytics  = { navController.navigate(Screen.Analytics.route) },
                    onNavigateToCuradoria  = { navController.navigate(Screen.Curadoria.route) },
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
                ProntuarioScreen(
                    onBack = { navController.popBackStack() },
                    onOpenEvolucao = { openPid -> navController.navigate(Screen.Evolucao.routeFor(openPid)) },
                    patientId = pid
                )
            }
            composable(
                route = Screen.Evolucao.route,
                arguments = listOf(androidx.navigation.navArgument("patientId") { type = androidx.navigation.NavType.LongType })
            ) { entry ->
                val pid = entry.arguments?.getLong("patientId") ?: 0L
                EvolucaoScreen(patientId = pid, onBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.Atendimento.route,
                arguments = listOf(androidx.navigation.navArgument("appointmentId") { type = androidx.navigation.NavType.LongType })
            ) { entry ->
                val apptId = entry.arguments?.getLong("appointmentId") ?: 0L
                AtendimentoScreen(
                    appointmentId = apptId,
                    onBack = { navController.popBackStack() },
                    onFinalized = { navController.popBackStack() },
                )
            }
            composable(Screen.Flashcards.route)  { FlashcardsScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.Curadoria.route)   { CuradoriaScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.Analytics.route)   { AnalyticsScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.Simulador.route)   { SimuladorScreen() }
            composable(Screen.AiAssistant.route) { AiAssistantScreen(onNavigateToCRM = { navController.navigate(Screen.CRM.route) }) }
            composable(Screen.Relatorios.route)  { RelatoriosScreen() }
        }
    }
}
