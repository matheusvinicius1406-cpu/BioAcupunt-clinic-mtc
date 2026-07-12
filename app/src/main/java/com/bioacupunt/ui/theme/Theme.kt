package com.bioacupunt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// The reference clinical platform is a single, dark "Supremo" look. The app
// commits to it too (dark-only) — this avoids a washed-out light mode and keeps
// the emerald/gold-on-near-black identity consistent across every screen.
private val BioAcupuntColors = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    secondary = Accent,
    onSecondary = OnAccent,
    tertiary = Accent,
    background = Background,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = OnSurfaceVariant,
    error = SemanticError,
)

@Composable
fun BioAcupuntTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BioAcupuntColors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
