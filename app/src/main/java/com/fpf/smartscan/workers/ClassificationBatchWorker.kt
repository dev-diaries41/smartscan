package com.fpf.smartscan.workers

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fpf.smartscan.data.scans.ScanData
import com.fpf.smartscan.data.scans.ScanDataRepository
import com.fpf.smartscan.data.scans.AppDatabase
import com.fpf.smartscan.lib.showNotification
import com.fpf.smartscan.lib.clip.Embeddings
import com.fpf.smartscan.lib.clip.ModelType
import com.fpf.smartscan.lib.deleteLocalFile
import com.fpf.smartscan.lib.processors.Organiser
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

/**
 * Batch worker that reads a persisted file containing all image URIs,
 * extracts a batch based on input parameters, processes each image for classification,
 * and updates progress information. On the final batch, a notification is shown.
 */
class ClassificationBatchWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ClassificationBatchWorker"
        private const val PREFS_NAME = "ClassificationIndexPrefs"
        private const val KEY_TOTAL_PROCESSED_COUNT = "totalProcessedCount"
        private const val KEY_TOTAL_PROCESSING_TIME = "totalProcessingTime"
    }

    val sharedPreferences = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)!!
    private val scanRepository = ScanDataRepository(AppDatabase.getDatabase(applicationContext as Application).scanDataDao())
    val batchIndex = inputData.getInt("BATCH_INDEX", -1)
    val batchSize = inputData.getInt("BATCH_SIZE", -1)
    val totalImages = inputData.getInt("TOTAL_IMAGES", -1)
    val isLastBatch = inputData.getBoolean("IS_LAST_BATCH", false)

    override suspend fun doWork(): Result = withContext(kotlinx.coroutines.Dispatchers.IO) {
        val batchStartTime = System.currentTimeMillis()
        val organiser = Organiser(applicationContext)
        val imageUriFilePath = inputData.getString("IMAGE_URI_FILE") ?: run {
            Log.e(TAG, "IMAGE_URI_FILE not provided")
            return@withContext Result.failure()
        }

        try {
            if (batchIndex < 0 || batchSize <= 0 || totalImages <= 0) {
                Log.e(TAG, "Invalid batch parameters provided")
                return@withContext Result.failure()
            }

            val file = File(imageUriFilePath)
            if (!file.exists()) {
                Log.e(TAG, "Persisted image URI file not found: $imageUriFilePath")
                return@withContext Result.failure()
            }
            val jsonArray = JSONArray(file.readText())
            if (jsonArray.length() != totalImages) {
                Log.w(TAG, "Mismatch between expected totalImages ($totalImages) and file contents (${jsonArray.length()})")
            }

            // Calculate start and end indices.
            val startIndex = batchIndex * batchSize
            val endIndex = kotlin.math.min(startIndex + batchSize, totalImages)
            if (startIndex >= endIndex) {
                if (isLastBatch) {
                resetMetrics(sharedPreferences)
                }
                Log.i(TAG, "No images to process in this batch ($batchIndex).")
                return@withContext Result.success()
            }

            // Build the list of image URIs for this batch.
            val batchUriList = mutableListOf<Uri>()
            for (i in startIndex until endIndex) {
                val uriString = jsonArray.optString(i, null)
                if (uriString != null) {
                    batchUriList.add(uriString.toUri())
                }
            }

            val processedCount = organiser.processBatch(batchUriList)
            Log.i(TAG, "Processed $processedCount images in batch $batchIndex.")

            val (updatedTotal, updatedProcessingTime) = updateMetrics(sharedPreferences, processedCount, batchStartTime)

            if (processedCount > 0) {
                insertScanData(scanRepository, processedCount)
            }

            if (isLastBatch) {
                onLastBatch(updatedProcessingTime, updatedTotal )
            }

            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing batch: ${e.message}", e)
            return@withContext Result.failure()
        } finally {
            organiser.close()
            if(isLastBatch){
                deleteLocalFile(applicationContext, imageUriFilePath)
                resetMetrics(sharedPreferences)
            }
        }
    }

    private suspend fun insertScanData(repository: ScanDataRepository, processedImages: Int) {
        try {
            repository.insert(
                ScanData(result = processedImages, date = System.currentTimeMillis())
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting scan data: ${e.message}", e)
        }
    }

    private fun onLastBatch(totalProcessingTime: Long, totalProcessedCount: Int){
        val processingTimeSeconds = totalProcessingTime / 1000
        val minutes = processingTimeSeconds / 60
        val seconds = processingTimeSeconds % 60
        val notificationText = "Total images processed: $totalProcessedCount, Total processing time: ${minutes}m ${seconds}s"
        Log.i(TAG, notificationText)
        showNotification(applicationContext, "Smart Scan Complete", notificationText, 1002)
    }

    private fun updateMetrics(preferences: SharedPreferences, processedCount: Int, batchStartTime: Long): Pair<Int, Long> {
        val batchProcessingTime = System.currentTimeMillis() - batchStartTime
        val previousProcessingTime = preferences.getLong(KEY_TOTAL_PROCESSING_TIME, 0L)
        val previousTotal = preferences.getInt(KEY_TOTAL_PROCESSED_COUNT, 0)

        val updatedProcessedCount = previousTotal + processedCount
        val updatedProcessingTime = previousProcessingTime + batchProcessingTime

        preferences.edit {
            putLong(KEY_TOTAL_PROCESSING_TIME, updatedProcessingTime)
            putInt(KEY_TOTAL_PROCESSED_COUNT, updatedProcessedCount)
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

