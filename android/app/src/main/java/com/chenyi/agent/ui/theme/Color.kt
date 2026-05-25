package com.chenyi.agent.ui.theme

import androidx.compose.ui.graphics.Color

// ==================== Background Colors ====================
val BgPrimary = Color(0xFFF0F4F8)
val BgSecondary = Color(0xFFE8EEF5)
val BgTertiary = Color(0xFFDCE4ED)
val BgCard = Color(0xFFFFFFFF)
val BgGlass = Color(0xD9FFFFFF) // 85% opacity

// ==================== Border Colors ====================
val BorderSubtle = Color(0x0F000000) // 6% opacity
val BorderGlow = Color(0x59FF0000) // 35% opacity cyan glow

// ==================== Text Colors ====================
val TextPrimary = Color(0xFF1A1A2E)
val TextSecondary = Color(0xFF5A6A7A)
val TextMuted = Color(0xFF8A9AAA)

// ==================== Accent Colors ====================
val AccentCyan = Color(0xFF0077FF)
val AccentPurple = Color(0xFF7C3AED)
val AccentPink = Color(0xFFDB2777)
val AccentGreen = Color(0xFF059669)
val AccentYellow = Color(0xFFD97706)
val AccentRed = Color(0xFFDC2626)

// ==================== Gradient Colors ====================
val GradientPrimary = listOf(AccentCyan, AccentPurple, AccentPink)
val GradientSecondary = listOf(AccentPurple, AccentPink)
val GradientSuccess = listOf(AccentGreen, Color(0xFF10B981))
val GradientWarning = listOf(AccentYellow, Color(0xFFF59E0B))
val GradientError = listOf(AccentRed, Color(0xFFEF4444))

// ==================== Status Colors ====================
val StatusOnline = Color(0xFF10B981)
val StatusOffline = Color(0xFF6B7280)
val StatusBusy = Color(0xFFF59E0B)
val StatusError = Color(0xFFEF4444)

// ==================== Shadow Colors ====================
val ShadowLight = Color(0x1A000000) // 10% opacity
val ShadowMedium = Color(0x33000000) // 20% opacity
val ShadowGlowCyan = Color(0x4D0077FF) // 30% opacity cyan glow
val ShadowGlowPurple = Color(0x4D7C3AED) // 30% opacity purple glow

// ==================== Material3 Color Scheme - Light ====================
val md_theme_light_primary = AccentCyan
val md_theme_light_onPrimary = Color.White
val md_theme_light_primaryContainer = Color(0xFFD1E4FF)
val md_theme_light_onPrimaryContainer = Color(0xFF001D36)
val md_theme_light_secondary = AccentPurple
val md_theme_light_onSecondary = Color.White
val md_theme_light_secondaryContainer = Color(0xFFE8DEF8)
val md_theme_light_onSecondaryContainer = Color(0xFF1D192B)
val md_theme_light_tertiary = AccentPink
val md_theme_light_onTertiary = Color.White
val md_theme_light_tertiaryContainer = Color(0xFFFFD8E4)
val md_theme_light_onTertiaryContainer = Color(0xFF31111D)
val md_theme_light_error = AccentRed
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onError = Color.White
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_background = BgPrimary
val md_theme_light_onBackground = TextPrimary
val md_theme_light_surface = BgCard
val md_theme_light_onSurface = TextPrimary
val md_theme_light_surfaceVariant = BgSecondary
val md_theme_light_onSurfaceVariant = TextSecondary
val md_theme_light_outline = BorderSubtle
val md_theme_light_inverseOnSurface = BgPrimary
val md_theme_light_inverseSurface = TextPrimary
val md_theme_light_inversePrimary = Color(0xFF9ECAFF)
val md_theme_light_surfaceTint = AccentCyan
val md_theme_light_outlineVariant = Color(0xFFC4C6CF)
val md_theme_light_scrim = Color.Black

// ==================== Material3 Color Scheme - Dark ====================
val md_theme_dark_primary = Color(0xFF9ECAFF)
val md_theme_dark_onPrimary = Color(0xFF003258)
val md_theme_dark_primaryContainer = Color(0xFF00497D)
val md_theme_dark_onPrimaryContainer = Color(0xFFD1E4FF)
val md_theme_dark_secondary = Color(0xFFCCC2DC)
val md_theme_dark_onSecondary = Color(0xFF332D41)
val md_theme_dark_secondaryContainer = Color(0xFF4A4458)
val md_theme_dark_onSecondaryContainer = Color(0xFFE8DEF8)
val md_theme_dark_tertiary = Color(0xFFEFB8C8)
val md_theme_dark_onTertiary = Color(0xFF492532)
val md_theme_dark_tertiaryContainer = Color(0xFF633B48)
val md_theme_dark_onTertiaryContainer = Color(0xFFFFD8E4)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_background = Color(0xFF1A1A2E)
val md_theme_dark_onBackground = Color(0xFFE0E0E8)
val md_theme_dark_surface = Color(0xFF12121C)
val md_theme_dark_onSurface = Color(0xFFE0E0E8)
val md_theme_dark_surfaceVariant = Color(0xFF2A2A3E)
val md_theme_dark_onSurfaceVariant = Color(0xFFC4C6CF)
val md_theme_dark_outline = Color(0xFF4A4A5E)
val md_theme_dark_inverseOnSurface = Color(0xFF1A1A2E)
val md_theme_dark_inverseSurface = Color(0xFFE0E0E8)
val md_theme_dark_inversePrimary = AccentCyan
val md_theme_dark_surfaceTint = Color(0xFF9ECAFF)
val md_theme_dark_outlineVariant = Color(0xFF4A4A5E)
val md_theme_dark_scrim = Color.Black
