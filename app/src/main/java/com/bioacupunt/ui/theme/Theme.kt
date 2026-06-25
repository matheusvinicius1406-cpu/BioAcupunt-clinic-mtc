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
            primary = SwissGreenPrimary,
            secondary = SwissGreenLight,
            background = SwissBeige,
            surface = SwissWhite,
            onPrimary = SwissWhite,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
        )
    } else {
        lightColorScheme(
            primary = SwissGreenPrimary,
            secondary = SwissGreenLight,
            background = SwissBeige,
            surface = SwissWhite,
            onPrimary = SwissWhite,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}
