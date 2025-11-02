package com.fpf.smartscan.constants

import com.fpf.smartscan.data.ModelInfo
import com.fpf.smartscan.data.SmartScanModelType

val smartScanModelTypeOptions = mapOf (
    SmartScanModelType.FACE to  "Facial recognition",
    SmartScanModelType.OBJECTS to  "Object detection",
    SmartScanModelType.IMAGE_ENCODER to "Image encoder",
    SmartScanModelType.TEXT_ENCODER to "Text encoder"
)

// Models Info
const val MODEL_DIR = "models"
const val INCEPTION_RESNET_DEP_FACE_DETECTOR = "face_detect.onnx"
const val INCEPTION_RESNET_DEP_INCEPTION = "inception_resnet_v1_quant.onnx"

val facialRecognitionModel = ModelInfo(
    type = SmartScanModelType.FACE,
    name = "Inception Resnet V1",
    url = "https://github.com/dev-diaries41/smartscan-models/releases/download/1.0.0/facial_recognition_inception_resnet_v1.zip",
)

val miniLmTextEmbedderModel = ModelInfo(
    type = SmartScanModelType.TEXT_ENCODER,
    name = "MiniLM-L6-v2",
    url = "https://github.com/dev-diaries41/smartscan-models/releases/download/1.0.0/minilm_sentence_transformer_quant.onnx",
)

val downloadableModels = listOf(facialRecognitionModel, miniLmTextEmbedderModel)

