package com.fpf.smartscan

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.fpf.smartscan.ui.theme.ColorSchemeType
import com.fpf.smartscan.ui.theme.DarkColorPalette
import com.fpf.smartscan.ui.theme.LightColorPalette
import com.fpf.smartscan.ui.theme.ThemeManager
import com.fpf.smartscan.ui.theme.ThemeMode


@Composable
fun App(
    content: @Composable () -> Unit
) {
    val themeMode by ThemeManager.themeMode.collectAsState()
    val colorSchemeType by ThemeManager.colorScheme.collectAsState()

    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

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
