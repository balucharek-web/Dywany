package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = StoreGreenAccent,
    onPrimary = Color.Black,
    primaryContainer = StoreGreenDark,
    onPrimaryContainer = Color.White,
    secondary = StoreGold,
    onSecondary = Color.Black,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = StoreGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = LocationBadgeBg,
    onPrimaryContainer = StoreGreenDark,
    secondary = StoreOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF3E0),
    onSecondaryContainer = Color(0xFFE65100),
    tertiary = StoreGold,
    background = SurfaceLight,
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F1),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent store branding
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
        content = content
    )
}
