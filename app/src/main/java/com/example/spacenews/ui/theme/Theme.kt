package com.example.spacenews.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import com.example.spacenews.data.preferences.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = SpaceBlue,
    onPrimary = SpaceNavy,
    secondary = SpaceCyan,
    onSecondary = SpaceNavy,
    tertiary = SpaceViolet,
    background = SpaceNavy,
    onBackground = SpaceText,
    surface = SpaceSurface,
    onSurface = SpaceText,
    surfaceVariant = SpaceCard,
    onSurfaceVariant = SpaceMuted,
    outline = Color(0xFF52647D)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF245A9B),
    onPrimary = Color.White,
    secondary = Color(0xFF006874),
    onSecondary = Color.White,
    tertiary = Color(0xFF6250A4),
    background = Color(0xFFF5F7FC),
    onBackground = Color(0xFF1A1D24),
    surface = Color.White,
    onSurface = Color(0xFF1A1D24),
    surfaceVariant = Color(0xFFE8EDF5),
    onSurfaceVariant = Color(0xFF465160),
    outline = Color(0xFF727B88)
)

@Composable
fun SpaceNewsTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
