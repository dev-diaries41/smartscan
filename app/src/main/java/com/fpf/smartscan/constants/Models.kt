package com.fpf.smartscan.constants

import com.fpf.smartscan.data.ModelPathInfo
import com.fpf.smartscan.data.SmartScanModelType

val smartScanModelTypeOptions = mapOf (
    SmartScanModelType.FACE to  "Facial recognition",
    SmartScanModelType.OBJECTS to  "Object detection",
    SmartScanModelType.IMAGE_ENCODER to "Image encoder",
    SmartScanModelType.TEXT_ENCODER to "Text encoder"
)

const val MODEL_DIR = "models"

val modelPathsMap = mapOf(
    SmartScanModelType.FACE to ModelPathInfo(
        path = "$MODEL_DIR/${SmartScanModelType.FACE.tag}.zip",
        dependentModelPaths = listOf(
            "$MODEL_DIR/${SmartScanModelType.FACE.tag}/face_detect.onnx",
            "$MODEL_DIR/${SmartScanModelType.FACE.tag}/inception_resnet_v1_quant.onnx"
        )
    ),
    SmartScanModelType.OBJECTS to ModelPathInfo("$MODEL_DIR/${SmartScanModelType.OBJECTS.tag}.onnx"),
    SmartScanModelType.IMAGE_ENCODER to ModelPathInfo("$MODEL_DIR/${SmartScanModelType.IMAGE_ENCODER.tag}.onnx"),
    SmartScanModelType.TEXT_ENCODER to ModelPathInfo("$MODEL_DIR/${SmartScanModelType.TEXT_ENCODER.tag}.onnx")
)
