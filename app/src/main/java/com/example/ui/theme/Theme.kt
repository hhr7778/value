package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val GoponDarkColorScheme = darkColorScheme(
    primary = VioletPrimary,
    onPrimary = Color.White,
    primaryContainer = VaultSurfaceElevated,
    onPrimaryContainer = VioletSoft,
    secondary = VioletGlow,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2B1D4E),
    onSecondaryContainer = VioletSoft,
    tertiary = CyanAccent,
    onTertiary = VaultObsidian,
    background = VaultObsidian,
    onBackground = TextPrimary,
    surface = VaultSurface,
    onSurface = TextPrimary,
    surfaceVariant = VaultSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = VaultCardBorder,
    outlineVariant = Color(0xFF261C42),
    error = AlertCrimson,
    onError = Color.White
)

val GoponShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val VaultBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        VaultObsidian,
        VaultDeepPurple,
        Color(0xFF0C0718)
    )
)

val VioletCardGlowBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF2D1B69),
        Color(0xFF1A1233)
    )
)

@Composable
fun GoponVaultTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GoponDarkColorScheme,
        typography = GoponTypography,
        shapes = GoponShapes,
        content = content
    )
}
