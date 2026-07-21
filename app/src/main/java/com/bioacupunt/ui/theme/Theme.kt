package com.bioacupunt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.bioacupunt.security.SecurePreferences

// The design mockup ships BOTH palettes (lightVars/darkVars) plus a theme toggle
// in the header, so the app supports the same: warm-light "Supremo" by default,
// with the mockup's dark palette one tap away, persisted across launches.
private val LightColors = lightColorScheme(
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

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = OnPrimary,
    primaryContainer = DarkPrimaryContainer,
    secondary = DarkAccent,
    onSecondary = OnAccent,
    secondaryContainer = DarkAccentContainer,
    tertiary = DarkAccent,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = DarkError,
    errorContainer = DarkErrorBg,
)

/** Single source of truth for the light/dark toggle (mockup's header button). */
object ThemeController {
    val dark = mutableStateOf(false)

    fun load(prefs: SecurePreferences) {
        dark.value = runCatching { prefs.darkModeEnabled }.getOrDefault(false)
    }

    fun toggle(prefs: SecurePreferences) {
        dark.value = !dark.value
        runCatching { prefs.darkModeEnabled = dark.value }
    }
}

@Composable
fun BioAcupuntTheme(
    content: @Composable () -> Unit
) {
    val dark by ThemeController.dark
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
