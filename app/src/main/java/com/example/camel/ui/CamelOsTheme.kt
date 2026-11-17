package com.example.camel.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.camel.core.CamelFontStyle
import com.example.camel.core.CamelThemePreset
import com.example.ui.theme.InterFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.ui.theme.PlusJakartaSansFamily

@Composable
fun CamelOsTheme(
    preset: CamelThemePreset = CamelThemePreset.MIDNIGHT_GOLD,
    fontStyle: CamelFontStyle = CamelFontStyle.PLUS_JAKARTA,
    content: @Composable () -> Unit
) {
    val primary = Color(preset.primaryHex)
    val secondary = Color(preset.secondaryHex)
    val background = Color(preset.backgroundHex)
    val surface = Color(preset.surfaceHex)
    val surfaceVariant = Color(preset.surfaceVariantHex)

    val colorScheme = darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF0A0D14),
        primaryContainer = Color(0xFF2B230B),
        onPrimaryContainer = secondary,
        secondary = secondary,
        onSecondary = Color(0xFF090D16),
        secondaryContainer = surfaceVariant,
        onSecondaryContainer = Color(0xFFE2E8F0),
        tertiary = Color(0xFF10B981),
        onTertiary = Color(0xFF041C14),
        background = background,
        onBackground = Color(0xFFF8FAFC),
        surface = surface,
        onSurface = Color(0xFFF8FAFC),
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = Color(0xFFA8B8D0),
        outline = Color(0xFF283A5E),
        error = Color(0xFFF87171),
        onError = Color(0xFF2A0909)
    )

    val typography = remember(fontStyle) { buildCamelTypography(fontStyle) }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}

fun buildCamelTypography(fontStyle: CamelFontStyle): Typography {
    val displayFamily = when (fontStyle) {
        CamelFontStyle.PLUS_JAKARTA -> PlusJakartaSansFamily
        CamelFontStyle.INTER -> InterFamily
        CamelFontStyle.JETBRAINS_MONO -> JetBrainsMonoFamily
        CamelFontStyle.PLAYFAIR -> PlayfairDisplayFamily
    }
    val bodyFamily = when (fontStyle) {
        CamelFontStyle.PLUS_JAKARTA -> InterFamily
        CamelFontStyle.INTER -> InterFamily
        CamelFontStyle.JETBRAINS_MONO -> JetBrainsMonoFamily
        CamelFontStyle.PLAYFAIR -> PlusJakartaSansFamily
    }

    return Typography(
        displaySmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 38.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 30.sp
        ),
        titleLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 26.sp
        ),
        titleMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp
        ),
        titleSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 17.sp
        ),
        labelLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    )
}
