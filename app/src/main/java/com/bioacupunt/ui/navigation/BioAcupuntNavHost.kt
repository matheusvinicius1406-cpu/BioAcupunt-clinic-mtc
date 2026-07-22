package com.bioacupunt.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bioacupunt.di.AppContainer
import com.bioacupunt.ui.lock.BiometricLockScreen
import com.bioacupunt.ui.screens.*
import com.bioacupunt.ui.theme.ThemeController
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class BottomNavItem(val screen: Screen, val icon: ImageVector, val label: String)

private val bottomItems = listOf(
    BottomNavItem(Screen.Dashboard, Icons.Default.Home, "Início"),
    BottomNavItem(Screen.CRM, Icons.Default.Group, "Pacientes"),
    BottomNavItem(Screen.Prontuario, Icons.AutoMirrored.Filled.Assignment, "Prontuário"),
    BottomNavItem(Screen.Biblioteca, Icons.Default.AutoStories, "Biblioteca"),
)

private data class MoreItem(val label: String, val icon: ImageVector, val route: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BioAcupuntNavHost(
    navController: NavHostController = rememberNavController()
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showShell = currentRoute != null && currentRoute != Screen.Login.route && currentRoute != Screen.BiometricLock.route
    var moreOpen by remember { mutableStateOf(false) }

    // Auth é 100% local (sem servidor): o app trava a cada abertura fria e a
    // LoginScreen resolve os dois casos — criar a conta local no primeiro uso ou
    // destravar com PIN/biometria depois. Sempre começar no Login é o próprio gate:
    // dado clínico não pode ficar acessível só porque uma sessão antiga existia.
    val startDestination = Screen.Login.route

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo(Screen.Dashboard.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val moreItems = remember {
        listOf(
            MoreItem("Agenda", Icons.Default.CalendarMonth, Screen.Agenda.route),
            MoreItem("Atendimento", Icons.Default.Healing, Screen.Agenda.route),
            MoreItem("Evolução", Icons.AutoMirrored.Filled.TrendingUp, Screen.Evolucao.routeFor(0)),
            MoreItem("Inteligência", Icons.Default.SmartToy, Screen.AiAssistant.route),
            MoreItem("Financeiro", Icons.Default.AccountBalance, Screen.Financeiro.route),
            MoreItem("Relatórios", Icons.Default.Description, Screen.Relatorios.route),
            MoreItem("Ajustes", Icons.Default.Settings, Screen.Ajustes.route),
            MoreItem("Conflitos", Icons.Default.SyncProblem, Screen.Conflitos.route),
            MoreItem("Curadoria", Icons.Default.Inbox, Screen.Curadoria.route),
            MoreItem("Pipeline", Icons.Default.Construction, Screen.PipelineMonitor.route),
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { if (showShell) SupremoHeader(onAvatarClick = { navigateTab(Screen.CRM.route) }) },
        bottomBar = {
            if (showShell) {
                SupremoBottomNav(
                    currentRoute = currentRoute,
                    onNavigate = ::navigateTab,
                    onMore = { moreOpen = true },
                )
            }
        },
        floatingActionButton = {
            // Quick access to the one AI surface in the app — the same gated RAG chat
            // as the Inteligência screen, reachable from anywhere (mockup's AI FAB).
            if (showShell && currentRoute != Screen.AiAssistant.route) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
                        .clickable { navController.navigate(Screen.AiAssistant.route) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Consultar IA", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            // imePadding: with enableEdgeToEdge() the window does not resize for
            // the keyboard, so without this the field being typed into sits
            // underneath it and buttons below become untappable.
            modifier = Modifier.padding(innerPadding).imePadding(),
            // Screens cross-fade with a short lateral drift rather than cutting.
            // Deliberately quick (200ms in, 180ms out): motion here is a spatial
            // cue about where the new screen came from, not a performance. A
            // doctor navigating between a chart and the agenda mid-consultation
            // must never wait on an animation to read what is on screen.
            enterTransition = {
                fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(
                        animationSpec = tween(200, easing = FastOutSlowInEasing),
                        initialOffsetX = { it / 12 },
                    )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(
                        animationSpec = tween(200, easing = FastOutSlowInEasing),
                        initialOffsetX = { -it / 12 },
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing))
            },
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
                    onNavigateToCRM        = { navigateTab(Screen.CRM.route) },
                    onNavigateToRelatorios = { navController.navigate(Screen.Relatorios.route) },
                    onNavigateToAI         = { navController.navigate(Screen.AiAssistant.route) }
                )
            }
            composable(Screen.CRM.route) {
                // No explicit viewModel passed: CrmScreen's own default wires
                // AppContainer.crmViewModelFactory, which (unlike a manual
                // construction here) actually supplies repository/tenantManager.
                CrmScreen(
                    onNavigateToProntuario = { pid -> navController.navigate(Screen.Prontuario.routeFor(pid)) }
                )
            }
            composable(Screen.Agenda.route) {
                AgendaScreen(onOpenAtendimento = { apptId -> navController.navigate(Screen.Atendimento.routeFor(apptId)) })
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
                ProntuarioScreen(
                    onBack = { navController.popBackStack() },
                    onOpenEvolucao = { openPid -> navController.navigate(Screen.Evolucao.routeFor(openPid)) },
                    onOpenAtendimento = { navController.navigate(Screen.Agenda.route) },
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
            composable(Screen.Financeiro.route)  { FinanceiroScreen() }
            composable(Screen.Conflitos.route)   { ConflitosScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.Curadoria.route)   { CuradoriaScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.PipelineMonitor.route) {
                val factory = androidx.compose.runtime.remember { AppContainer.pipelineMonitorViewModelFactory() }
                com.bioacupunt.ui.screens.pipelineMonitorFactory = factory
                PipelineMonitorScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Flashcards.route)  { FlashcardsScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.Analytics.route)   { AnalyticsScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.Simulador.route)   { SimuladorScreen() }
            composable(Screen.AiAssistant.route) { InteligenciaScreen(onNavigateToCRM = { navigateTab(Screen.CRM.route) }) }
            composable(Screen.Relatorios.route)  { RelatoriosScreen() }
        }
    }

    if (moreOpen) {
        ModalBottomSheet(
            onDismissRequest = { moreOpen = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 22.dp).padding(bottom = 30.dp)) {
                moreItems.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        row.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.background)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                                    .clickable {
                                        moreOpen = false
                                        navController.navigate(item.route) { launchSingleTop = true }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(item.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                Text(item.label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Mockup header: ☯ logo, "Olá, Dra. …", date · Supremo badge, sync chip, theme toggle, avatar. */
@Composable
private fun SupremoHeader(onAvatarClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val userName = remember { AppContainer.authRepository.getCurrentUser()?.name.orEmpty() }
    val today = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEE., d 'de' MMMM", Locale("pt", "BR")))
    }
    val dark by ThemeController.dark

    Column(modifier = Modifier.fillMaxWidth().background(cs.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Brush.linearGradient(listOf(cs.primary, cs.secondary))),
                contentAlignment = Alignment.Center,
            ) {
                Text("☯", color = Color.White, fontSize = 18.sp)
            }
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (userName.isBlank()) "Olá" else "Olá, $userName",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row {
                    Text(today, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    Text(" · ", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    Text("Supremo", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = cs.secondary)
                }
            }
            SyncChip()
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { ThemeController.toggle(AppContainer.securePreferences) }, modifier = Modifier.size(34.dp)) {
                Icon(
                    if (dark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Tema",
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Box {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(cs.primaryContainer)
                        .border(2.dp, cs.secondary, CircleShape)
                        .clickable(onClick = onAvatarClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        initialsOf(userName),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = cs.primary,
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(cs.error)
                        .border(2.dp, cs.surface, CircleShape)
                )
            }
        }
        HorizontalDivider(color = cs.outline)
    }
}

@Composable
private fun SyncChip() {
    val cs = MaterialTheme.colorScheme
    val pulse = rememberInfiniteTransition(label = "sync")
    val a by pulse.animateFloat(0.4f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "dot")
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(cs.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).alpha(a).background(cs.primary))
        Text("sync", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = cs.primary)
    }
}

/** Mockup bottom nav: 5 slots, labels always visible, 3dp indicator bar above the active icon. */
@Composable
private fun SupremoBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onMore: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().background(cs.surface)) {
        HorizontalDivider(color = cs.outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            bottomItems.forEach { item ->
                val selected = currentRoute == item.screen.route
                NavSlot(item.icon, item.label, selected) { onNavigate(navRouteFor(item.screen)) }
            }
            NavSlot(Icons.Default.MoreHoriz, "Mais", selected = false, onClick = onMore)
        }
    }
}

/** Tab items whose route pattern carries an argument need a concrete path to navigate to. */
private fun navRouteFor(screen: Screen): String =
    if (screen == Screen.Prontuario) Screen.Prontuario.routeFor(0) else screen.route

@Composable
private fun NavSlot(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val tint = if (selected) cs.primary else cs.onSurfaceVariant
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .widthIn(min = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) cs.primary else Color.Transparent)
        )
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(25.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = tint)
    }
}

private fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "☯"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}
