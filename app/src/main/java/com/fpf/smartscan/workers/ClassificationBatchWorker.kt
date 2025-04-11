package com.fpf.smartscan.workers

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fpf.smartscan.lib.deleteLocalFile
import com.fpf.smartscan.lib.processors.Organiser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

/**
 * Batch worker that reads a persisted file containing all image URIs,
 * extracts a batch based on input parameters, processes each image for classification,
 * and reports progress via JobManager.
 */
class ClassificationBatchWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ClassificationBatchWorker"
        private const val JOB_NAME = "classification"
    }

    private val jobManager = JobManager.getInstance(context)
    private var startTime: Long = 0L
    private var previousProcessingCount: Int = 0

    private val batchIndex = inputData.getInt("BATCH_INDEX", -1)
    private val batchSize = inputData.getInt("BATCH_SIZE", -1)
    private val totalImages = inputData.getInt("TOTAL_IMAGES", -1)
    private val isLastBatch = inputData.getBoolean("IS_LAST_BATCH", false)
    private val imageUriFilePath = inputData.getString("IMAGE_URI_FILE") ?: ""

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val startResult = jobManager.onStart(JOB_NAME)
        startTime = startResult.startTime
        previousProcessingCount = startResult.initialProcessedCount

        if (batchIndex < 0 || batchSize <= 0 || totalImages <= 0) {
            Log.e(TAG, "Invalid batch parameters provided")
            return@withContext Result.failure()
        }
        if (imageUriFilePath.isEmpty()) {
            Log.e(TAG, "IMAGE_URI_FILE not provided")
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

        // Calculate start and end indices for the batch.
        val startIndex = batchIndex * batchSize
        val endIndex = kotlin.math.min(startIndex + batchSize, totalImages)
        if (startIndex >= endIndex) {
            Log.i(TAG, "No images to process in this batch ($batchIndex).")
            if (isLastBatch) {
                jobManager.clearJobs(JOB_NAME)
                jobManager.notifyAllJobsComplete(applicationContext, JOB_NAME)
            }
            return@withContext Result.success()
        }

        // Build the list of image URIs for this batch.
        val batchUriList = mutableListOf<Uri>()
        for (i in startIndex until endIndex) {
            jsonArray.optString(i, null)?.let { uriString ->
                batchUriList.add(uriString.toUri())
            }
        }

        val organiser = Organiser(applicationContext)

        try {
            Log.i(TAG, "Processing classification batch $batchIndex with ${batchUriList.size} images.")
            val processedCount = organiser.processBatch(batchUriList)

            val finishTime = System.currentTimeMillis()

            jobManager.onComplete(
                jobName = JOB_NAME,
                startTime = startTime,
                finishTime = finishTime,
                processedCount = processedCount
            )

            return@withContext Result.success()
        } catch (e: Exception) {
            val failTime = System.currentTimeMillis()
            Log.e(TAG, "Error processing classification batch: ${e.message}", e)

            jobManager.onError(
                jobName = JOB_NAME,
                startTime = startTime,
                finishTime = failTime,
                processedCount = 0
            )

            return@withContext Result.failure()
        } finally {
            organiser.close()
            if (isLastBatch) {
                deleteLocalFile(applicationContext, imageUriFilePath)
                jobManager.clearJobs(JOB_NAME)
                jobManager.notifyAllJobsComplete(applicationContext, JOB_NAME)
            }
        }
    }
}
