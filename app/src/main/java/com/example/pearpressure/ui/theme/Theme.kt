/* Theme.kt
This file combines design elements into a theme for the app */
package com.example.pearpressure.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Custom green color scheme
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