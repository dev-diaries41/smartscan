package com.fpf.smartscan.data

import com.fpf.smartscan.ui.theme.ColorSchemeType
import com.fpf.smartscan.ui.theme.ThemeMode
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val similarityThreshold: Float = 0.2f,
    val indexFrequency: String = "1 Week",
    val searchableImageDirectories: List<String> = emptyList(),
    val searchableVideoDirectories: List<String> = emptyList(),
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val color: ColorSchemeType = ColorSchemeType.SMARTSCAN
    )
