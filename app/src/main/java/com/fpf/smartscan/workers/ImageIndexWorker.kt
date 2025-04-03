package com.fpf.smartscan.workers

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.work.*
import com.fpf.smartscan.data.images.ImageEmbedding
import com.fpf.smartscan.data.images.ImageEmbeddingDatabase
import com.fpf.smartscan.data.images.ImageEmbeddingRepository
import com.fpf.smartscan.lib.showNotification
import com.fpf.smartscan.lib.clip.Embeddings
import com.fpf.smartscan.lib.clip.ModelType
import com.fpf.smartscan.lib.getBitmapFromUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.*

class ImageIndexWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    private val repository = ImageEmbeddingRepository(
        ImageEmbeddingDatabase.getDatabase(applicationContext as Application).imageEmbeddingDao()
    )


    override suspend fun doWork(): Result {
        val embeddingHandler = Embeddings(applicationContext.resources, ModelType.IMAGE)
        val tag = "ImageIndexWorker"
        try {
            var count = 0
            val timeTaken = measureTimeMillis {
                count = indexAllImages(applicationContext, repository, embeddingHandler)
            }
            val minutes = timeTaken / 60000
            val seconds = (timeTaken % 60000) / 1000
            val metricLog = if (count == 0) {
                "No images indexed"
            } else {
                "$count images processed in $minutes min $seconds sec."
            }
            Log.i(tag, metricLog)
            showNotification(applicationContext, "Indexing complete", metricLog, 1002)
            return Result.success()
        } catch (e: Exception) {
            Log.e(tag, "Error during work: ${e.message}", e)
            return Result.failure()
        } finally {
            embeddingHandler.closeSession()
        }
    }


    suspend fun indexAllImages(context: Context, repository: ImageEmbeddingRepository, embeddingsHandler: Embeddings
    ): Int = withContext(Dispatchers.IO) {
        val indexedIds: Set<Long> = repository.getAllEmbeddingsSync().map { it.id }.toSet()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        val imageIds = mutableListOf<Long>()
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                imageIds.add(cursor.getLong(idColumn))
            }
        }

        val semaphore = Semaphore(4)
        val deferredResults = imageIds.map { id ->
            if (indexedIds.contains(id)) return@map null
            async {
                semaphore.withPermit {
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    try {
                        val bitmap = getBitmapFromUri(context, contentUri)
                        val embedding: FloatArray? =
                            embeddingsHandler.generateImageEmbedding(bitmap)
                        if (embedding != null) {
                            repository.insert(
                                ImageEmbedding(
                                id = id,
                                date = System.currentTimeMillis(),
                                embeddings = embedding
                                )
                            )
                            return@async 1
                        }
                    } catch (e: Exception) {
                        Log.e("ImageEmbeddingInsertionError", "Failed to process image $id", e)
                    }
                    return@async 0
                }
            }
        }.filterNotNull()

        deferredResults.awaitAll().sum()
    }
}


    fun scheduleImageIndexWorker(context: Context, frequency: String) {
        val workerName = "ImageIndexWorker"
        val duration = when (frequency) {
            "1 Day" -> 1L to TimeUnit.DAYS
            "1 Week" -> 7L to TimeUnit.DAYS
            else -> {
                Log.e("ImageIndexWorker", "Invalid frequency: $frequency, defaulting to 1 Week")
                7L to TimeUnit.DAYS
            }
        }

        val workRequest = PeriodicWorkRequestBuilder<ImageIndexWorker>(duration.first, duration.second)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workerName,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
