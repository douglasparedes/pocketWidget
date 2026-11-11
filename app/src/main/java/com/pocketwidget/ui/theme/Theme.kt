package com.pocketwidget.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Brand palette: warm paper, deep forest ink, terracotta accent.
val Forest = Color(0xFF1F4D3A)
val Terrra = Color(0xFFB4552D)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F4D3A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD5E7DA),
    onPrimaryContainer = Color(0xFF0A2418),
    secondary = Color(0xFFB4552D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF7DFD2),
    onSecondaryContainer = Color(0xFF3A1608),
    tertiary = Color(0xFF4A5E73),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFAF7F2),
    onBackground = Color(0xFF1B1A17),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1A17),
    surfaceVariant = Color(0xFFEFEAE1),
    onSurfaceVariant = Color(0xFF6B6459),
    outline = Color(0xFFD6CFC3),
    outlineVariant = Color(0xFFE7E1D6),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F2EA),
    surfaceContainer = Color(0xFFF1ECE3),
    surfaceContainerHigh = Color(0xFFEBE5DB),
    surfaceContainerHighest = Color(0xFFE5DED3),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FD3B4),
    onPrimary = Color(0xFF0A2418),
    primaryContainer = Color(0xFF2A4C3B),
    onPrimaryContainer = Color(0xFFD6EFDD),
    secondary = Color(0xFFE8A47E),
    onSecondary = Color(0xFF42190A),
    secondaryContainer = Color(0xFF5A2A14),
    onSecondaryContainer = Color(0xFFF7DFD2),
    tertiary = Color(0xFFB3C7DE),
    onTertiary = Color(0xFF1A2C3D),
    background = Color(0xFF12110F),
    onBackground = Color(0xFFF2EDE4),
    surface = Color(0xFF1B1915),
    onSurface = Color(0xFFF2EDE4),
    surfaceVariant = Color(0xFF26231E),
    onSurfaceVariant = Color(0xFFB4AB9C),
    outline = Color(0xFF5C564C),
    outlineVariant = Color(0xFF3A352E),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    surfaceContainerLowest = Color(0xFF0D0C0A),
    surfaceContainerLow = Color(0xFF1B1915),
    surfaceContainer = Color(0xFF201E19),
    surfaceContainerHigh = Color(0xFF2A2721),
    surfaceContainerHighest = Color(0xFF353129),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 56.sp,
        lineHeight = 58.sp,
        letterSpacing = (-2).sp,
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 44.sp,
        lineHeight = 46.sp,
        letterSpacing = (-1.5).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.1.sp,
    ),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun PocketWidgetTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
