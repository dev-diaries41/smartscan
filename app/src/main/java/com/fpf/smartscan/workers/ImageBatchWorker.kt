package com.fpf.smartscan.workers

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.fpf.smartscan.lib.processors.ImageIndexListener
import com.fpf.smartscan.lib.processors.ImageIndexer
import com.fpf.smartscan.lib.showNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageBatchWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams), ImageIndexListener {

    private val imageIndexer = ImageIndexer(context.applicationContext as Application, this) // Passing listener
    val sharedPreferences = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)!!
    val previousProcessingCount = sharedPreferences.getInt(KEY_TOTAL_PROCESSED_COUNT, 0)
    val batchStartTime = System.currentTimeMillis()
    val batchIds = inputData.getLongArray("BATCH_IMAGE_IDS")?.toList() ?: emptyList()
    val isLastBatch = inputData.getBoolean("IS_LAST_BATCH", false)
    val totalImageIds = inputData.getInt("TOTAL_IMAGES_IDS", 0)

    companion object {
        private const val TAG = "ImageBatchWorker"
        private const val PREFS_NAME = "ImageIndexPrefs"
        private const val KEY_TOTAL_PROCESSED_COUNT = "totalProcessedCount"
        private const val KEY_TOTAL_PROCESSING_TIME = "totalProcessingTime"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            if (batchIds.isEmpty()) {
                Log.i(TAG, "No image IDs provided for this batch.")
                return@withContext Result.success()
            }

            Log.i(TAG, "Processing batch of ${batchIds.size} images.")

            val processedCount = imageIndexer.indexImages(batchIds)

            Log.i(TAG, "Processed $processedCount images in this batch.")

            val (updatedTotal, updatedProcessingTime) = updateMetrics(previousProcessingCount, processedCount, batchStartTime)

            if (isLastBatch) {
                onLastBatch(updatedProcessingTime, updatedTotal)
            }

            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing batch: ${e.message}", e)
            return@withContext Result.failure()
        } finally {
            imageIndexer.close()
            if (isLastBatch) {
                resetMetrics(sharedPreferences)
            }
        }
    }

    override fun onProgress(processedCount: Int) {
        val count = if(processedCount >previousProcessingCount) processedCount else previousProcessingCount + processedCount
        setProgressAsync(workDataOf("processed_count" to count, "total_count" to totalImageIds))
        Log.i(TAG, "Progress: $count/$totalImageIds images processed")
    }

    private fun onLastBatch(totalProcessingTime: Long, totalProcessedCount: Int){
        val processingTimeSeconds = totalProcessingTime / 1000
        val minutes = processingTimeSeconds / 60
        val seconds = processingTimeSeconds % 60
        val notificationText = "Total images processed: $totalProcessedCount, Total processing time: ${minutes}m ${seconds}s"
        Log.i(TAG, notificationText)
        showNotification(applicationContext, "Indexing Complete", notificationText, 1002)
    }

    private fun updateMetrics(previousTotal: Int, newBatchProcessedCount: Int, batchStartTime: Long): Pair<Int, Long> {
        val previousProcessingTime = sharedPreferences.getLong(KEY_TOTAL_PROCESSING_TIME, 0L)
        val batchProcessingTime = System.currentTimeMillis() - batchStartTime
        val updatedProcessedCount = previousTotal + newBatchProcessedCount
        val updatedProcessingTime = previousProcessingTime + batchProcessingTime

        sharedPreferences.edit {
            putInt(KEY_TOTAL_PROCESSED_COUNT, updatedProcessedCount)
            putLong(KEY_TOTAL_PROCESSING_TIME, updatedProcessingTime)
        }
        return Pair(updatedProcessedCount, updatedProcessingTime)
    }

    private fun resetMetrics(sharedPreferences: SharedPreferences) {
        sharedPreferences.edit {
            putInt(KEY_TOTAL_PROCESSED_COUNT, 0)
            putLong(KEY_TOTAL_PROCESSING_TIME, 0L)
        }
    }
}
