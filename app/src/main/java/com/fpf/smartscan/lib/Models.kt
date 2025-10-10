package com.fpf.smartscan.lib

import android.content.Context
import com.fpf.smartscan.R
import com.fpf.smartscan.data.DownloadableModel
import com.fpf.smartscan.data.SmartScanModelType

fun getModels(context: Context): List<DownloadableModel>{
    val facialRecognitionModel = DownloadableModel(
        type = SmartScanModelType.FACE,
        name = context.getString(R.string.facial_recognition_model_name),
        url = context.getString(R.string.inception_resnet_v1_model_url),
    )
    return listOf(facialRecognitionModel)
}