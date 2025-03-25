package com.fpf.smartscan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
// Color scheme for light mode
private val LightColorPalette = lightColorScheme(
    primary = Blue500,
    onPrimary = Color.White,
    secondary = Teal200,
    onSecondary = Color.Black,
    background = Color.White,  // Use your custom background color
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

// Color scheme for dark mode
private val DarkColorPalette = darkColorScheme(
    primary = Blue200,
    onPrimary = Color.Black,
    secondary = Teal200,
    onSecondary = Color.Black,
    background = Color.Black,  // Define background for dark mode
    surface = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun MyAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorPalette else LightColorPalette

    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}
