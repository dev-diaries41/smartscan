package com.fpf.smartscan.constants

import com.fpf.smartscan.data.DownloadableModel
import com.fpf.smartscan.data.SmartScanModelTypes

val faceDetectorModel = DownloadableModel(type = SmartScanModelTypes.FACE, name = "Face detector", url = "")
val facialRecognitionModel = DownloadableModel(
    type = SmartScanModelTypes.FACE,
    name = "Facial recognition",
    url = "",
    dependentModels = listOf(faceDetectorModel)
)