
package com.fpf.smartscan.workers

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.work.*
import com.fpf.smartscan.lib.JobManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ImageIndexWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = WorkerConstants.IMAGE_INDEXER_WORKER
        private const val BATCH_SIZE = 500
        private const val JOB_NAME = WorkerConstants.JOB_NAME_INDEX
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val allImageIds = queryAllImageIds()
            if (allImageIds.isEmpty()) {
                Log.i(TAG, "No images found to index.")
                return@withContext Result.success()
            }

            // This prevents stale data between chained workers
            val jobManager = JobManager.getInstance(applicationContext)
            jobManager.clearJobs(JOB_NAME)

            // Calculate total number of batches.
            val totalBatches = (allImageIds.size + BATCH_SIZE - 1) / BATCH_SIZE
            Log.i(TAG, "Found ${allImageIds.size} images; scheduling $totalBatches batch workers sequentially.")

            // Chain one-time batch workers so they run one after the other.
            val workManager = WorkManager.getInstance(applicationContext)
            var continuation: WorkContinuation? = null

            for (batchIndex in 0 until totalBatches) {
                val startIndex = batchIndex * BATCH_SIZE
                val endIndex = minOf(startIndex + BATCH_SIZE, allImageIds.size)
                val batchIds = allImageIds.subList(startIndex, endIndex).toLongArray()
                val isLastBatch = (batchIndex == totalBatches - 1)
                val workData = workDataOf(
                    "BATCH_IMAGE_IDS" to batchIds,
                    "IS_LAST_BATCH" to isLastBatch,
                    "TOTAL_IMAGES_IDS" to allImageIds.size
                )

                val batchWorkerRequest = OneTimeWorkRequestBuilder<ImageBatchWorker>()
                    .setInputData(workData)
                    .addTag(WorkerConstants.IMAGE_INDEXER_BATCH_WORKER)
                    .build()

                continuation = continuation?.then(batchWorkerRequest) ?: workManager.beginWith(batchWorkerRequest)
                Log.i(TAG, "Chained batch worker for batch $batchIndex, processing ${batchIds.size} images.")
            }
            // Enqueue the entire chain.
            continuation?.enqueue()

            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in orchestrator: ${e.message}", e)
            return@withContext Result.failure()
        }
    }


    private fun queryAllImageIds(): List<Long> {
        val imageIds = mutableListOf<Long>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        applicationContext.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                imageIds.add(cursor.getLong(idColumn))
            }
        }
        return imageIds
    }
}


fun scheduleImageIndexWorker(context: Context, frequency: String) {
    val duration = when (frequency) {
        "1 Day" -> 1L to TimeUnit.DAYS
        "1 Week" -> 7L to TimeUnit.DAYS
        else -> {
            Log.e("scheduleImageIndexWorker", "Invalid frequency: $frequency, defaulting to 1 Week")
            7L to TimeUnit.DAYS
        }
    }

    val workRequest = PeriodicWorkRequestBuilder<ImageIndexWorker>(duration.first, duration.second)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WorkerConstants.IMAGE_INDEXER_WORKER,
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}
