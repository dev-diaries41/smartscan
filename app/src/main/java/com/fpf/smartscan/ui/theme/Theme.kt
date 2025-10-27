package com.fpf.smartscan.ui.theme

import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ColorSchemeType { DEFAULT, SMARTSCAN }
enum class ThemeMode { LIGHT, DARK, SYSTEM }

val LightColorPalette = lightColorScheme(
    primary = Peach,
    onPrimary = Color.White,
    primaryContainer = Peach.copy(alpha = 0.5f),
    onPrimaryContainer = Color.Black
)

val DarkColorPalette = darkColorScheme(
    primary = Peach,
    onPrimary = Color.Black,
    primaryContainer = Peach.copy(alpha = 0.5f),
    onPrimaryContainer = Color.White
)

object ThemeManager {
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode = _themeMode.asStateFlow()

    private val _colorScheme = MutableStateFlow(ColorSchemeType.SMARTSCAN)
    val colorScheme = _colorScheme.asStateFlow()

    fun updateThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun updateColorScheme(scheme: ColorSchemeType) {
        _colorScheme.value = scheme
    }

    fun isDarkTheme(resources: Resources): Boolean {
        return when (_themeMode.value) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
    }
}


