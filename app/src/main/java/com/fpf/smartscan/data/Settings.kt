package com.fpf.smartscan.data

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val enableScan: Boolean = false,
    val targetDirectories: List<String> = emptyList(),
    val frequency: String = "1 Day",
    val destinationDirectories: List<String> = emptyList(),
    val similarityThreshold: Float = 0.2f,
    val numberSimilarResults: Int = 10,
    val indexFrequency: String = "1 Week",
    val organiserSimilarityThreshold: Float = 0.4f,
    val organiserConfMargin: Float = 0.03f,
)
