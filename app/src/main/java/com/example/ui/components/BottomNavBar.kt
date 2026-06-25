package com.example.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ui.navigation.Screen
import com.example.ui.theme.Gold
import com.example.ui.theme.SwissWhite
import com.example.ui.theme.TextSecondary

@Composable
fun BottomNavBar(
    screens: List<Screen>,
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = SwissWhite,
        contentColor = Gold
    ) {
        val mainScreens = listOf(
            Screen.Dashboard,
            Screen.Pacientes,
            Screen.Agenda,
            Screen.Inteligencia,
            Screen.Ajustes
        )
        mainScreens.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label, fontSize = MaterialTheme.typography.labelSmall.fontSize) },
                selected = currentScreen == screen,
                onClick = { onScreenSelected(screen) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Gold,
                    selectedTextColor = Gold,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = SwissWhite
                )
            )
        }
    }
}
