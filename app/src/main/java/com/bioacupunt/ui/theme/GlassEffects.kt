package com.bioacupunt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Soft card/button elevation shadow — matches the HTML mockups' box-shadow on
// cards (0 8px 32px rgba(30,27,22,.08)). The dark-theme glassmorphism helpers
// (glassSurface/GlassButton/LiquidGlassCard — translucent white over near-black)
// were removed: they read as nearly invisible on the cream/white "Supremo" look
// the HTML mockups define, since there's no dark surface for the glass to sit on.
@Composable
fun Modifier.premiumShadow(
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = Color.Black.copy(alpha = 0.10f),
    elevationDp: Dp = 16.dp
): Modifier = this.shadow(elevationDp, shape = shape, spotColor = color)
