package com.shiyun.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

val Paper = Color(0xFFF7F4EA)
val Ink = Color(0xFF2B2B2B)
val Cinnabar = Color(0xFFB03A2E)
val FadedInk = Color(0xFF8A8578)
val Border = Color(0xFFD8D2C0)
val PaperRaised = Color(0xFFFCFAF3)

private val LightColors = lightColorScheme(
    primary = Cinnabar, onPrimary = Color.White,
    secondary = Ink, onSecondary = Paper,
    background = Paper, onBackground = Ink,
    surface = PaperRaised, onSurface = Ink,
    onSurfaceVariant = FadedInk,
    outline = Border,
)

private val DarkColors = darkColorScheme(
    primary = Cinnabar, onPrimary = Color.White,
    background = Color(0xFF1C1B18), onBackground = Color(0xFFE8E3D5),
    surface = Color(0xFF26241F), onSurface = Color(0xFFE8E3D5),
    onSurfaceVariant = Color(0xFF9A9587), outline = Color(0xFF4A463D),
)

private val SerifTypography = Typography().withFontFamily(FontFamily.Serif)

private fun Typography.withFontFamily(family: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
)

@Composable
fun ShiyunTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = SerifTypography,
        content = content,
    )
}
