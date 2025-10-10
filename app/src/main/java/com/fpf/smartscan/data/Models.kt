package com.fpf.smartscan.data

enum class SmartScanModelType(val fileName: String) {
    FACE("facial"),
    OBJECTS("object"),
    IMAGE_ENCODER("image_encoder"),
    TEXT_ENCODER("text_encoder")
}

data class DownloadableModel(
    val type: SmartScanModelType,
    val name: String,
    val url: String,
)