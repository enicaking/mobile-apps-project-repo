package com.example.pearpressure.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    tertiary = Tertiary,
    onTertiary = OnTertiary
)

/**
 * Theme.kt: puts together Color, Shapes and Type
 * contentReference[oaicite:7]{index=7}
 */
private val GreenColorScheme = lightColorScheme(
    primary = Color(0xFF89AC48),
    secondary = Color(0xFFC4DA70),
    tertiary = Color(0xFFC4DA70),

    primaryContainer = Color(0xFFC4DA70),
    secondaryContainer = Color(0xFFEAF3C8),

    onPrimary = Color.White,
    onSecondary = Color(0xFF1F2A10),
    onPrimaryContainer = Color(0xFF1F2A10),
    onSecondaryContainer = Color(0xFF1F2A10)
)

@Composable
fun TestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = GreenColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}