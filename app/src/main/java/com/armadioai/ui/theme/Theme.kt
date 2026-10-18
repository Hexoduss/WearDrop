package com.armadioai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// Palette calda da atelier: crema, terracotta e verde oliva.
private val Light = lightColorScheme(
    primary = Color(0xFF9C4A2A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCC),
    onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Color(0xFF5E6B3A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2EDC0),
    onSecondaryContainer = Color(0xFF1B2003),
    tertiary = Color(0xFF6B5B7B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF1DBFF),
    onTertiaryContainer = Color(0xFF251834),
    background = Color(0xFFFBF7F2),
    onBackground = Color(0xFF1F1B18),
    surface = Color(0xFFFBF7F2),
    onSurface = Color(0xFF1F1B18),
    surfaceVariant = Color(0xFFF1E7DE),
    onSurfaceVariant = Color(0xFF52443D),
    outline = Color(0xFF85736A),
    outlineVariant = Color(0xFFD7C2B9),
    surfaceDim = Color(0xFFE3D8D0),
    surfaceBright = Color(0xFFFBF7F2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F1EB),
    surfaceContainer = Color(0xFFF4ECE5),
    surfaceContainerHigh = Color(0xFFEEE5DE),
    surfaceContainerHighest = Color(0xFFE8DFD8),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB596),
    onPrimary = Color(0xFF5A1C03),
    primaryContainer = Color(0xFF7A3216),
    onPrimaryContainer = Color(0xFFFFDBCC),
    secondary = Color(0xFFC6D1A5),
    onSecondary = Color(0xFF2F3713),
    secondaryContainer = Color(0xFF464F27),
    onSecondaryContainer = Color(0xFFE2EDC0),
    tertiary = Color(0xFFD7BEE8),
    onTertiary = Color(0xFF3B2D4A),
    tertiaryContainer = Color(0xFF524362),
    onTertiaryContainer = Color(0xFFF1DBFF),
    background = Color(0xFF181412),
    onBackground = Color(0xFFEDE0D9),
    surface = Color(0xFF181412),
    onSurface = Color(0xFFEDE0D9),
    surfaceVariant = Color(0xFF52443D),
    onSurfaceVariant = Color(0xFFD7C2B9),
    outline = Color(0xFFA08D84),
    outlineVariant = Color(0xFF52443D),
    surfaceDim = Color(0xFF181412),
    surfaceBright = Color(0xFF3F3835),
    surfaceContainerLowest = Color(0xFF120E0C),
    surfaceContainerLow = Color(0xFF201A17),
    surfaceContainer = Color(0xFF251E1B),
    surfaceContainerHigh = Color(0xFF302825),
    surfaceContainerHighest = Color(0xFF3B332F),
)

private val base = Typography()

// Titoli con grazie, da rivista di moda; testi in sans-serif per la leggibilità.
private val AppTypography = Typography(
    displaySmall = base.displaySmall.copy(fontFamily = FontFamily.Serif),
    headlineLarge = base.headlineLarge.copy(fontFamily = FontFamily.Serif),
    headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif),
    headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Serif),
    titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium),
)

@Composable
fun ArmadioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        content = content,
    )
}
