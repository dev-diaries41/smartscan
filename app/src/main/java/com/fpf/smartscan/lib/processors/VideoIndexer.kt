package com.fpf.smartscan.lib.processors

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.fpf.smartscan.R
import com.fpf.smartscan.data.videos.VideoEmbedding
import com.fpf.smartscan.data.videos.VideoEmbeddingDatabase
import com.fpf.smartscan.data.videos.VideoEmbeddingRepository
import com.fpf.smartscan.lib.clip.Embeddings
import com.fpf.smartscan.lib.clip.ModelType
import com.fpf.smartscan.lib.MemoryUtils
import com.fpf.smartscan.lib.extractFramesFromVideo
import com.fpf.smartscan.lib.getTimeInMinutesAndSeconds
import com.fpf.smartscan.lib.showNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class VideoIndexer(
    private val application: Application,
    private val listener: IIndexListener? = null
) {
    var embeddingHandler: Embeddings? = null

    companion object {
        private const val TAG = "VideoIndexer"
    }

    private val repository = VideoEmbeddingRepository(
        VideoEmbeddingDatabase.getDatabase(application).videoEmbeddingDao()
    )

    private val memoryUtils = MemoryUtils(application.applicationContext)

    init {
        embeddingHandler = Embeddings(application.resources, ModelType.IMAGE)
    }

    suspend fun indexVideos(ids: List<Long>): Int = withContext(Dispatchers.IO) {
        val processedCount = AtomicInteger(0)
        val startTime = System.currentTimeMillis()

        try {
            if (ids.isEmpty()) {
                Log.d(TAG, "No videos to index.")
                return@withContext 0
            }

            val existingIds: Set<Long> = repository.getAllEmbeddingsSync()
                .map { it.id }
                .toSet()
            val videosToProcess = ids.filterNot { existingIds.contains(it) }
            var totalProcessed = 0

            for (batch in videosToProcess.chunked(10)) {
                val currentConcurrency = memoryUtils.calculateConcurrencyLevel()
                // Log.i(TAG, "Current allowed concurrency: $currentConcurrency | Free Memory: ${memoryUtils.getFreeMemory() / (1024 * 1024)} MB")

                val semaphore = Semaphore(currentConcurrency)

                val deferredResults = batch.map { id ->
                    async {
                        semaphore.withPermit {
                            try {
                                val contentUri = ContentUris.withAppendedId(
                                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                                )
                                val frameBitmaps = extractFramesFromVideo(application, contentUri)

                                if(frameBitmaps == null) return@async 0

                                val embedding: FloatArray? = embeddingHandler?.generatePrototypeEmbedding(frameBitmaps)

                                if (embedding != null) {
                                    repository.insert(
                                        VideoEmbedding(
                                            id = id,
                                            date = System.currentTimeMillis(),
                                            embeddings = embedding
                                        )
                                    )
                                    val current = processedCount.incrementAndGet()
                                    listener?.onProgress(current, videosToProcess.size)
                                    return@async 1
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to process video $id", e)
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
        } catch (e: Exception) {
            Log.e(TAG, "Error indexing videos: ${e.message}", e)
            0
        }
    }


    fun close() {
        embeddingHandler?.closeSession()
        embeddingHandler = null
    }
}

object VideoIndexListener : IIndexListener {
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    override fun onProgress(processedCount: Int, total: Int) {
        val currentProgress = processedCount.toFloat() / total.toFloat()
        if(currentProgress > 0f){
            _progress.value = currentProgress
        }
    }

    override fun onComplete(context: Context, totalProcessed: Int, processingTime: Long) {
        if (totalProcessed == 0) return

        try {
            val (minutes, seconds) = getTimeInMinutesAndSeconds(processingTime)
            val notificationText = "Total videos indexed: ${totalProcessed}, Time: ${minutes}m ${seconds}s"
            showNotification(context, context.getString(R.string.notif_title_index_complete), notificationText, 1003)
        }
        catch (e: Exception){
            Log.e("VideoIndexListener", "Error in onComplete ${e.message}", e)
        }
    }
}
