package com.fpf.smartscan.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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
}
