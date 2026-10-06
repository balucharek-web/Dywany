package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ExpoDarkPrimary,
    onPrimary = ExpoDarkOnPrimary,
    primaryContainer = ExpoDarkPrimaryContainer,
    onPrimaryContainer = ExpoDarkOnPrimaryContainer,
    secondary = ExpoDarkSecondary,
    onSecondary = ExpoDarkOnSecondary,
    secondaryContainer = ExpoDarkSecondaryContainer,
    onSecondaryContainer = ExpoDarkOnSecondaryContainer,
    tertiary = ExpoDarkTertiary,
    onTertiary = ExpoDarkOnTertiary,
    background = ExpoDarkBackground,
    onBackground = ExpoDarkOnBackground,
    surface = ExpoDarkSurface,
    onSurface = ExpoDarkOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = ExpoPrimary,
    onPrimary = ExpoOnPrimary,
    primaryContainer = ExpoPrimaryContainer,
    onPrimaryContainer = ExpoOnPrimaryContainer,
    secondary = ExpoSecondary,
    onSecondary = ExpoOnSecondary,
    secondaryContainer = ExpoSecondaryContainer,
    onSecondaryContainer = ExpoOnSecondaryContainer,
    tertiary = ExpoTertiary,
    onTertiary = ExpoOnTertiary,
    tertiaryContainer = ExpoTertiaryContainer,
    onTertiaryContainer = ExpoOnTertiaryContainer,
    background = ExpoBackground,
    onBackground = ExpoOnBackground,
    surface = ExpoSurface,
    onSurface = ExpoOnSurface,
    surfaceVariant = ExpoSurfaceVariant,
    onSurfaceVariant = ExpoOnSurfaceVariant,
    outline = ExpoOutline,
    outlineVariant = ExpoOutlineVariant
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
