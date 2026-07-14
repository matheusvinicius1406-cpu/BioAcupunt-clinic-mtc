package com.bioacupunt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// The reference clinical platform is a single, warm-light "Supremo" look: olive
// green + gold over cream. The app commits to it too (light-only) — matches the
// design mockups (dashboard/patients/prontuario/.../ajustes) screen for screen.
private val BioAcupuntColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    secondary = Accent,
    onSecondary = OnAccent,
    secondaryContainer = AccentContainer,
    tertiary = Accent,
    background = Background,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    error = SemanticError,
    errorContainer = SemanticErrorBg,
)

@Composable
fun BioAcupuntTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BioAcupuntColors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
