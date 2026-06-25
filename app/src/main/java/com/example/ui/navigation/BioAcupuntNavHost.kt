package com.example.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.MainViewModel
import com.example.ui.components.BioAcupuntDrawer
import com.example.ui.components.BottomNavBar
import com.example.ui.screens.BibliotecaScreen
import com.example.ui.screens.SimuladorScreen
import com.example.ui.screens.ProntuarioScreen
import com.example.ui.screens.FlashcardsScreen
import com.example.ui.screens.AnalyticsScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BioAcupuntNavHost(
    viewModel: MainViewModel,
    navController: NavHostController = rememberNavController()
) {
    val screens = listOf(
        Screen.Biblioteca,
        Screen.Simulador,
        Screen.Prontuario,
        Screen.Flashcards,
        Screen.Analytics,
        Screen.Ajustes
    )
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isIaChatOpen by viewModel.isIaChatOpen.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isIaLoading by viewModel.isIaLoading.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet {
                    BioAcupuntDrawer(
                        currentScreen = currentScreen,
                        onScreenSelected = { screen ->
                            viewModel.navigateTo(screen)
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) {
            Scaffold(
                bottomBar = {
                    BottomNavBar(
                        screens = screens,
                        currentScreen = currentScreen,
                        onScreenSelected = { screen ->
                            viewModel.navigateTo(screen)
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            ) { innerPadding ->
                @OptIn(ExperimentalSharedTransitionApi::class)
                SharedTransitionLayout {
                    val onNavigate: (Screen) -> Unit = { screen ->
                        viewModel.navigateTo(screen)
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Biblioteca.route,
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = {
                            fadeIn(animationSpec = tween(280, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) +
                            scaleIn(initialScale = 0.96f, animationSpec = tween(280, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(180, easing = androidx.compose.animation.core.FastOutLinearInEasing))
                        },
                        popEnterTransition = {
                            fadeIn(animationSpec = tween(280, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) +
                            scaleIn(initialScale = 0.96f, animationSpec = tween(280, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
                        },
                        popExitTransition = {
                            fadeOut(animationSpec = tween(180, easing = androidx.compose.animation.core.FastOutLinearInEasing))
                        }
                    ) {
                        composable(Screen.Biblioteca.route) {
                            BibliotecaScreen()
                        }
                        composable(Screen.Simulador.route) {
                            SimuladorScreen()
                        }
                        composable(Screen.Prontuario.route) {
                            ProntuarioScreen()
                        }
                        composable(Screen.Flashcards.route) {
                            FlashcardsScreen()
                        }
                        composable(Screen.Analytics.route) {
                            AnalyticsScreen()
                        }
                        composable(Screen.Ajustes.route) {
                            AjustesScreen(
                                viewModel = viewModel,
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@composable
                            )
                        }
                    }
                }
            }
        }

        // FLOATING CHAT CARD OVERLAY
        AnimatedVisibility(
            visible = isIaChatOpen,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 150.dp, end = 16.dp)
                .zIndex(99f)
        ) {
            Card(
                modifier = Modifier
                    .width(340.dp)
                    .height(420.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                border = BorderStroke(1.dp, com.example.ui.theme.BorderColor)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Chat Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(com.example.ui.theme.SwissGreenLight)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Psychology,
                                contentDescription = null,
                                tint = com.example.ui.theme.Gold,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    "BioAcupunt AI",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.TextPrimary
                                )
                                Text(
                                    "Clínica Suíça • Online",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = com.example.ui.theme.TextSecondary
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.setIaChatOpen(false) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = com.example.ui.theme.Gold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Chat Messages List
                    val chatListState = rememberLazyListState()
                    Box(modifier = Modifier.weight(1f).padding(8.dp)) {
                        LazyColumn(
                            state = chatListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chatMessages) { msg ->
                                val isUser = msg.role == "user"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(
                                            topStart = 8.dp,
                                            topEnd = 8.dp,
                                            bottomStart = if (isUser) 8.dp else 2.dp,
                                            bottomEnd = if (isUser) 2.dp else 8.dp
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isUser) com.example.ui.theme.Gold else com.example.ui.theme.SwissGreenLight.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.widthIn(max = 240.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            color = if (isUser) Color.White else com.example.ui.theme.TextPrimary,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }

                            if (isIaLoading) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(start = 4.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(10.dp),
                                            strokeWidth = 1.5.dp,
                                            color = com.example.ui.theme.Gold
                                        )
                                        Text(
                                            "Buscando base...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = com.example.ui.theme.TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Chat Input
                    var floatingInputText by remember { mutableStateOf("") }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = com.example.ui.theme.SwissWhite,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = floatingInputText,
                                onValueChange = { floatingInputText = it },
                                placeholder = { Text("Fale com a IA...", color = com.example.ui.theme.TextSecondary, fontSize = 12.sp) },
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = com.example.ui.theme.TextPrimary),
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = com.example.ui.theme.Gold,
                                    unfocusedBorderColor = com.example.ui.theme.BorderColor,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                            IconButton(
                                onClick = {
                                    if (floatingInputText.isNotBlank()) {
                                        viewModel.sendIaChatMessage(floatingInputText)
                                        floatingInputText = ""
                                        coroutineScope.launch {
                                            chatListState.animateScrollToItem(chatMessages.size)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(com.example.ui.theme.Gold, RoundedCornerShape(18.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Send,
                                    contentDescription = "Enviar",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // FLOATING ACTION BUTTON
        FloatingActionButton(
            onClick = { viewModel.toggleIaChat() },
            containerColor = com.example.ui.theme.Gold,
            contentColor = Color.White,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 86.dp, end = 16.dp)
                .zIndex(100f)
        ) {
            Icon(
                imageVector = Icons.Outlined.Psychology,
                contentDescription = "MTC AI Chat",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
