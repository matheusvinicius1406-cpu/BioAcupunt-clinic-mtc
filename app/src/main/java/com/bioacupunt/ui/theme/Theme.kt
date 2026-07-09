package com.bioacupunt.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightGlassBackground = Color(0xFFE8F5E9)
private val LightGlassSurface = Color(0xAAFFFFFF)
private val LightGlassSurfaceVariant = Color(0x9AFFFFFF)

private val DarkGlassBackground = Color(0xFF06150A)
private val DarkGlassSurface = Color(0xAA0D1F14)
private val DarkGlassSurfaceVariant = Color(0x8A11231A)

@Composable
fun BioAcupuntTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Primary,
            onPrimary = OnPrimary,
            secondary = PrimaryDark,
            background = DarkGlassBackground,
            surface = DarkGlassSurface,
            onSurface = Color(0xE6E8F5E9),
            surfaceVariant = DarkGlassSurfaceVariant,
            onSurfaceVariant = Color(0xB3D7E8D3)
        )
    } else {
        lightColorScheme(
            primary = Primary,
            onPrimary = OnPrimary,
            secondary = PrimaryDark,
            background = LightGlassBackground,
            surface = LightGlassSurface,
            onSurface = OnSurface,
            surfaceVariant = LightGlassSurfaceVariant,
            onSurfaceVariant = OnSurfaceVariant
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
