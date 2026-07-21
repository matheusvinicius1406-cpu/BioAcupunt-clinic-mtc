package com.bioacupunt.ui.theme

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
