package com.fpf.smartscan.lib.processors

import android.app.Application
import android.content.ContentUris
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
){
    var embeddingHandler: Embeddings? = null

    companion object {
        private const val TAG = "ImageIndexer"
    }

    private val repository = ImageEmbeddingRepository(
        ImageEmbeddingDatabase.getDatabase(application).imageEmbeddingDao()
    )

    init {
        embeddingHandler = Embeddings(application.resources, ModelType.IMAGE)
    }

    suspend fun indexImages(imageIds: List<Long>): Int = withContext(Dispatchers.IO) {
        val processedCount = AtomicInteger(0)
        try {
            if (imageIds.isEmpty()) {
                Log.i(TAG, "No images found.")
                return@withContext 0
            }

            Log.i(TAG, "Processing ${imageIds.size} images.")

            val indexedIds: Set<Long> = repository.getAllEmbeddingsSync().map { it.id }.toSet()
            val semaphore = Semaphore(4)

            val deferredResults = imageIds.map { id ->
                if (indexedIds.contains(id)) return@map null
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
            }.filterNotNull()

            val processedCount = deferredResults.awaitAll().sum()

            return@withContext processedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error indexing images: ${e.message}", e)
            return@withContext 0
        }
    }

    fun close(){
        embeddingHandler?.closeSession()
        embeddingHandler = null
    }
}

interface ImageIndexListener {
    fun onProgress(processedCount: Int)
//    fun onError(imageId: Long, exception: Exception)
//    fun onComplete(totalProcessed: Int)
}
