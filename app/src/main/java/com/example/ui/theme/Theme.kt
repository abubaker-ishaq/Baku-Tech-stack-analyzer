package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

object DesignSystem {
    val CornerSmall = 8.dp
    val CornerMedium = 16.dp
    val CornerLarge = 24.dp
    
    val PaddingSmall = 8.dp
    val PaddingMedium = 16.dp
    val PaddingLarge = 24.dp
    
    val IconSmall = 16.dp
    val IconMedium = 24.dp
    val IconLarge = 48.dp
}

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFFFFFFF), // Titanium White
    secondary = Color(0xFF5E8C71), // Refined Emerald
    tertiary = Color(0xFFC28E75), // Copper
    background = Color(0xFF050505), // Deep Black
    surface = Color(0x0AFFFFFF), // Low opacity white
    onPrimary = Color(0xFF050505),
    onSecondary = Color(0xFF050505),
    onBackground = Color(0xFFE5E5E5),
    onSurface = Color(0xFFE5E5E5)
  )

private val LightColorScheme =
  lightColorScheme( // Consulting Report: elegant light ivory paper mode
    primary = Color(0xFF111827), // Deep Obsidian Charcoal
    secondary = Color(0xFF386A4E), // Deep Forest Green
    tertiary = Color(0xFFA25944), // Corporate Burgundy
    background = Color(0xFFFAF9F5), // Ivory paper
    surface = Color(0xFFFFFFFF), // Pristine card sheet
    onPrimary = Color(0xFFFAF9F5),
    onSecondary = Color(0xFFFAF9F5),
    onBackground = Color(0xFF1E2229), // Deep Charcoal
    onSurface = Color(0xFF1E2229)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(), // Automatically switch using system settings
  dynamicColor: Boolean = false, // Use our professional corporate color branding rather than dynamic OS colors
  content: @Composable () -> Unit,
) {
  isDarkThemeSystem = darkTheme
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
