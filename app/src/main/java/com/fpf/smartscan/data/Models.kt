package com.fpf.smartscan.data

enum class SmartScanModelType(val tag: String) {
    FACE("facial"),
    OBJECTS("object"),
    IMAGE_ENCODER("image_encoder"),
    TEXT_ENCODER("text_encoder")
}

data class ModelInfo(
    val type: SmartScanModelType,
    val name: String,
    val url: String,
)

data class ModelPathInfo(
    val path: String,
    val dependentModelPaths: List<String> = emptyList()
)

data class ImportedModel(
    val type: SmartScanModelType,
    val name: String,
    val dependentModelPaths: List<String> = emptyList()
)