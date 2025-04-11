package com.fpf.smartscan.workers

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fpf.smartscan.lib.processors.ImageIndexer
import com.fpf.smartscan.lib.showNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageBatchWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ImageBatchWorker"
        private const val PREFS_NAME = "ImageIndexPrefs"
        private const val KEY_TOTAL_PROCESSED_COUNT = "totalProcessedCount"
        private const val KEY_TOTAL_PROCESSING_TIME = "totalProcessingTime"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val sharedPreferences = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val imageIndexer = ImageIndexer(applicationContext as Application)
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

            val processedCount = imageIndexer.indexImages(batchIds)

            Log.i(TAG, "Processed $processedCount images in this batch.")

            // Update the total processed count and processing time
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
            imageIndexer.onComplete()
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
