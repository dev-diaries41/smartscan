package com.fpf.smartscan.workers

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fpf.smartscan.R
import com.fpf.smartscan.data.scans.AppDatabase
import com.fpf.smartscan.data.scans.ScanData
import com.fpf.smartscan.data.scans.ScanDataRepository
import com.fpf.smartscan.lib.JobManager
import com.fpf.smartscan.lib.getTimeInMinutesAndSeconds
import com.fpf.smartscan.lib.processors.Organiser
import com.fpf.smartscan.lib.readUriListFromFile
import com.fpf.smartscan.lib.showNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * Batch worker that reads a persisted file containing all image URIs,
 * extracts a batch based on input parameters, processes each image for classification,
 * and reports progress via JobManager.
 */
class ClassificationBatchWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "ClassificationBatchWorker"
        private const val JOB_NAME = ClassificationWorker.JOB_NAME
    }

    private val jobManager = JobManager.getInstance(context)
    private var startTime: Long = 0L
    private var previousProcessingCount: Int = 0

    private val scanId = inputData.getInt("SCAN_ID", -1)
    private val batchIndex = inputData.getInt("BATCH_INDEX", -1)
    private val batchSize = inputData.getInt("BATCH_SIZE", -1)
    private val totalImages = inputData.getInt("TOTAL_IMAGES", -1)
    private val isLastBatch = inputData.getBoolean("IS_LAST_BATCH", false)
    private val imageUriFilePath = inputData.getString("IMAGE_URI_FILE") ?: ""
    private val repository = ScanDataRepository(AppDatabase.getDatabase(applicationContext as Application).scanDataDao())

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val organiser = Organiser(applicationContext)
        val startResult = jobManager.onStart(JOB_NAME)
        startTime = startResult.startTime
        previousProcessingCount = startResult.initialProcessedCount

        try {
            if (batchIndex < 0 || batchSize <= 0 || totalImages <= 0 || scanId == -1) {
                throw IllegalArgumentException("Invalid batch parameters: BATCH_INDEX=$batchIndex, BATCH_SIZE=$batchSize, TOTAL_IMAGES=$totalImages, SCAN_ID=$scanId")
            }
            if (imageUriFilePath.isEmpty()) {
                throw IllegalArgumentException("IMAGE_URI_FILE not provided")
            }

            val uriList = readUriListFromFile(imageUriFilePath)
            val startIndex = batchIndex * batchSize
            val endIndex = kotlin.math.min(startIndex + batchSize, uriList.size)
            val batchUriList = uriList.subList(startIndex, endIndex)

            Log.i(TAG, "Processing classification batch $batchIndex with ${batchUriList.size} images.")
            val processedCount = organiser.processBatch(batchUriList, scanId)
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
            val results = jobManager.getJobResults(JOB_NAME)
            if(results.errorCount >= 3){
                // Temporary workaround to avoid modifying db schema:
                // If totalProcessedCount > 0 it indicates some batches were successful.
                // ERROR_RESULT (-1) is used to indicate a failure with no reliable result.
                val count = if(results.totalProcessedCount > 0) results.totalProcessedCount else ScanData.ERROR_RESULT
                repository.insert(ScanData(result=count, date = System.currentTimeMillis()))
                // Do not continually retry unlike indexing, because this does not have a super critical impact on usability
                showNotification(applicationContext, applicationContext.getString(R.string.notif_title_smart_scan_issue), applicationContext.getString(R.string.notif_title_smart_scan_issue_description), 1003)
                return@withContext Result.failure()
            }

            return@withContext Result.retry()
        } finally {
            organiser.close()
            if (isLastBatch) {
                onAllJobsComplete()
                jobManager.clearJobs(JOB_NAME)
            }
        }
    }

    private suspend fun onAllJobsComplete(){
        val results = jobManager.getJobResults(JOB_NAME)
        if (results.totalProcessedCount == 0) return

        try {
            repository.insert(ScanData(result=results.totalProcessedCount, date = System.currentTimeMillis()))

            val totalProcessingTime = results.finishTime - results.startTime
            val (minutes, seconds) = getTimeInMinutesAndSeconds(totalProcessingTime)
            val notificationText = "Total images moved: ${results.totalProcessedCount}, Time: ${minutes}m ${seconds}s"

            showNotification(applicationContext, applicationContext.getString(R.string.notif_title_smart_scan_complete), notificationText, 1003)
        }
        catch (e: Exception){
            Log.e(TAG, "Error finalising $JOB_NAME jobs: ${e.message}", e)
        }
    }
}
