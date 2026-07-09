package com.bioacupunt.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun Modifier.gradientLiquid(
    colors: List<Color>,
    shape: Shape = MaterialTheme.shapes.large
): Modifier = this
    .clip(shape)
    .background(Brush.verticalGradient(colors))

@Composable
fun Modifier.premiumShadow(
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = Color.Black.copy(alpha = 0.10f),
    elevationDp: Dp = 16.dp
): Modifier = this.shadow(elevationDp, shape = shape, spotColor = color)

@Composable
fun Modifier.glassSurface(
    shape: Shape = MaterialTheme.shapes.large,
    surface: Color = Color.White.copy(alpha = 0.10f),
    stroke: Color = Color.White.copy(alpha = 0.18f),
    elevationDp: Dp = 16.dp
): Modifier = this
    .shadow(elevationDp, shape = shape, spotColor = Color.Black.copy(alpha = 0.10f))
    .clip(shape)
    .background(surface)
    .border(1.dp, stroke, shape)

@Composable
fun Modifier.animateEntrance(initialAlpha: Float = 0f, initialOffsetY: Dp = 10.dp): Modifier {
    val alpha = remember { Animatable(initialAlpha) }
    val offsetY = remember { Animatable(initialOffsetY.value) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(480, easing = LinearOutSlowInEasing))
        offsetY.animateTo(0f, animationSpec = spring(stiffness = 360f, dampingRatio = 0.78f))
    }
    return this.alpha(alpha.value).offset(y = offsetY.value.dp)
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = Primary.copy(alpha = 0.80f),
    contentColor: Color = Color.White,
    disabledContainerColor: Color = containerColor.copy(alpha = 0.35f),
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = lerp(0.97f, 1f, if (pressed) 0f else 1f)
    val elevation = lerp(10f, 20f, if (pressed) 6f else 1f).dp

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (enabled) containerColor else disabledContainerColor)
            .border(1.dp, Color.White.copy(alpha = 0.22f), shape)
            .shadow(elevation, shape = shape, spotColor = if (enabled) Primary.copy(alpha = 0.32f) else Color.Transparent)
            .scale(scale)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    surface: Color = Color.White.copy(alpha = 0.10f),
    stroke: Color = Color.White.copy(alpha = 0.18f),
    waveColor: Color = Primary.copy(alpha = 0.18f),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(16.dp, shape = shape, spotColor = Color.Black.copy(alpha = 0.08f))
            .clip(shape)
            .background(surface)
            .border(1.dp, stroke, shape),
        contentAlignment = Alignment.Center
    ) {
        LiquidWave(shape = shape, waveColor = waveColor)
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), content = content)
    }
}

@Composable
private fun LiquidWave(shape: Shape, waveColor: Color) {
    val phase = rememberInfiniteTransition(label = "wave")
    val angle by phase.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(7000), RepeatMode.Restart),
        label = "phase"
    )
    Canvas(modifier = Modifier
        .fillMaxSize()
        .clip(shape)
    ) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, h)
            val k = 60f
            for (x in 0..(w.toInt())) {
                val y = h * 0.55f + (h * 0.18f) * sin(x / k + angle)
                lineTo(x.toFloat(), y)
            }
            lineTo(w, h)
            close()
        }
        drawPath(path = path, color = waveColor, style = Fill)
    }
}
