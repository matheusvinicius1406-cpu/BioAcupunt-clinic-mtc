package com.bioacupunt.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
            background = Color(0xFF121212),
            surface = Color(0x1DFFFFFF),
            onSurface = Color(0xE6FFFFFF),
            surfaceVariant = Color(0x14FFFFFF),
            onSurfaceVariant = Color(0xBFFFFFFF)
        )
    } else {
        lightColorScheme(
            primary = Primary,
            onPrimary = OnPrimary,
            secondary = PrimaryDark,
            background = Background,
            surface = Surface,
            onSurface = OnSurface,
            surfaceVariant = SurfaceVariant,
            onSurfaceVariant = OnSurfaceVariant
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
