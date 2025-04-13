package com.fpf.smartscan.workers

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fpf.smartscan.R
import com.fpf.smartscan.data.scans.AppDatabase
import com.fpf.smartscan.data.scans.ScanData
import com.fpf.smartscan.data.scans.ScanDataRepository
import com.fpf.smartscan.lib.JobManager
import com.fpf.smartscan.lib.deleteLocalFile
import com.fpf.smartscan.lib.getTimeInMinutesAndSeconds
import com.fpf.smartscan.lib.processors.Organiser
import com.fpf.smartscan.lib.showNotification
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
        private const val TAG = WorkerConstants.CLASSIFICATION_BATCH_WORKER
        private const val JOB_NAME = WorkerConstants.JOB_NAME_CLASSIFICATION
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

            return@withContext Result.retry()
        } finally {
            organiser.close()
            if (isLastBatch) {
                onAllJobsComplete()
                deleteLocalFile(applicationContext, imageUriFilePath)
                jobManager.clearJobs(JOB_NAME)
            }
        }
    }

    private suspend fun onAllJobsComplete(){
        val (totalProcessedCount, timePair) = jobManager.getJobResults(JOB_NAME)
        if (totalProcessedCount == 0) return

        try {
            val repository = ScanDataRepository(AppDatabase.getDatabase(applicationContext as Application).scanDataDao())
            repository.insert(ScanData(result=totalProcessedCount, date = System.currentTimeMillis()))

            val totalProcessingTime = timePair.second - timePair.first
            val (minutes, seconds) = getTimeInMinutesAndSeconds(totalProcessingTime)
            val notificationText = "Total images moved: $totalProcessedCount, Time: ${minutes}m ${seconds}s"

            showNotification(applicationContext, applicationContext.getString(R.string.notif_title_smart_scan_complete), notificationText, 1003)
        }
        catch (e: Exception){
            Log.e(TAG, "Error finalising $JOB_NAME jobs: ${e.message}", e)
        }
    }
}
