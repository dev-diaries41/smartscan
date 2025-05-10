package com.fpf.smartscan.lib.processors

import android.app.Application
import android.content.ContentUris
import android.provider.MediaStore
import android.util.Log
import com.fpf.smartscan.data.videos.VideoEmbedding
import com.fpf.smartscan.data.videos.VideoEmbeddingDatabase
import com.fpf.smartscan.data.videos.VideoEmbeddingRepository
import com.fpf.smartscan.lib.clip.Embeddings
import com.fpf.smartscan.lib.clip.ModelType
import com.fpf.smartscan.lib.MemoryUtils
import com.fpf.smartscan.lib.extractFramesFromVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class VideoIndexer(
    private val application: Application,
    private val listener: VideoIndexListener? = null
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
                                    listener?.onProgress(current)
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

interface VideoIndexListener {
    fun onProgress(processedCount: Int)
    // Additional callbacks can be added as needed:
    // fun onError(imageId: Long, exception: Exception)
    // fun onComplete(totalProcessed: Int)
}
