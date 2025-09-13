package com.fpf.smartscan.lib.processors

import android.content.Context
import android.util.Log
import com.fpf.smartscan.lib.showNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.fpf.smartscan.R
import com.fpf.smartscan.lib.getTimeInMinutesAndSeconds
import com.fpf.smartscansdk.core.ml.embeddings.Embedding
import com.fpf.smartscansdk.core.processors.IProcessorListener
import com.fpf.smartscansdk.core.processors.Metrics
import com.fpf.smartscansdk.core.processors.ProcessorStatus
import com.fpf.smartscansdk.extensions.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.extensions.indexers.ImageIndexer
import java.io.File


object ImageIndexListener : IProcessorListener<Long, Embedding> {
    const val NOTIFICATION_ID = 1002
    const val TAG = "ImageIndexListener"
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    var store: FileEmbeddingStore? = null

    private val _indexingStatus = MutableStateFlow<ProcessorStatus>(ProcessorStatus.IDLE)
    val indexingStatus: StateFlow<ProcessorStatus> = _indexingStatus

    override suspend fun onProgress(context: Context, progress: Float) {
        if(_indexingStatus.value != ProcessorStatus.ACTIVE){
            _indexingStatus.value = ProcessorStatus.ACTIVE
        }
        _progress.value = progress
    }

    override suspend fun onBatchComplete(context: Context, batch: List<Embedding>) {
        if(store == null){
            val file = File(context.filesDir, ImageIndexer.INDEX_FILENAME)
            store = FileEmbeddingStore(file, 512)

        }
        store?.add(batch)
    }

    override suspend fun onComplete(context: Context, metrics: Metrics.Success) {
        if (metrics.totalProcessed == 0) return

        try {
            _indexingStatus.value = ProcessorStatus.COMPLETE
            _progress.value = 0f
            val (minutes, seconds) = getTimeInMinutesAndSeconds(metrics.timeElapsed)
            val notificationText = "Total images indexed: ${metrics.totalProcessed}, Time: ${minutes}m ${seconds}s"
            showNotification(context, context.getString(R.string.notif_title_index_complete), notificationText, NOTIFICATION_ID)
        }
        catch (e: Exception){
            Log.e(TAG, "Error in onComplete: ${e.message}", e)
        }
    }

    override suspend fun onError(context: Context, metrics: Metrics.Failure) {
        try {
            _indexingStatus.value = ProcessorStatus.FAILED
            _progress.value = 0f
            val title = context.getString(R.string.notif_title_index_error_service, "Image")
            val content = context.getString(R.string.notif_content_index_error_service, "image")
            showNotification(context, title, content, NOTIFICATION_ID)
        }
        catch (e: Exception){
            Log.e(TAG, "Error in onError: ${e.message}", e)
        }
    }

}


object VideoIndexListener : IProcessorListener<Long, Embedding> {
    const val NOTIFICATION_ID = 1002
    const val TAG = "VideoIndexListener"
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _indexingStatus = MutableStateFlow<ProcessorStatus>(ProcessorStatus.IDLE)
    val indexingStatus: StateFlow<ProcessorStatus> = _indexingStatus

    override suspend fun onProgress(context: Context, progress: Float) {
        if(_indexingStatus.value != ProcessorStatus.ACTIVE){
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
            val notificationText = "Total videos indexed: ${metrics.totalProcessed}, Time: ${minutes}m ${seconds}s"
            showNotification(context, context.getString(R.string.notif_title_index_complete), notificationText, NOTIFICATION_ID)
        }
        catch (e: Exception){
            Log.e(TAG, "Error in onComplete: ${e.message}", e)
        }
    }

    override suspend fun onError(context: Context, metrics: Metrics.Failure) {
        try {
            _indexingStatus.value = ProcessorStatus.FAILED
            _progress.value = 0f
            val title = context.getString(R.string.notif_title_index_error_service, "Video")
            val content = context.getString(R.string.notif_content_index_error_service, "video")
            showNotification(context, title, content, NOTIFICATION_ID)
        }
        catch (e: Exception){
            Log.e(TAG, "Error in onError: ${e.message}", e)
        }
    }

}
