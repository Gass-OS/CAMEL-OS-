package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EuropassLightColors = lightColorScheme(
    primary = EuroNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = EuroNavy,
    secondary = EuroCobalt,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = EuroNavy,
    tertiary = EuroGold,
    onTertiary = Color(0xFF1E1B18),
    tertiaryContainer = EuroGoldSoft,
    onTertiaryContainer = Color(0xFF422006),
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = ErrorRed,
    onError = Color.White
)

private val EuropassDarkColors = darkColorScheme(
    primary = EuroSky,
    onPrimary = DarkBg,
    primaryContainer = EuroNavy,
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = EuroGold,
    onSecondary = DarkBg,
    secondaryContainer = Color(0xFF33290A),
    onSecondaryContainer = EuroGoldSoft,
    tertiary = EuroGold,
    onTertiary = DarkBg,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = Color(0xFF334155),
    error = Color(0xFFF87171),
    onError = DarkBg
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) EuropassDarkColors else EuropassLightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
