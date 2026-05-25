package com.chenyi.agent.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ==================== Corner Radius Values ====================
// Based on HTML design: border-radius: 16px for cards, 12px for buttons
val CornerRadiusNone = 0.dp
val CornerRadiusXs = 4.dp
val CornerRadiusSm = 8.dp
val CornerRadiusMd = 12.dp
val CornerRadiusLg = 16.dp
val CornerRadiusXl = 20.dp
val CornerRadius2Xl = 24.dp
val CornerRadius3Xl = 32.dp
val CornerRadiusFull = 9999.dp

// ==================== Pre-defined Shapes ====================
// Extra Small - for chips, small buttons
val ShapeXs = RoundedCornerShape(CornerRadiusXs)

// Small - for text fields, small cards
val ShapeSm = RoundedCornerShape(CornerRadiusSm)

// Medium - for buttons (HTML: border-radius: 12px)
val ShapeMd = RoundedCornerShape(CornerRadiusMd)

// Large - for cards (HTML: border-radius: 16px)
val ShapeLg = RoundedCornerShape(CornerRadiusLg)

// Extra Large - for large cards, dialogs
val ShapeXl = RoundedCornerShape(CornerRadiusXl)

// 2X Large - for bottom sheets, large dialogs
val Shape2Xl = RoundedCornerShape(CornerRadius2Xl)

// 3X Large - for modal dialogs
val Shape3Xl = RoundedCornerShape(CornerRadius3Xl)

// Full - for pills, avatars, floating action buttons
val ShapeFull = RoundedCornerShape(CornerRadiusFull)

// ==================== Asymmetric Shapes ====================
// For special UI elements like bottom sheets with only top corners rounded
val ShapeTopLg = RoundedCornerShape(
    topStart = CornerRadiusLg,
    topEnd = CornerRadiusLg,
    bottomStart = CornerRadiusNone,
    bottomEnd = CornerRadiusNone
)

val ShapeTopXl = RoundedCornerShape(
    topStart = CornerRadiusXl,
    topEnd = CornerRadiusXl,
    bottomStart = CornerRadiusNone,
    bottomEnd = CornerRadiusNone
)

val ShapeTop2Xl = RoundedCornerShape(
    topStart = CornerRadius2Xl,
    topEnd = CornerRadius2Xl,
    bottomStart = CornerRadiusNone,
    bottomEnd = CornerRadiusNone
)

// Bottom rounded shapes
val ShapeBottomLg = RoundedCornerShape(
    topStart = CornerRadiusNone,
    topEnd = CornerRadiusNone,
    bottomStart = CornerRadiusLg,
    bottomEnd = CornerRadiusLg
)

// Left side rounded (for list items, navigation items)
val ShapeStartMd = RoundedCornerShape(
    topStart = CornerRadiusMd,
    topEnd = CornerRadiusNone,
    bottomStart = CornerRadiusMd,
    bottomEnd = CornerRadiusNone
)

// Right side rounded
val ShapeEndMd = RoundedCornerShape(
    topStart = CornerRadiusNone,
    topEnd = CornerRadiusMd,
    bottomStart = CornerRadiusNone,
    bottomEnd = CornerRadiusMd
)

// ==================== Card Shapes ====================
// Standard card shape matching HTML design
val ShapeCard = ShapeLg // 16dp corners

// Elevated card with slightly larger corners
val ShapeCardElevated = ShapeXl // 20dp corners

// Compact card for dense layouts
val ShapeCardCompact = ShapeMd // 12dp corners

// ==================== Button Shapes ====================
// Standard button shape matching HTML design
val ShapeButton = ShapeMd // 12dp corners

// Pill button shape
val ShapeButtonPill = ShapeFull

// Icon button shape
val ShapeIconButton = ShapeFull

// ==================== Input Shapes ====================
// Text field shape
val ShapeTextField = ShapeMd // 12dp corners

// Search bar shape
val ShapeSearchBar = ShapeLg // 16dp corners

// ==================== Dialog Shapes ====================
// Standard dialog
val ShapeDialog = Shape2Xl // 24dp corners

// Bottom sheet
val ShapeBottomSheet = ShapeTop2Xl // 24dp top corners only

// ==================== Material3 Shapes Configuration ====================
val AppShapes = Shapes(
    extraSmall = ShapeXs,
    small = ShapeSm,
    medium = ShapeMd,
    large = ShapeLg,
    extraLarge = ShapeXl
)
