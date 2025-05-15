package com.fpf.smartscan.workers

import android.app.Application
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
import com.fpf.smartscan.data.prototypes.PrototypeEmbedding
import com.fpf.smartscan.data.prototypes.PrototypeEmbeddingDatabase
import com.fpf.smartscan.data.prototypes.PrototypeEmbeddingRepository
import com.fpf.smartscan.lib.JobManager
import com.fpf.smartscan.lib.deleteLocalFile
import com.fpf.smartscan.lib.getFilesFromDir
import com.fpf.smartscan.lib.readUriListFromFile

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
        private const val PREF_KEY_LAST_USED_CLASSIFICATION_DIRS = "last_used_destinations"
        private const val IMAGE_URI_FILE_PREFIX = "classification_image_uris"
    }

    private val prototypeRepository: PrototypeEmbeddingRepository =
        PrototypeEmbeddingRepository(
            PrototypeEmbeddingDatabase.getDatabase(context.applicationContext as Application)
                .prototypeEmbeddingDao()
        )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val uriStrings = inputData.getStringArray("uris") ?: run {
                Log.e(TAG, "No URIs provided to classify.")
                return@withContext Result.failure()
            }

            val targetDirectories = uriStrings.map { it.toUri() }
            val imageExtensions = listOf("jpg", "jpeg", "png", "webp")
            val fileUriList = getFilesFromDir(applicationContext, targetDirectories, imageExtensions)
            val prototypeList: List<PrototypeEmbedding> = prototypeRepository.getAllEmbeddingsSync()
            val currentDestinationDirectories = prototypeList.map { it.id }
            val  filteredFileUriList = getFilteredUriList(applicationContext, fileUriList, currentDestinationDirectories)
            if (filteredFileUriList.isEmpty()) {
                Log.i(TAG, "No image files found for classification.")
                return@withContext Result.success()
            }

            Log.i(TAG, "Found ${filteredFileUriList.size} image files for classification.")

            val imageUriFilePath = persistImageUriList(applicationContext, filteredFileUriList)
            val totalImages = filteredFileUriList.size
            val totalBatches = (totalImages + BATCH_SIZE - 1) / BATCH_SIZE

            Log.i(TAG, "Will schedule $totalBatches batch workers (batch size: $BATCH_SIZE).")

            val workManager = WorkManager.getInstance(applicationContext)
            var continuation: WorkContinuation? = null

            // This prevents stale data between chained workers
            val jobManager = JobManager.getInstance(applicationContext)
            jobManager.clearJobs(WorkerConstants.JOB_NAME_CLASSIFICATION)


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
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
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

    private fun persistImageUriList(context: Context, fileUris: List<Uri>): String {
        val jsonArray = JSONArray()
        fileUris.forEach { uri ->
            jsonArray.put(uri.toString())
        }
        val fileName = "${IMAGE_URI_FILE_PREFIX}_${System.currentTimeMillis()}.json"
        val file = File(context.filesDir, fileName)
        file.writeText(jsonArray.toString())
        return file.absolutePath
    }

    private fun retrieveWorkFiles(context: Context): List<String> {
        val filesDir = context.filesDir
        val targetFiles = filesDir.listFiles { file ->
            file.isFile && file.name.contains(IMAGE_URI_FILE_PREFIX)
        }?.toList() ?: emptyList()

        return targetFiles
            .sortedByDescending { it.lastModified() }
            .map { it.absolutePath }
    }


    private fun getLastUsedDestinations(context: Context): List<String> {
        val prefs = context.getSharedPreferences(
            WorkerConstants.JOB_NAME_CLASSIFICATION,
            Context.MODE_PRIVATE
        )
        val uriSet =
            prefs.getStringSet(PREF_KEY_LAST_USED_CLASSIFICATION_DIRS, emptySet()) ?: emptySet()
        return uriSet.toList()
    }

    // must be called before new file path generated
    private fun getFilteredUriList(context: Context, currentFileUriList: List<Uri>, currentDestinationDirectories: List<String>): List<Uri> {
        return try {
            val workFiles = retrieveWorkFiles(context)
            if (workFiles.isNotEmpty()) {
                val previousWorkPath = workFiles[0]
                val previousUriList = readUriListFromFile(previousWorkPath)
                val lastUsedDestinationsDirectories = getLastUsedDestinations(context)
                if (lastUsedDestinationsDirectories.isEmpty()) return currentFileUriList
                val isSameDestinations = currentDestinationDirectories.toSet() == lastUsedDestinationsDirectories.toSet()
                workFiles.map{deleteLocalFile(context, it)} // cleanup old files
                if (isSameDestinations) {
                    return currentFileUriList.filterNot { it in previousUriList.toSet() }
                }
            }
            currentFileUriList
        } catch (e: Exception) {
            currentFileUriList
        }
    }
}


fun scheduleClassificationWorker(
    context: Context,
    uris: Array<Uri?>,
    frequency: String,
    delayInHours: Long? = null
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

    val constraints = Constraints.Builder()
        .setRequiresBatteryNotLow(true)
        .build()

    val workRequestBuilder =
        PeriodicWorkRequestBuilder<ClassificationWorker>(duration.first, duration.second)
            .setInputData(inputData)
            .setConstraints(constraints)



    if (delayInHours != null && delayInHours > 0) {
        workRequestBuilder.setInitialDelay(delayInHours, TimeUnit.HOURS)
    }

    val workRequest = workRequestBuilder.build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WorkerConstants.CLASSIFICATION_WORKER,
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}
