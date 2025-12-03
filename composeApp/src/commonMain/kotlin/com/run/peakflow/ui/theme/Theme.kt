package com.run.peakflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Primary Green
val PrimaryGreen = Color(0xFF2E7D32)
val PrimaryGreenLight = Color(0xFF4CAF50)
val PrimaryGreenDark = Color(0xFF1B5E20)

// Secondary
val SecondaryTeal = Color(0xFF00897B)

// Surface colors
val SurfaceLight = Color(0xFFFFFBFE)
val SurfaceDark = Color(0xFF1C1B1F)

// Background
val BackgroundLight = Color(0xFFF5F5F5)
val BackgroundDark = Color(0xFF121212)

// Error
val ErrorRed = Color(0xFFB00020)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = Color(0xFF002106),
    secondary = SecondaryTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF00201E),
    tertiary = Color(0xFF6D4C41),
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = Color(0xFF1C1B1F),
    surface = SurfaceLight,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    error = ErrorRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreenLight,
    onPrimary = Color(0xFF003910),
    primaryContainer = PrimaryGreenDark,
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color(0xFF003733),
    secondaryContainer = Color(0xFF00504A),
    onSecondaryContainer = Color(0xFFB2DFDB),
    tertiary = Color(0xFFBCAAA4),
    onTertiary = Color(0xFF3E2723),
    background = BackgroundDark,
    onBackground = Color(0xFFE6E1E5),
    surface = SurfaceDark,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    error = Color(0xFFCF6679),
    onError = Color.Black
)

@Composable
fun PeakFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}