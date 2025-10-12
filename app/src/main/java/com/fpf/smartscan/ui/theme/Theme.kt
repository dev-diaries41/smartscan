package com.fpf.smartscan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ColorSchemeType { DEFAULT, SMARTSCAN }

private val LightColorPalette = lightColorScheme(
    primary = Peach,
    onPrimary = Color.White,
    primaryContainer = Peach.copy(alpha = 0.5f),
    onPrimaryContainer = Color.Black
)

private val DarkColorPalette = darkColorScheme(
    primary = Peach,
    onPrimary = Color.Black,
    primaryContainer = Peach.copy(alpha = 0.5f),
    onPrimaryContainer = Color.White
)


@Composable
fun MyAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorSchemeType: ColorSchemeType = ColorSchemeType.SMARTSCAN,
    content: @Composable () -> Unit
) {
    val colors = when (colorSchemeType) {
        ColorSchemeType.DEFAULT -> if (darkTheme) darkColorScheme() else lightColorScheme()
        ColorSchemeType.SMARTSCAN -> if (darkTheme) DarkColorPalette else LightColorPalette
    }

    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}
