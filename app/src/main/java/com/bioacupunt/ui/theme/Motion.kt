package com.bioacupunt.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Elevation and motion for the "Supremo" look.
 *
 * Two rules shape everything here, and they come from who uses this app:
 *
 * 1. **Depth carries meaning.** Elevation says "this sits above that" — a card
 *    above the page, a dialog above the card. It is a hierarchy cue, not
 *    decoration, so the steps are few and clearly distinct rather than a smooth
 *    ramp of near-identical shadows.
 *
 * 2. **Motion never delays information.** Animations here are short (≤220ms) and
 *    never gate content behind themselves. A doctor with a patient on the table
 *    is reading, not watching. Nothing in this file animates a clinical warning:
 *    a contraindication appears immediately, at full contrast, or the safety
 *    invariants in CLAUDE.md are broken.
 */

// ── Elevation ────────────────────────────────────────────────────────────────

/**
 * Ambient + spot shadow pair.
 *
 * A single shadow colour reads flat and grey. Real depth comes from a wide, soft
 * ambient occlusion plus a tighter directional spot — which is what the warm,
 * low-alpha pair below approximates. The tint is warm (matching the cream
 * background) rather than pure black, so shadows sit *in* the palette instead of
 * greying it out.
 */
object Elevation {
    /** Resting cards, list rows. */
    val Card: Dp = 10.dp

    /** Cards being pressed or dragged, and anything that should read as lifted. */
    val Raised: Dp = 20.dp

    /** Floating action buttons and other persistent overlays. */
    val Floating: Dp = 18.dp

    /** Dialogs, bottom sheets — the top of the stack. */
    val Overlay: Dp = 28.dp

    /** Warm spot tint. Pure black turns the cream palette grey. */
    val SpotTint: Color = Color(0xFF1E1B16).copy(alpha = 0.16f)

    /** Wider, softer, lighter — the ambient half of the pair. */
    val AmbientTint: Color = Color(0xFF1E1B16).copy(alpha = 0.07f)
}

/**
 * The project's standard card surface: warm layered shadow at a chosen depth.
 *
 * Prefer this over a bare `Modifier.shadow` so depth stays consistent across
 * screens — inconsistent elevation is what makes an interface feel assembled
 * rather than designed.
 */
@Composable
fun Modifier.supremeShadow(
    shape: Shape = MaterialTheme.shapes.large,
    elevation: Dp = Elevation.Card,
): Modifier = this
    .shadow(
        elevation = elevation * 1.6f,
        shape = shape,
        ambientColor = Elevation.AmbientTint,
        spotColor = Elevation.AmbientTint,
    )
    .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = Elevation.SpotTint,
        spotColor = Elevation.SpotTint,
    )

// ── Motion ───────────────────────────────────────────────────────────────────

/** Springs, not linear tweens: physical settling reads as quality. */
object Motion {
    /** For scale/press feedback — snappy, barely any overshoot. */
    fun <T> press() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** For content entering the screen. */
    fun <T> enter() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    const val EnterDurationMs = 220
    const val StaggerStepMs = 40

    /**
     * Entry transition for list items and cards: a short rise with a fade.
     *
     * [index] staggers items so a list assembles in sequence instead of all at
     * once. Capped deliberately — an un-capped stagger means the twentieth row
     * arrives a second late, which stops being elegant and starts being a wait.
     */
    fun listItemEnter(index: Int = 0): EnterTransition {
        val delay = (index * StaggerStepMs).coerceAtMost(240)
        return fadeIn(
            animationSpec = tween(EnterDurationMs, delayMillis = delay, easing = FastOutSlowInEasing)
        ) + slideInVertically(
            animationSpec = tween(EnterDurationMs, delayMillis = delay, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 6 },
        )
    }
}

/**
 * Press feedback: the surface dips slightly and its shadow tightens, as though
 * pushed toward the page.
 *
 * Touch targets stay ≥48dp — this only changes how the surface is *drawn*, never
 * how large it is to hit. Shrinking the hit area on press would make a
 * one-handed tap slide off its own target.
 */
@Composable
fun Modifier.pressable(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.975f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = Motion.press(),
        label = "pressScale",
    )
    return this.scale(scale)
}

/**
 * A card that responds to touch: lifts on press and settles back.
 *
 * Combines [supremeShadow] and [pressable] so callers get consistent depth and
 * feedback from one modifier instead of re-deriving both at each call site.
 *
 * [interactionSource] **must be the same instance passed to the `clickable`** on
 * this element. Letting this modifier create its own would give the card a
 * private source that no click ever reaches — the animation would compile, run,
 * and never fire, which is the kind of dead code that survives review because
 * nothing about it looks wrong.
 */
@Composable
fun Modifier.interactiveCard(
    interactionSource: MutableInteractionSource,
    shape: Shape = MaterialTheme.shapes.large,
    resting: Dp = Elevation.Card,
    pressedElevation: Dp = Elevation.Raised,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val elevation by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (pressed) pressedElevation else resting,
        animationSpec = Motion.press(),
        label = "cardElevation",
    )
    this
        .pressable(interactionSource)
        .supremeShadow(shape = shape, elevation = elevation)
}
