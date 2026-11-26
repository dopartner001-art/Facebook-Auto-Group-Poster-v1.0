package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = FbBlue,
    onPrimary = Color.White,
    primaryContainer = FbBlueSoft,
    onPrimaryContainer = FbBlueDark,
    secondary = FbBlueDark,
    onSecondary = Color.White,
    secondaryContainer = SurfaceMutedLight,
    onSecondaryContainer = InkPrimaryLight,
    tertiary = SuccessGreen,
    onTertiary = Color.White,
    tertiaryContainer = SuccessGreenSoft,
    onTertiaryContainer = SuccessGreen,
    background = CanvasLight,
    onBackground = InkPrimaryLight,
    surface = CardWhite,
    onSurface = InkPrimaryLight,
    surfaceVariant = SurfaceMutedLight,
    onSurfaceVariant = InkSecondaryLight,
    outline = OutlineSubtleLight,
    outlineVariant = Color(0xFFEAECEF),
    error = DangerRed,
    onError = Color.White,
    errorContainer = DangerRedSoft,
    onErrorContainer = DangerRed
)

private val DarkColorScheme = darkColorScheme(
    primary = FbBlueLight,
    onPrimary = Color(0xFF061B3B),
    primaryContainer = FbBlueSoftDark,
    onPrimaryContainer = Color(0xFFD3E4FF),
    secondary = FbBlue,
    onSecondary = Color.White,
    secondaryContainer = SurfaceMutedDark,
    onSecondaryContainer = InkPrimaryDark,
    tertiary = Color(0xFF6DD58C),
    onTertiary = Color(0xFF0A3818),
    tertiaryContainer = SuccessGreenDarkSoft,
    onTertiaryContainer = Color(0xFFA8E6BA),
    background = CanvasDark,
    onBackground = InkPrimaryDark,
    surface = CardDark,
    onSurface = InkPrimaryDark,
    surfaceVariant = SurfaceMutedDark,
    onSurfaceVariant = InkSecondaryDark,
    outline = OutlineSubtleDark,
    outlineVariant = Color(0xFF2B313C),
    error = Color(0xFFF28B82),
    onError = Color(0xFF601410),
    errorContainer = DangerRedDarkSoft,
    onErrorContainer = Color(0xFFF9DEDC)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun AutoGroupPosterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
