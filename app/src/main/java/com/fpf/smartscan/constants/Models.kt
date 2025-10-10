package com.fpf.smartscan.constants

object SmartScanModelTypes {
    const val FACE = "facial"
    const val OBJECTS = "object"
    const val IMAGE_ENCODER = "image_encoder"
    const val TEXT_ENCODER = "text_encoder"
}

val smartScanModelTypesOptions = mapOf (
    SmartScanModelTypes.FACE to  "Facial recognition",
    SmartScanModelTypes.OBJECTS to  "Object detection",
    SmartScanModelTypes.IMAGE_ENCODER to "Image encoder",
    SmartScanModelTypes.TEXT_ENCODER to "Text encoder"
)

const val MODEL_DIR = "models"

object ModelPaths {
    const val FACE = "$MODEL_DIR/${SmartScanModelTypes.FACE}.zip"
    const val OBJECTS =  "$MODEL_DIR/${SmartScanModelTypes.OBJECTS}.onnx"
    const val IMAGE_ENCODER = "$MODEL_DIR/${SmartScanModelTypes.IMAGE_ENCODER}.onnx"
    const val TEXT_ENCODER = "$MODEL_DIR/${SmartScanModelTypes.TEXT_ENCODER}.onnx"
}