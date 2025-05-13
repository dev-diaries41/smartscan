package com.fpf.smartscan.lib.processors

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.fpf.smartscan.R
import com.fpf.smartscan.data.images.ImageEmbedding
import com.fpf.smartscan.data.images.ImageEmbeddingDatabase
import com.fpf.smartscan.data.images.ImageEmbeddingRepository
import com.fpf.smartscan.lib.clip.Embeddings
import com.fpf.smartscan.lib.clip.ModelType
import com.fpf.smartscan.lib.getBitmapFromUri
import com.fpf.smartscan.lib.MemoryUtils
import com.fpf.smartscan.lib.getTimeInMinutesAndSeconds
import com.fpf.smartscan.lib.showNotification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class ImageIndexer(
    private val application: Application,
    private val listener: IIndexListener? = null
) {
    var embeddingHandler: Embeddings? = null

    companion object {
        private const val TAG = "ImageIndexer"
    }

    private val repository = ImageEmbeddingRepository(
        ImageEmbeddingDatabase.getDatabase(application).imageEmbeddingDao()
    )

    private val memoryUtils = MemoryUtils(application.applicationContext)

    init {
        embeddingHandler = Embeddings(application.resources, ModelType.IMAGE)
    }

    suspend fun indexImages(imageIds: List<Long>): Int = withContext(Dispatchers.IO) {
        val processedCount = AtomicInteger(0)
        val startTime = System.currentTimeMillis()

        try {
            if (imageIds.isEmpty()) {
                Log.i(TAG, "No images found.")
                return@withContext 0
            }

            val indexedIds: Set<Long> = repository.getAllEmbeddingsSync()
                .map { it.id }
                .toSet()
            val imagesToProcess = imageIds.filterNot { indexedIds.contains(it) }
            var totalProcessed = 0

            for (batch in imagesToProcess.chunked(10)) {
                val currentConcurrency = memoryUtils.calculateConcurrencyLevel()
                // Log.i(TAG, "Current allowed concurrency: $currentConcurrency | Free Memory: ${memoryUtils.getFreeMemory() / (1024 * 1024)} MB")

                val semaphore = Semaphore(currentConcurrency)

                val deferredResults = batch.map { id ->
                    async {
                        semaphore.withPermit {
                            try {
                                val contentUri = ContentUris.withAppendedId(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                                )
                                val bitmap = getBitmapFromUri(application, contentUri)
                                val embedding = withContext(NonCancellable) {
                                    embeddingHandler?.generateImageEmbedding(bitmap)
                                }

                                bitmap.recycle()
                                if (embedding != null) {
                                    repository.insert(
                                        ImageEmbedding(
                                            id = id,
                                            date = System.currentTimeMillis(),
                                            embeddings = embedding
                                        )
                                    )
                                    val current = processedCount.incrementAndGet()
                                    listener?.onProgress(current, imagesToProcess.size)
                                    return@async 1
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to process image $id", e)
                            }
                            return@async 0
                        }
                    }
                }
                totalProcessed += deferredResults.awaitAll().sum()
            }
            val endTime = System.currentTimeMillis()
            val completionTime = endTime - startTime
            listener?.onComplete(application, totalProcessed, completionTime)
            totalProcessed
        }
        catch (e: CancellationException) {
            throw e
        }
        catch (e: Exception) {
            listener?.onError(application, e)
            Log.e(TAG, "Error indexing images: ${e.message}", e)
            0
        }
    }


    fun close() {
        embeddingHandler?.closeSession()
        embeddingHandler = null
    }
}

object ImageIndexListener : IIndexListener {
    const val NOTIFICATION_ID = 1002
    const val TAG = "ImageIndexListener"
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _indexingInProgress = MutableStateFlow(false)
    val indexingInProgress: StateFlow<Boolean> = _indexingInProgress

    override fun onProgress(processedCount: Int, total: Int) {
        val currentProgress = processedCount.toFloat() / total.toFloat()
        if(currentProgress > 0f){
            if(!_indexingInProgress.value){
                _indexingInProgress.value = true
            }

            _progress.value = currentProgress
        }
    }

    override fun onComplete(context: Context, totalProcessed: Int, processingTime: Long) {
        if (totalProcessed == 0) return

        try {
            _indexingInProgress.value = false
            val (minutes, seconds) = getTimeInMinutesAndSeconds(processingTime)
            val notificationText = "Total images indexed: ${totalProcessed}, Time: ${minutes}m ${seconds}s"
            showNotification(context, context.getString(R.string.notif_title_index_complete), notificationText, NOTIFICATION_ID)
        }
        catch (e: Exception){
            Log.e(TAG, "Error in onComplete: ${e.message}", e)
        }
    }

    override fun onError(context: Context, error: Exception) {
        try {
            _indexingInProgress.value = false
            val title = context.getString(R.string.notif_title_index_error_service, "Image")
            val content = context.getString(R.string.notif_content_index_error_service, "image")
            showNotification(context, title, content, NOTIFICATION_ID)
        }
        catch (e: Exception){
            Log.e(TAG, "Error in onError: ${e.message}", e)
        }
    }

}