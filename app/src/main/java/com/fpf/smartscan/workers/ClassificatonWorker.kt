package com.fpf.smartscan.workers

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import androidx.work.*
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri
import com.fpf.smartscan.data.prototypes.PrototypeEmbedding
import com.fpf.smartscan.data.prototypes.PrototypeEmbeddingDatabase
import com.fpf.smartscan.data.prototypes.PrototypeEmbeddingRepository
import com.fpf.smartscan.data.scans.ScanData
import com.fpf.smartscan.data.scans.ScanDataRepository
import com.fpf.smartscan.data.scans.AppDatabase
import com.fpf.smartscan.lib.showNotification
import com.fpf.smartscan.lib.clip.Embeddings
import com.fpf.smartscan.lib.clip.getSimilarities
import com.fpf.smartscan.lib.clip.getTopN
import com.fpf.smartscan.lib.getBitmapFromUri
import com.fpf.smartscan.lib.moveFile

class ClassificationWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    private val repository = ScanDataRepository(AppDatabase.getDatabase(applicationContext as Application).scanDataDao())
    private  val prototypeRepository = PrototypeEmbeddingRepository(PrototypeEmbeddingDatabase.getDatabase(applicationContext as Application).prototypeEmbeddingDao())

    override suspend fun doWork(): Result {
        val prototypeList = prototypeRepository.getAllEmbeddingsSync()
        val uriStrings = inputData.getStringArray("uris") ?: return Result.failure()
        val uris = uriStrings.map { it.toUri() }
        val sharedPreferences = applicationContext.getSharedPreferences(
            "ClassificationWorkerPrefs",
            Context.MODE_PRIVATE
        )
        val lastProcessedTime = sharedPreferences.getLong("last_processed_time", 0L)
        val imageExtensions = listOf("jpg", "jpeg", "png", "webp")
        var maxProcessedTime = lastProcessedTime
        val embeddingHandler = Embeddings(applicationContext.resources)
        val tag = "ClassificationWorker"
        var processedFileCount = 0

        try {
            for (uri in uris) {
                val documentDir = DocumentFile.fromTreeUri(applicationContext, uri)
                if (documentDir != null && documentDir.isDirectory) {
                    documentDir.listFiles().forEach { documentFile ->
                        if (documentFile.isFile) {
                            val fileName = documentFile.name ?: ""

                            if (imageExtensions.any { fileName.endsWith(".$it", ignoreCase = true) }) {
                                val fileModifiedTime = getLastModifiedTime(documentFile.uri)
                                if (fileModifiedTime > lastProcessedTime && prototypeList.isNotEmpty()) {
                                    val processed = processNewImage(applicationContext, documentFile.uri,
                                        prototypeList, embeddingHandler)
                                    if (processed) {
                                        processedFileCount++
                                    }
                                    if (fileModifiedTime > maxProcessedTime) {
                                        maxProcessedTime = fileModifiedTime
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Log.e(tag, "Invalid directory URI: $uri")
                }
            }

            if (maxProcessedTime > lastProcessedTime) {
                sharedPreferences.edit { putLong("last_processed_time", maxProcessedTime) }
                Log.i(tag, "Updated last process time: $maxProcessedTime")
            }

            if(processedFileCount > 0){
                insertScanData(repository, processedFileCount)
                showNotification(applicationContext, "Smart Scan Complete", "$processedFileCount images processed.")
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e(tag, "Error during work: ${e.message}", e)
            return Result.failure()
        } finally {
            embeddingHandler.closeSession()
        }
    }

    private suspend fun processNewImage(context: Context, uri: Uri, prototypeEmbeddings: List<PrototypeEmbedding>, embeddingsHandler: Embeddings
    ): Boolean {
        val tag = "ClassificationError"
        val bitmap = getBitmapFromUri(applicationContext, uri)
        val imageEmbedding = embeddingsHandler.generateImageEmbedding(bitmap)
        val similarities = getSimilarities(imageEmbedding, prototypeEmbeddings.map { it.embeddings })
        val bestIndex = getTopN(similarities, 1, 0.2f).firstOrNull() ?: -1
        val destinationUri = prototypeEmbeddings.getOrNull(bestIndex)?.id

        if (destinationUri == null) {
            Log.e(tag, "Image classification failed.")
            return false
        }
        return moveFile(context, uri, destinationUri.toUri())
    }



    private suspend fun insertScanData(repository: ScanDataRepository, processedImages: Int){
        val tag = "DataInsertionError"
        try {
            repository.insert(
                ScanData(result=processedImages, date = System.currentTimeMillis())
            )
        }
        catch (e: Exception){
            Log.e(tag, "Error inserting scan data: ${e.message}", e)
        }
    }

    /**
     * Queries the content resolver to get the last modified timestamp for the given Uri.
     * Note: Not every Uri supports this query.
     */
    private fun getLastModifiedTime(uri: Uri): Long {
        var lastModified = 0L
        val projection = arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
        val cursor = applicationContext.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                lastModified = it.getLong(0)
            }
        }
        return lastModified
    }
}


fun scheduleClassificationWorker(context: Context, uris: Array<Uri?>,  frequency: String) {
    val duration = when (frequency) {
        "1 Day" -> 1L to TimeUnit.DAYS
        "1 Week" -> 7L to TimeUnit.DAYS
        else -> {
            Log.e("ClassificationWorker", "Invalid frequency: $frequency, defaulting to 1 Day")
            1L to TimeUnit.DAYS
        }
    }

    val uriStrings = uris.mapNotNull { it?.toString() }.toTypedArray()
    val inputData = Data.Builder()
        .putStringArray("uris", uriStrings as Array<String?>)
        .build()

    val workRequest = PeriodicWorkRequestBuilder<ClassificationWorker>(duration.first, duration.second)
        .setInputData(inputData)
        .setConstraints(
            Constraints(
                requiresBatteryNotLow = true,
            )
        )
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "ClassificationWorker",
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}
