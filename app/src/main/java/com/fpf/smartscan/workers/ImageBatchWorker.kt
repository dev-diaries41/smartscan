package com.fpf.smartscan.workers

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.fpf.smartscan.lib.processors.ImageIndexListener
import com.fpf.smartscan.lib.processors.ImageIndexer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageBatchWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams), ImageIndexListener {

    private val jobManager = JobManager.getInstance(context)
    private var startTime: Long = 0L
    private var previousProcessingCount: Int = 0
    private var lastPercentage: Int = 0

    private val batchIds = inputData.getLongArray("BATCH_IMAGE_IDS")?.toList() ?: emptyList()
    private val isLastBatch = inputData.getBoolean("IS_LAST_BATCH", false)
    private val totalImageIds = inputData.getInt("TOTAL_IMAGES_IDS", 0)

    companion object {
        private const val TAG = "ImageBatchWorker"
        private const val JOB_NAME = "index"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val startResult = jobManager.onStart(JOB_NAME)
        startTime = startResult.startTime
        previousProcessingCount = startResult.initialProcessedCount

        if (batchIds.isEmpty()) {
            Log.i(TAG, "No image IDs provided for this batch.")
            return@withContext Result.success()
        }

        val imageIndexer = ImageIndexer(applicationContext as Application, this@ImageBatchWorker)

        try {
            Log.i(TAG, "Processing batch of ${batchIds.size} images.")

            val processedCount = imageIndexer.indexImages(batchIds)

            val finishTime = System.currentTimeMillis()

            jobManager.onComplete(
                jobName = JOB_NAME,
                startTime = startTime,
                finishTime = finishTime,
                processedCount = processedCount
            )

            Result.success()

        } catch (e: Exception) {
            val failTime = System.currentTimeMillis()
            Log.e(TAG, "Error processing batch: ${e.message}", e)

            jobManager.onError(
                jobName = JOB_NAME,
                startTime = startTime,
                finishTime = failTime,
                processedCount = 0
            )

            Result.failure()
        } finally {
            imageIndexer.close()
            if(isLastBatch){
                jobManager.notifyAllJobsComplete(applicationContext, JOB_NAME)
                jobManager.clearJobs(JOB_NAME)
            }
        }
    }

    override fun onProgress(processedCount: Int) {
        val count = previousProcessingCount + processedCount
        val currentPercentage = ((count.toDouble() / totalImageIds) * 100).toInt()

        if (currentPercentage > lastPercentage) {
            lastPercentage = currentPercentage
            setProgressAsync(workDataOf("processed_count" to count, "total_count" to totalImageIds))
//            Log.i(TAG, "Progress: $count/$totalImageIds images processed ($currentPercentage%)")
        }
    }
}
