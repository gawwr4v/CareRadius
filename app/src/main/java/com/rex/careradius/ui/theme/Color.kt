package com.rex.careradius.ui.theme

import androidx.compose.ui.graphics.Color

// === Nordic Utility Palette ===
// Philosophy: Practical, Neutral, Structured
// No "Mint", "Teal", or "Decorations"

// Primary: Practical Blue (Focus/Action)
val UtilityBlueLight = Color(0xFF0058A3)
val UtilityBlueDark = Color(0xFF4FA3FF)

val LightBackground = Color(0xFFF6F6F4) // Off-white (Paper-like)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEBEBEB)
val LightOnBackground = Color(0xFF1E1E1E) // High Contrast Charcoal
val LightOnSurface = Color(0xFF1E1E1E)
val LightOnSurfaceVariant = Color(0xFF484848)
val LightOutline = Color(0xFFC6C6C6) // Neutral gray for borders

// === Dark Theme (Nordic Utility) ===
// Background: Neutral Charcoal
val DarkBackground = Color(0xFF1E1E1E)
val DarkSurface = Color(0xFF242424)     // Slightly lighter for cards
val DarkSurfaceVariant = Color(0xFF2B2B2B) // For dialogs/modals
val DarkOnBackground = Color(0xFFE6E6E6)
val DarkOnSurface = Color(0xFFE6E6E6)
val DarkOnSurfaceVariant = Color(0xFFAAAAAA)
val DarkOutline = Color(0xFF444444)

val ActiveAccent = UtilityBlueLight // For light mode active states
val ActiveAccentDark = UtilityBlueDark // For dark mode active states

val ErrorColor = Color(0xFFBA1A1A)
val OnErrorColor = Color(0xFFFFFFFF)
val DarkErrorColor = Color(0xFFFFB4AB)
val DarkOnErrorColor = Color(0xFF690005)