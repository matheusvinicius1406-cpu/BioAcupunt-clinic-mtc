package com.bioacupunt.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ── BioAcupunt "Supremo" palette ─────────────────────────────
// Olive-green primary + gold accent over a warm cream, matching the
// design mockups (dashboard/patients/prontuario/.../ajustes).

val Primary = Color(0xFF4A6B2A)        // olive — nav active, buttons, chips
val PrimaryDark = Color(0xFF6B8F4A)    // olive-light
val PrimaryContainer = Color(0xFFEAF3E0) // olive-soft
val OnPrimary = Color(0xFFFFFFFF)

val Accent = Color(0xFFC9A96E)         // gold — secondary highlight, badges
val AccentDark = Color(0xFFE8D5A8)     // gold-light
val OnAccent = Color(0xFF1E1B16)
val AccentContainer = Color(0xFFF8F0E0) // gold-soft

val Background = Color(0xFFF6F5F2)     // warm cream
val Surface = Color(0xFFFFFFFF)        // cards
val SurfaceVariant = Color(0xFFE6E2DA) // border-color doubles as subtle fill
val OnSurface = Color(0xFF1E1B16)      // text-primary
val OnSurfaceVariant = Color(0xFF5A5548) // text-secondary
val TextMuted = Color(0xFF8A8578)

val Outline = Color(0xFFE6E2DA)
val OutlineVariant = Color(0xFFF0EDE6)

val SemanticSuccess = Color(0xFF4A6B2A)
val SemanticWarningBg = Color(0xFFFEF3C7)
val SemanticWarning = Color(0xFFD97706)
val SemanticErrorBg = Color(0xFFFEF2F2)
val SemanticError = Color(0xFFDC2626)
val SemanticInfoBg = Color(0xFFEFF6FF)
val SemanticInfo = Color(0xFF2563EB)

// ── Dark palette — the mockup's darkVars, verbatim ───────────
val DarkPrimary = Color(0xFF7DA355)          // --olive
val DarkPrimaryLight = Color(0xFF96BC6E)     // --olive-light
val DarkPrimaryContainer = Color(0xFF2A3A1E) // --olive-soft
val DarkAccent = Color(0xFFD4B87A)           // --gold
val DarkAccentContainer = Color(0xFF3D3528)  // --gold-soft
val DarkBackground = Color(0xFF14110E)       // --bg-primary
val DarkSurface = Color(0xFF26221C)          // --bg-card
val DarkSurfaceVariant = Color(0xFF3D3830)   // --border-color
val DarkOnSurface = Color(0xFFF0EDE8)        // --text-primary
val DarkOnSurfaceVariant = Color(0xFFBFB8AA) // --text-secondary
val DarkOutline = Color(0xFF3D3830)
val DarkError = Color(0xFFF87171)            // --danger-fg
val DarkErrorBg = Color(0xFF3D1A1A)          // --danger-bg

// ── Status colors — semantic feedback (success / info / warning / danger)
// that Material's colorScheme has no dedicated slot for. These previously lived
// as `Color(0xFF...)` literals scattered across the screens, so they never
// tracked the light/dark toggle and read wrong in dark mode. Read them through
// [statusColors()] inside a @Composable instead: the light variants are deepened
// for contrast on the cream surface, the dark variants lightened for contrast on
// the charcoal one.
@Immutable
class StatusColorScheme(
    val success: Color,
    val info: Color,
    val warning: Color,
    val danger: Color,
    val gold: Color,
)

private val LightStatusColors = StatusColorScheme(
    success = Color(0xFF2E7D32),
    info = Color(0xFF1565C0),
    warning = Color(0xFFE65100),
    danger = Color(0xFFD32F2F),
    gold = Color(0xFFB8860B),
)

private val DarkStatusColors = StatusColorScheme(
    success = Color(0xFF81C784),
    info = Color(0xFF64B5F6),
    warning = Color(0xFFFFB74D),
    danger = Color(0xFFEF9A9A),
    gold = Color(0xFFD4B87A),
)

/** Theme-aware semantic status colors. Follows the ThemeController toggle. */
@Composable
fun statusColors(): StatusColorScheme =
    if (ThemeController.dark.value) DarkStatusColors else LightStatusColors

// ── Categorical palette — fixed, mutually distinct hues for charts, pipeline
// stages, knowledge-type tags and report badges. Deliberately NOT theme-swapped:
// the job is to stay distinguishable from one another, and these mid-tones stay
// legible on both the cream and the charcoal surfaces.
val CatGreen = Color(0xFF66BB6A)
val CatBlue = Color(0xFF42A5F5)
val CatOrange = Color(0xFFFF8A65)
val CatRed = Color(0xFFEF5350)
val CatPurple = Color(0xFF9575CD)
val CatAmber = Color(0xFFFFB300)
val CatCyan = Color(0xFF26C6DA)
val CatBrown = Color(0xFF8D6E63)
val CatBlueGrey = Color(0xFF78909C)
val CatPink = Color(0xFFEC7FA9)
val CatIndigo = Color(0xFF5C6BC0)
val CatTeal = Color(0xFF26A69A)

// ── Brand-fixed accent — Google's mark keeps its own blue regardless of theme.
val GoogleBlue = Color(0xFF4285F4)

// ── Flashcard surface — an intentionally always-dark "study card" in both
// themes (dark-green felt with white text), like a physical flashcard. Its
// foreground colors stay fixed & bright so they read on that dark surface.
val FlashcardFront = listOf(Color(0xFF1B2F1A), Color(0xFF2D4E2C))
val FlashcardBack = listOf(Color(0xFF0D1F0C), Color(0xFF1A3019))
val FlashcardGold = Color(0xFFFFD700)
val FlashcardGreen = Color(0xFF66BB6A)
