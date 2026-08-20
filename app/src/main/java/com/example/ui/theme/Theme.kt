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
    primary = ElegantLavenderPrimary,
    onPrimary = ElegantDeepViolet,
    primaryContainer = ElegantVioletContainer,
    onPrimaryContainer = ElegantOnVioletContainer,
    secondary = ElegantLavenderPrimary,
    onSecondary = ElegantDeepViolet,
    secondaryContainer = ElegantVioletContainer,
    onSecondaryContainer = ElegantOnVioletContainer,
    tertiary = ElegantLavenderPrimary,
    onTertiary = ElegantDeepViolet,
    tertiaryContainer = ElegantVioletContainer,
    onTertiaryContainer = ElegantOnVioletContainer,
    background = ElegantDarkBackground,
    onBackground = ElegantTextPrimary,
    surface = ElegantDarkSurface,
    onSurface = ElegantTextPrimary,
    surfaceVariant = ElegantDarkSurfaceVariant,
    onSurfaceVariant = ElegantTextSecondary,
    outline = ElegantDarkCardBorder,
    error = ElegantRose,
    onError = ElegantDeepViolet
)

private val LightColorScheme = lightColorScheme(
    primary = MummaPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFF4E2600),
    secondary = MummaSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCBE9FE),
    onSecondaryContainer = Color(0xFF003547),
    tertiary = MummaTertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFECE0FD),
    onTertiaryContainer = Color(0xFF2C0A63),
    background = MummaBackgroundLight,
    onBackground = MummaTextPrimaryLight,
    surface = MummaSurfaceLight,
    onSurface = MummaTextPrimaryLight,
    surfaceVariant = MummaSurfaceVariantLight,
    onSurfaceVariant = MummaTextSecondaryLight,
    outline = MummaCardBorderLight,
    error = MummaRose,
    onError = Color.White
)

@Composable
fun MummaTheme(
    darkTheme: Boolean = true, // Default to sleek futuristic dark theme, allows toggle
    dynamicColor: Boolean = false, // Keep Mumma's bespoke aesthetic
    content: @Composable () -> Unit,
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

