package com.fpf.smartscan.constants

import com.fpf.smartscan.data.SmartScanModelType

val smartScanModelTypeOptions = mapOf (
    SmartScanModelType.FACE to  "Facial recognition",
    SmartScanModelType.OBJECTS to  "Object detection",
    SmartScanModelType.IMAGE_ENCODER to "Image encoder",
    SmartScanModelType.TEXT_ENCODER to "Text encoder"
)

const val MODEL_DIR = "models"

val modelPathsMap = mapOf(
    SmartScanModelType.FACE to "$MODEL_DIR/${SmartScanModelType.FACE.fileName}.zip",
    SmartScanModelType.OBJECTS to "$MODEL_DIR/${SmartScanModelType.OBJECTS.fileName}.onnx",
    SmartScanModelType.IMAGE_ENCODER to "$MODEL_DIR/${SmartScanModelType.IMAGE_ENCODER.fileName}.onnx",
    SmartScanModelType.TEXT_ENCODER to "$MODEL_DIR/${SmartScanModelType.TEXT_ENCODER.fileName}.onnx"
)
