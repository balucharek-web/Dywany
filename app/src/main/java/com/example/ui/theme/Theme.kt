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
    primary = GreenLMPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenLMDark,
    onPrimaryContainer = Color.White,
    secondary = AccentAmber,
    surface = DarkSurface,
    background = Color(0xFF141712),
    onSurface = Color(0xFFE8EDE3),
    onBackground = Color(0xFFE8EDE3)
)

private val LightColorScheme = lightColorScheme(
    primary = GreenLMPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenLMLight,
    onPrimaryContainer = GreenLMDark,
    secondary = AccentAmber,
    surface = Color.White,
    background = LightSurface,
    onSurface = TextPrimary,
    onBackground = TextPrimary
)

@Composable
fun MyApplicationTheme(
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
        content = content
    )
}
