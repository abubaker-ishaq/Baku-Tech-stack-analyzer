package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.composed

// Global state to track dark theme mode, updated by the theme wrapper.
// This allows properties to read the active theme state from any context (including Canvas and draw loops)
// without requiring the @Composable annotation which breaks in DrawScopes.
var isDarkThemeSystem by mutableStateOf(true)

// Absolute deep executive background: Obsidian Black in Dark Mode, Warm Ivory Paper in Light Mode
val DarkBg: Color
    get() = if (isDarkThemeSystem) Color(0xFF0F1115) else Color(0xFFFAF9F5)

// Premium Surfaces (Frosted white on dark, Pristine Sheet white on light)
val DarkSurface: Color
    get() = if (isDarkThemeSystem) Color(0xFF171A20) else Color(0xFFFFFFFF)

val DarkSurfaceElevated: Color
    get() = if (isDarkThemeSystem) Color(0xFF1C2028) else Color(0xFFF4F3ED)

val CardBorderColor: Color
    get() = if (isDarkThemeSystem) Color(0xFF2D323C) else Color(0xFFE5E2D8)

// Professional Executive Muted Palette (Strictly NO bright or neon colors!)
val RedCost: Color
    get() = if (isDarkThemeSystem) Color(0xFFFF453A) else Color(0xFFA25944) // Corporate Red vs corporate burgundy

val RedCostBg: Color
    get() = if (isDarkThemeSystem) Color(0x0EFF453A) else Color(0x08A25944)

val RedCostBorder: Color
    get() = if (isDarkThemeSystem) Color(0x22FF453A) else Color(0x1AA25944)

val GreenCost: Color
    get() = if (isDarkThemeSystem) Color(0xFF34C759) else Color(0xFF386A4E) // Refined emerald vs deep forest green

val GreenCostBg: Color
    get() = if (isDarkThemeSystem) Color(0x0E34C759) else Color(0x08386A4E)

val GreenCostBorder: Color
    get() = if (isDarkThemeSystem) Color(0x2234C759) else Color(0x1AA25944)

val BlueAccent: Color
    get() = if (isDarkThemeSystem) Color(0xFFFFFFFF) else Color(0xFF111827) // Premium White Accent for text/structure in dark mode

val BlueAccentBg: Color
    get() = if (isDarkThemeSystem) Color(0x11FFFFFF) else Color(0x0F111827)

val BlueAccentBorder: Color
    get() = if (isDarkThemeSystem) Color(0x22FFFFFF) else Color(0x1F111827)

val GoldHighlight: Color
    get() = if (isDarkThemeSystem) Color(0xFFC8A75D) else Color(0xFF9E7E2F) // Luxury ROI Gold vs Consulting Ochre (Strictly for Winner/ROI/Score)

val TextPrimary: Color
    get() = if (isDarkThemeSystem) Color(0xFFFFFFFF) else Color(0xFF1E2229) // Pure white vs deep charcoal

val TextSecondary: Color
    get() = if (isDarkThemeSystem) Color(0xFFB7BEC9) else Color(0xFF5C6370) // Soft executive slate gray

val TextMuted: Color
    get() = if (isDarkThemeSystem) Color(0xFF6E7582) else Color(0xFF8C929D) // Muted slate gray

val TealAccent: Color
    get() = BlueAccent // Re-map to Grayscale Neutral

val EmeraldAccent: Color
    get() = GreenCost // Re-map to Muted Emerald/Forest Green

val CyberpunkPink: Color
    get() = AmberWarning // Re-map to Amber

val Pink80: Color
    get() = RedCost // Re-map to copper/burgundy

// Elegant Amber Warning for score states
val AmberWarning: Color
    get() = if (isDarkThemeSystem) Color(0xFFFF9F0A) else Color(0xFFB0821A)

// Premium metallic gradient border brush for high-end cards
val MetallicBorderBrush: Brush
    @Composable
    get() = if (isDarkThemeSystem) {
        Brush.linearGradient(
            colors = listOf(
                Color(0x3CFFFFFF), // Crisp silver reflection highlight
                Color(0x0CFFFFFF), // Soft steel metal transition
                Color(0x22FFFFFF), // Medium steel highlight
                Color(0x02FFFFFF)  // Dark shadow border
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0x38111827), // Soft dark metal reflection
                Color(0x05FAF9F5), // Warm ivory paper transition
                Color(0x1A111827), // Medium slate highlight
                Color(0x02FAF9F5)  // Warm ivory shadow border
            )
        )
    }

// Gradient for frosted card glass body
val GlassCardBrush: Brush
    @Composable
    get() = if (isDarkThemeSystem) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x0EFFFFFF), // Semi-opaque top to mimic glass refraction
                Color(0x04FFFFFF)  // Highly translucent bottom
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF), // Pristine paper top
                Color(0xFFFAF9F5)  // Warm bone white bottom
            )
        )
    }

// Extension modifier for a premium frosted glassmorphism effect with a soft metallic border
fun Modifier.glassCard(
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp
): Modifier = this.composed {
    val bg = GlassCardBrush
    val border = MetallicBorderBrush
    this.clip(RoundedCornerShape(cornerRadius))
        .background(bg)
        .border(
            width = borderWidth,
            brush = border,
            shape = RoundedCornerShape(cornerRadius)
        )
}
