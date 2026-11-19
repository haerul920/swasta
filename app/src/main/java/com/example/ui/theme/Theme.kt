package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SwasanaLightColorScheme = lightColorScheme(
    primary = PrimaryEmerald,
    onPrimary = PureWhite,
    primaryContainer = SoftMint,
    onPrimaryContainer = DeepEmerald,
    secondary = SkyBlue,
    onSecondary = PureWhite,
    secondaryContainer = SoftSky,
    onSecondaryContainer = Color(0xFF075985),
    tertiary = VibrantEmerald,
    onTertiary = PureWhite,
    tertiaryContainer = MintSurface,
    onTertiaryContainer = DeepEmerald,
    background = MistBackground,
    onBackground = TextPrimary,
    surface = PureWhite,
    onSurface = TextPrimary,
    surfaceVariant = SubtleSurface,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = Color(0xFFD5E2DC),
    error = AqiVeryUnhealthy,
    onError = PureWhite,
    errorContainer = AqiVeryUnhealthyBg,
    onErrorContainer = Color(0xFF991B1B)
)

val SwasanaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SwasanaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SwasanaLightColorScheme,
        typography = SwasanaTypography,
        shapes = SwasanaShapes,
        content = content
    )
}
