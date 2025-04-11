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
                    resetMetrics(sharedPreferences)
                }
                return@withContext Result.success()
            }

            Log.i(TAG, "Processing batch of ${batchIds.size} images.")

            val processedCount = imageIndexer.indexImages(batchIds)

            Log.i(TAG, "Processed $processedCount images in this batch.")

            val (updatedTotal, updatedProcessingTime) = updateMetrics(sharedPreferences, processedCount, batchStartTime)

            if (isLastBatch) {
                onLastBatch(sharedPreferences, updatedProcessingTime, updatedTotal)
            }

            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing batch: ${e.message}", e)
            return@withContext Result.failure()
        } finally {
            imageIndexer.onComplete()
        }
    }

    private fun onLastBatch(preferences: SharedPreferences, totalProcessingTime: Long, totalProcessedCount: Int){
        val processingTimeSeconds = totalProcessingTime / 1000
        val minutes = processingTimeSeconds / 60
        val seconds = processingTimeSeconds % 60
        val notificationText = "Total images processed: $totalProcessedCount, Total processing time: ${minutes}m ${seconds}s"
        Log.i(TAG, notificationText)
        showNotification(applicationContext, "Indexing Complete", notificationText, 1002)
        resetMetrics(preferences)
    }

    private fun updateMetrics(preferences: SharedPreferences, processedCount: Int, batchStartTime: Long): Pair<Int, Long> {
        val previousTotal = preferences.getInt(KEY_TOTAL_PROCESSED_COUNT, 0)
        val updatedProcessedCount = previousTotal + processedCount
        preferences.edit { putInt(KEY_TOTAL_PROCESSED_COUNT, updatedProcessedCount) }

        val batchProcessingTime = System.currentTimeMillis() - batchStartTime
        val previousProcessingTime = preferences.getLong(KEY_TOTAL_PROCESSING_TIME, 0L)
        val updatedProcessingTime = previousProcessingTime + batchProcessingTime
        preferences.edit { putLong(KEY_TOTAL_PROCESSING_TIME, updatedProcessingTime) }
        return Pair(updatedProcessedCount, updatedProcessingTime)
    }

    private fun resetMetrics(sharedPreferences: SharedPreferences) {
        sharedPreferences.edit {
            putInt(KEY_TOTAL_PROCESSED_COUNT, 0)
            putLong(KEY_TOTAL_PROCESSING_TIME, 0L)
        }
    }
}
