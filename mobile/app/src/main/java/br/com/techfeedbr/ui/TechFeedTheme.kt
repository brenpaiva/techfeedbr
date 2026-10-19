package br.com.techfeedbr.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF145DA0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCECF8),
    onPrimaryContainer = Color(0xFF10243E),
    secondary = Color(0xFF007F73),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8F3EE),
    onSecondaryContainer = Color(0xFF083E39),
    tertiary = Color(0xFF2166C2),
    tertiaryContainer = Color(0xFFE2EDFA),
    onTertiaryContainer = Color(0xFF10243E),
    background = Color(0xFFF7F9FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEF3F7),
    outline = Color(0xFF75869A),
    outlineVariant = Color(0xFFCDD8E2),
    onBackground = Color(0xFF10243E),
    onSurface = Color(0xFF10243E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF76B9FF),
    onPrimary = Color(0xFF002B50),
    primaryContainer = Color(0xFF183B5A),
    onPrimaryContainer = Color(0xFFDCECF8),
    secondary = Color(0xFF50D8C8),
    onSecondary = Color(0xFF003732),
    secondaryContainer = Color(0xFF164A46),
    onSecondaryContainer = Color(0xFFD8F3EE),
    tertiary = Color(0xFF9FC5F5),
    tertiaryContainer = Color(0xFF1F416B),
    onTertiaryContainer = Color(0xFFE2EDFA),
    background = Color(0xFF071525),
    surface = Color(0xFF10243E),
    surfaceVariant = Color(0xFF1C3652),
    outline = Color(0xFF9AADC0),
    outlineVariant = Color(0xFF334C66),
    onBackground = Color(0xFFE6EEF8),
    onSurface = Color(0xFFE6EEF8),
)

@Composable
fun TechFeedTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
