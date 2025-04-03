package com.fpf.smartscan.workers

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fpf.smartscan.data.images.ImageEmbedding
import com.fpf.smartscan.data.images.ImageEmbeddingDatabase
import com.fpf.smartscan.data.images.ImageEmbeddingRepository
import com.fpf.smartscan.lib.clip.Embeddings
import com.fpf.smartscan.lib.clip.ModelType
import com.fpf.smartscan.lib.getBitmapFromUri
import com.fpf.smartscan.lib.showNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class ImageBatchWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ImageBatchWorker"
        private const val PREFS_NAME = "ImageIndexPrefs"
        private const val KEY_TOTAL_PROCESSED_COUNT = "totalProcessedCount"
        private const val KEY_TOTAL_PROCESSING_TIME = "totalProcessingTime"
    }

    private val repository = ImageEmbeddingRepository(
        ImageEmbeddingDatabase.getDatabase(applicationContext as Application).imageEmbeddingDao()
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val sharedPreferences = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val embeddingHandler = Embeddings(applicationContext.resources, ModelType.IMAGE)
        try {
            val batchStartTime = System.currentTimeMillis()

            val batchIds = inputData.getLongArray("BATCH_IMAGE_IDS")?.toList() ?: emptyList()
            val isLastBatch = inputData.getBoolean("IS_LAST_BATCH", false)

            if (batchIds.isEmpty()) {
                Log.i(TAG, "No image IDs provided for this batch.")
                if (isLastBatch) {
                    resetSharedPreferences(sharedPreferences)
                }
                return@withContext Result.success()
            }

            Log.i(TAG, "Processing batch of ${batchIds.size} images.")

            val indexedIds: Set<Long> = repository.getAllEmbeddingsSync().map { it.id }.toSet()
            val semaphore = Semaphore(4)

            val deferredResults = batchIds.map { id ->
                if (indexedIds.contains(id)) return@map null
                async {
                    semaphore.withPermit {
                        try {
                            val contentUri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            )
                            val bitmap = getBitmapFromUri(applicationContext, contentUri)
                            val embedding: FloatArray? = embeddingHandler.generateImageEmbedding(bitmap)
                            bitmap.recycle()
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
                            Log.e(TAG, "Failed to process image $id", e)
                        }
                        return@async 0
                    }
                }
            }.filterNotNull()

            val processedCount = deferredResults.awaitAll().sum()
            Log.i(TAG, "Processed $processedCount images in this batch.")

            val previousTotal = sharedPreferences.getInt(KEY_TOTAL_PROCESSED_COUNT, 0)
            val newTotal = previousTotal + processedCount
            saveTotalProcessedCount(sharedPreferences, newTotal)

            val batchProcessingTime = System.currentTimeMillis() - batchStartTime
            val previousProcessingTime = sharedPreferences.getLong(KEY_TOTAL_PROCESSING_TIME, 0L)
            val newProcessingTime = previousProcessingTime + batchProcessingTime
            saveTotalProcessingTime(sharedPreferences, newProcessingTime)

            if (isLastBatch) {
                val processingTimeSeconds = newProcessingTime / 1000
                val minutes = processingTimeSeconds / 60
                val seconds = processingTimeSeconds % 60
                val notificationText = "Total images processed: $newTotal, Total processing time: ${minutes}m ${seconds}s"
                Log.i(TAG, notificationText)
                showNotification(applicationContext, "Indexing Complete", notificationText, 1002)
                resetSharedPreferences(sharedPreferences)
            }

            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing batch: ${e.message}", e)
            return@withContext Result.failure()
        } finally {
            embeddingHandler.closeSession()
        }
    }


    private fun saveTotalProcessedCount(sharedPreferences: SharedPreferences, count: Int) {
        sharedPreferences.edit { putInt(KEY_TOTAL_PROCESSED_COUNT, count) }
    }

    private fun saveTotalProcessingTime(sharedPreferences: SharedPreferences, time: Long) {
        sharedPreferences.edit { putLong(KEY_TOTAL_PROCESSING_TIME, time) }
    }

    private fun resetSharedPreferences(sharedPreferences: SharedPreferences) {
        sharedPreferences.edit {
            putInt(KEY_TOTAL_PROCESSED_COUNT, 0)
            putLong(KEY_TOTAL_PROCESSING_TIME, 0L)
        }
    }
}
