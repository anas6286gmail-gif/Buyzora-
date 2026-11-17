package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = BuyzoraCoralDark,
    onPrimary = Color(0xFF4F1400),
    primaryContainer = BuyzoraCoralContainerDark,
    onPrimaryContainer = BuyzoraCoralContainer,
    secondary = Color(0xFFD0D5E2),
    onSecondary = Color(0xFF1F2430),
    secondaryContainer = BuyzoraDarkSurfaceVariant,
    onSecondaryContainer = Color(0xFFE7EAF2),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF3F2800),
    background = BuyzoraDarkBg,
    onBackground = Color(0xFFF4F1EC),
    surface = BuyzoraDarkSurface,
    onSurface = Color(0xFFF4F1EC),
    surfaceVariant = BuyzoraDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB8B3AA),
    outline = BuyzoraDarkOutline,
    outlineVariant = Color(0xFF282D3B)
)

private val LightColorScheme = lightColorScheme(
    primary = BuyzoraCoral,
    onPrimary = Color.White,
    primaryContainer = BuyzoraCoralContainer,
    onPrimaryContainer = OnBuyzoraCoralContainer,
    secondary = BuyzoraObsidian,
    onSecondary = Color.White,
    secondaryContainer = BuyzoraSlateContainer,
    onSecondaryContainer = BuyzoraObsidian,
    tertiary = BuyzoraAmber,
    onTertiary = Color.White,
    tertiaryContainer = BuyzoraAmberContainer,
    onTertiaryContainer = Color(0xFF451A03),
    background = BuyzoraIvoryBg,
    onBackground = BuyzoraObsidian,
    surface = BuyzoraCardLight,
    onSurface = BuyzoraObsidian,
    surfaceVariant = BuyzoraSurfaceVariantLight,
    onSurfaceVariant = BuyzoraSlate,
    outline = BuyzoraOutlineLight,
    outlineVariant = Color(0xFFEAE3D8)
)

val BuyzoraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BuyzoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = BuyzoraShapes,
        content = content
    )
}
