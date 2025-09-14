package com.fpf.smartscan.lib

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.fpf.smartscan.R
import com.fpf.smartscansdk.core.ml.embeddings.Embedding
import com.fpf.smartscansdk.core.processors.IProcessorListener
import com.fpf.smartscansdk.core.processors.Metrics
import com.fpf.smartscansdk.core.processors.ProcessorStatus

abstract class BaseIndexListener(private val notificationId: Int, private val tag: String) : IProcessorListener<Long, Embedding> {
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _indexingStatus = MutableStateFlow(ProcessorStatus.IDLE)
    val indexingStatus: StateFlow<ProcessorStatus> = _indexingStatus

    abstract val itemName: String

    override suspend fun onProgress(context: Context, progress: Float) {
        if (_indexingStatus.value != ProcessorStatus.ACTIVE) {
            _indexingStatus.value = ProcessorStatus.ACTIVE
        }
        _progress.value = progress
    }

    override suspend fun onComplete(context: Context, metrics: Metrics.Success) {
        if (metrics.totalProcessed == 0) return
        try {
            _indexingStatus.value = ProcessorStatus.COMPLETE
            _progress.value = 0f
            val (minutes, seconds) = getTimeInMinutesAndSeconds(metrics.timeElapsed)
            val notificationText = "Total $itemName indexed: ${metrics.totalProcessed}, Time: ${minutes}m ${seconds}s"
            showNotification(context, context.getString(R.string.notif_title_index_complete), notificationText, notificationId)
        } catch (e: Exception) {
            Log.e(tag, "Error in onComplete: ${e.message}", e)
        }
    }

    override suspend fun onFail(context: Context, metrics: Metrics.Failure) {
        try {
            _indexingStatus.value = ProcessorStatus.FAILED
            _progress.value = 0f
            val title = context.getString(R.string.notif_title_index_error_service, itemName)
            val content = context.getString(R.string.notif_content_index_error_service, itemName.lowercase())
            showNotification(context, title, content, notificationId)
        } catch (e: Exception) {
            Log.e(tag, "Error in onFail: ${e.message}", e)
        }
    }
}

// Singletons
object ImageIndexListener : BaseIndexListener(
    notificationId = 1002,
    tag = "ImageIndexListener"
) {
    override val itemName: String = "Image"
}

object VideoIndexListener : BaseIndexListener(
    notificationId = 1002,
    tag = "VideoIndexListener"
) {
    override val itemName: String = "Video"
}