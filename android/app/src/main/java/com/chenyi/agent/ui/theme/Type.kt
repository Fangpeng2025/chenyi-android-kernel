package com.chenyi.agent.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ==================== Font Families ====================
// Note: Add font files to res/font/ directory:
// - orbitron_regular.ttf, orbitron_medium.ttf, orbitron_bold.ttf
// - noto_sans_sc_regular.ttf, noto_sans_sc_medium.ttf, noto_sans_sc_bold.ttf
// - jetbrains_mono_regular.ttf, jetbrains_mono_medium.ttf

val FontDisplay = FontFamily.Default // Replace with FontFamily(Font(R.font.orbitron_regular))
val FontBody = FontFamily.Default // Replace with FontFamily(Font(R.font.noto_sans_sc_regular))
val FontMono = FontFamily.Default // Replace with FontFamily(Font(R.font.jetbrains_mono_regular))

// ==================== Custom Text Styles ====================
// Display styles - for large titles (Orbitron font)
val TextStyleDisplayLarge = TextStyle(
    fontFamily = FontDisplay,
    fontWeight = FontWeight.Bold,
    fontSize = 57.sp,
    lineHeight = 64.sp,
    letterSpacing = (-0.25).sp
)

val TextStyleDisplayMedium = TextStyle(
    fontFamily = FontDisplay,
    fontWeight = FontWeight.Bold,
    fontSize = 45.sp,
    lineHeight = 52.sp,
    letterSpacing = 0.sp
)

val TextStyleDisplaySmall = TextStyle(
    fontFamily = FontDisplay,
    fontWeight = FontWeight.Medium,
    fontSize = 36.sp,
    lineHeight = 44.sp,
    letterSpacing = 0.sp
)

// Headline styles - for section headers
val TextStyleHeadlineLarge = TextStyle(
    fontFamily = FontDisplay,
    fontWeight = FontWeight.Bold,
    fontSize = 32.sp,
    lineHeight = 40.sp,
    letterSpacing = 0.sp
)

val TextStyleHeadlineMedium = TextStyle(
    fontFamily = FontDisplay,
    fontWeight = FontWeight.Medium,
    fontSize = 28.sp,
    lineHeight = 36.sp,
    letterSpacing = 0.sp
)

val TextStyleHeadlineSmall = TextStyle(
    fontFamily = FontDisplay,
    fontWeight = FontWeight.Medium,
    fontSize = 24.sp,
    lineHeight = 32.sp,
    letterSpacing = 0.sp
)

// Title styles - for card titles and app bar
val TextStyleTitleLarge = TextStyle(
    fontFamily = FontBody,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = 0.sp
)

val TextStyleTitleMedium = TextStyle(
    fontFamily = FontBody,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.15.sp
)

val TextStyleTitleSmall = TextStyle(
    fontFamily = FontBody,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.1.sp
)

// Body styles - for main content (Noto Sans SC font)
val TextStyleBodyLarge = TextStyle(
    fontFamily = FontBody,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.5.sp
)

val TextStyleBodyMedium = TextStyle(
    fontFamily = FontBody,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.25.sp
)

val TextStyleBodySmall = TextStyle(
    fontFamily = FontBody,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.4.sp
)

// Label styles - for buttons, labels
val TextStyleLabelLarge = TextStyle(
    fontFamily = FontBody,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.1.sp
)

val TextStyleLabelMedium = TextStyle(
    fontFamily = FontBody,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.5.sp
)

val TextStyleLabelSmall = TextStyle(
    fontFamily = FontBody,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.5.sp
)

// Mono styles - for code and numbers (JetBrains Mono font)
val TextStyleMonoLarge = TextStyle(
    fontFamily = FontMono,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.sp
)

val TextStyleMonoMedium = TextStyle(
    fontFamily = FontMono,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.sp
)

val TextStyleMonoSmall = TextStyle(
    fontFamily = FontMono,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.sp
)

// Caption and overline styles
val TextStyleCaption = TextStyle(
    fontFamily = FontBody,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.4.sp
)

val TextStyleOverline = TextStyle(
    fontFamily = FontBody,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    lineHeight = 16.sp,
    letterSpacing = 1.5.sp
)

// ==================== Material3 Typography ====================
val AppTypography = Typography(
    displayLarge = TextStyleDisplayLarge,
    displayMedium = TextStyleDisplayMedium,
    displaySmall = TextStyleDisplaySmall,
    headlineLarge = TextStyleHeadlineLarge,
    headlineMedium = TextStyleHeadlineMedium,
    headlineSmall = TextStyleHeadlineSmall,
    titleLarge = TextStyleTitleLarge,
    titleMedium = TextStyleTitleMedium,
    titleSmall = TextStyleTitleSmall,
    bodyLarge = TextStyleBodyLarge,
    bodyMedium = TextStyleBodyMedium,
    bodySmall = TextStyleBodySmall,
    labelLarge = TextStyleLabelLarge,
    labelMedium = TextStyleLabelMedium,
    labelSmall = TextStyleLabelSmall
)
