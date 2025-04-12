package com.fpf.smartscan.workers

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri
import com.fpf.smartscan.lib.getFilesFromDir


fun persistImageUriList(context: Context, fileUris: List<Uri>): String {
    val jsonArray = JSONArray()
    fileUris.forEach { uri ->
        jsonArray.put(uri.toString())
    }
    val fileName = "classification_image_uris_${System.currentTimeMillis()}.json"
    val file = File(context.filesDir, fileName)
    file.writeText(jsonArray.toString())
    return file.absolutePath
}

/**
 * Orchestrator worker that scans directory URIs for image files,
 * persists the complete list to file, and divides the workload into batches.
 *
 * Each batch is scheduled as a ClassificationBatchWorker. The input for each batch worker
 * contains only the following items:
 * - The file path of the persisted URI list.
 * - Batch index (i.e. which batch in the sequence).
 * - Batch size (i.e. how many images to process).
 * - Total number of images (for reference, if needed).
 * - A flag (IS_LAST_BATCH) indicating if this is the final batch.
 */
class ClassificationWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = WorkerConstants.CLASSIFICATION_WORKER
        private const val BATCH_SIZE = 500
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val uriStrings = inputData.getStringArray("uris") ?: run {
                Log.e(TAG, "No URIs provided to classify.")
                return@withContext Result.failure()
            }
            val directoryUris = uriStrings.map { it.toUri() }

            val imageExtensions = listOf("jpg", "jpeg", "png", "webp")
            val fileUriList = getFilesFromDir(applicationContext, directoryUris, imageExtensions)
            if (fileUriList.isEmpty()) {
                Log.i(TAG, "No image files found for classification.")
                return@withContext Result.success()
            }
            Log.i(TAG, "Found ${fileUriList.size} image files for classification.")

            val imageUriFilePath = persistImageUriList(applicationContext, fileUriList)

            val totalImages = fileUriList.size
            val totalBatches = (totalImages + BATCH_SIZE - 1) / BATCH_SIZE
            Log.i(TAG, "Will schedule $totalBatches batch workers (batch size: $BATCH_SIZE).")

            val workManager = WorkManager.getInstance(applicationContext)
            var continuation: WorkContinuation? = null

            // Chain ClassificationBatchWorker requests sequentially.
            for (batchIndex in 0 until totalBatches) {
                val isLastBatch = batchIndex == totalBatches - 1

                val workData = workDataOf(
                    "IMAGE_URI_FILE" to imageUriFilePath,
                    "BATCH_INDEX" to batchIndex,
                    "BATCH_SIZE" to BATCH_SIZE,
                    "TOTAL_IMAGES" to totalImages,
                    "IS_LAST_BATCH" to isLastBatch
                )

                val batchWorkerRequest = OneTimeWorkRequestBuilder<ClassificationBatchWorker>()
                    .setInputData(workData)
                    .addTag(WorkerConstants.CLASSIFICATION_BATCH_WORKER)
                    .build()

                continuation = continuation?.then(batchWorkerRequest)
                    ?: workManager.beginWith(batchWorkerRequest)

                Log.i(TAG, "Chained ClassificationBatchWorker for batch $batchIndex")
            }

            continuation?.enqueue()

            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during classification orchestration: ${e.message}", e)
            return@withContext Result.failure()
        }
    }
}

fun scheduleClassificationWorker(
    context: Context,
    uris: Array<Uri?>,
    frequency: String,
    delayInMinutes: Long? = null
) {
    val duration = when (frequency) {
        "1 Day" -> 1L to TimeUnit.DAYS
        "1 Week" -> 7L to TimeUnit.DAYS
        else -> {
            Log.e("scheduleClassificationWorker", "Invalid frequency: $frequency, defaulting to 1 Day")
            1L to TimeUnit.DAYS
        }
    }

    val uriStrings = uris.mapNotNull { it?.toString() }.toTypedArray()
    val inputData = Data.Builder()
        .putStringArray("uris", uriStrings as Array<String?>)
        .build()

    val workRequestBuilder =
        PeriodicWorkRequestBuilder<ClassificationWorker>(duration.first, duration.second)
            .setInputData(inputData)


    if (delayInMinutes != null && delayInMinutes > 0) {
        workRequestBuilder.setInitialDelay(delayInMinutes, TimeUnit.MINUTES)
    }

    val workRequest = workRequestBuilder.build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WorkerConstants.CLASSIFICATION_WORKER,
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}
