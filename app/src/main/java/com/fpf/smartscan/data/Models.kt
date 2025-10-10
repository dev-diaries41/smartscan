package com.fpf.smartscan.data

object SmartScanModelTypes {
    const val FACE = "facial"
    const val OBJECTS = "object"
    const val IMAGE_ENCODER = "image encoder"
    const val TEXT_ENCODER = "text encoder"
}

data class DownloadableModel(
    val type: String,
    val name: String,
    val url: String,
    val dependentModels: List<DownloadableModel>? = null
)