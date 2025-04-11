package com.fpf.smartscan.lib.processors

import android.app.ActivityManager
import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.fpf.smartscan.data.images.ImageEmbedding
import com.fpf.smartscan.data.images.ImageEmbeddingDatabase
import com.fpf.smartscan.data.images.ImageEmbeddingRepository
import com.fpf.smartscan.lib.clip.Embeddings
import com.fpf.smartscan.lib.clip.ModelType
import com.fpf.smartscan.lib.getBitmapFromUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class ImageIndexer(
    private val application: Application,
    private val listener: ImageIndexListener? = null
) {
    var embeddingHandler: Embeddings? = null

    companion object {
        private const val TAG = "ImageIndexer"
        private const val LOW_MEMORY_THRESHOLD = 800L * 1024 * 1024
        private const val HIGH_MEMORY_THRESHOLD = 1_600L * 1024 * 1024
        private const val MIN_CONCURRENCY = 1
        private const val MAX_CONCURRENCY = 4
    }

    private val repository = ImageEmbeddingRepository(
        ImageEmbeddingDatabase.getDatabase(application).imageEmbeddingDao()
    )

    init {
        embeddingHandler = Embeddings(application.resources, ModelType.IMAGE)
    }

    private fun getFreeMemory(): Long {
        val activityManager = application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem
    }

    private fun calculateConcurrencyLevel(): Int {
        val freeMemory = getFreeMemory()
        return when {
            freeMemory < LOW_MEMORY_THRESHOLD -> MIN_CONCURRENCY
            freeMemory >= HIGH_MEMORY_THRESHOLD -> MAX_CONCURRENCY
            else -> {
                val proportion = (freeMemory - LOW_MEMORY_THRESHOLD).toDouble() /
                        (HIGH_MEMORY_THRESHOLD - LOW_MEMORY_THRESHOLD)
                (MIN_CONCURRENCY + proportion * (MAX_CONCURRENCY - MIN_CONCURRENCY)).toInt().coerceAtLeast(MIN_CONCURRENCY)
            }
        }
    }

    suspend fun indexImages(imageIds: List<Long>): Int = withContext(Dispatchers.IO) {
        val processedCount = AtomicInteger(0)
        try {
            if (imageIds.isEmpty()) {
                Log.i(TAG, "No images found.")
                return@withContext 0
            }

            val indexedIds: Set<Long> = repository.getAllEmbeddingsSync().map { it.id }.toSet()
            val imagesToProcess = imageIds.filterNot { indexedIds.contains(it) }
            var totalProcessed = 0

            for (batch in imagesToProcess.chunked(10)) {
                val currentConcurrency = calculateConcurrencyLevel()
//                Log.i(TAG, "Current allowed concurrency: $currentConcurrency | Free Memory: ${getFreeMemory() / (1024 * 1024)} MB")

                val semaphore = Semaphore(currentConcurrency)

                val deferredResults = batch.map { id ->
                    async {
                        semaphore.withPermit {
                            try {
                                val contentUri = ContentUris.withAppendedId(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    id
                                )
                                val bitmap = getBitmapFromUri(application, contentUri)
                                val embedding: FloatArray? = embeddingHandler?.generateImageEmbedding(bitmap)
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
                                    listener?.onProgress(current)
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

            return@withContext totalProcessed
        } catch (e: Exception) {
            Log.e(TAG, "Error indexing images: ${e.message}", e)
            return@withContext 0
        }
    }

    fun close() {
        embeddingHandler?.closeSession()
        embeddingHandler = null
    }
}

interface ImageIndexListener {
    fun onProgress(processedCount: Int)
    // Additional callbacks can be added as needed:
    // fun onError(imageId: Long, exception: Exception)
    // fun onComplete(totalProcessed: Int)
}
