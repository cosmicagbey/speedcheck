package com.example.speedcheck.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SpeedSafeGreen,
    onPrimary = Color.Black,
    primaryContainer = SpeedSafeGreenGlow,
    secondary = AccentCyan,
    background = DarkDashboardBg,
    surface = DarkSurfaceCard,
    surfaceVariant = DarkSurfaceBorder,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    error = SpeedExceededRed
)

private val LightColorScheme = lightColorScheme(
    primary = SpeedSafeGreen,
    onPrimary = Color.White,
    primaryContainer = SpeedSafeGreenGlow,
    secondary = AccentCyan,
    background = Color(0xFFF1F5F9),
    surface = Color.White,
    surfaceVariant = Color(0xFFE2E8F0),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    error = SpeedExceededRed
)

@Composable
fun SpeedCheckTheme(
    darkTheme: Boolean = true, // Default to dark high-contrast mode for driving glare reduction
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
