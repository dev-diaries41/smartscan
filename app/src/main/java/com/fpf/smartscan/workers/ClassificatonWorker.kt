package com.fpf.smartscan.workers

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
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
import com.fpf.smartscan.lib.clip.ModelType
import com.fpf.smartscan.lib.clip.getSimilarities
import com.fpf.smartscan.lib.clip.getTopN
import com.fpf.smartscan.lib.getBitmapFromUri
import com.fpf.smartscan.lib.moveFile
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class ClassificationWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    private val repository = ScanDataRepository(AppDatabase.getDatabase(applicationContext as Application).scanDataDao())
    private  val prototypeRepository = PrototypeEmbeddingRepository(PrototypeEmbeddingDatabase.getDatabase(applicationContext as Application).prototypeEmbeddingDao())

    override suspend fun doWork(): Result {
        val tag = "ClassificationWorker"
        val prototypeList = prototypeRepository.getAllEmbeddingsSync()
        if (prototypeList.isEmpty()) {
            Log.e(tag, "No prototype embeddings available.")
            return Result.failure()
        }
        val uriStrings = inputData.getStringArray("uris") ?: return Result.failure()
        val uris = uriStrings.map { it.toUri() }
        val imageExtensions = listOf("jpg", "jpeg", "png", "webp")
        val embeddingHandler = Embeddings(applicationContext.resources, ModelType.IMAGE)

        try {
            val imageFileUris = mutableListOf<Uri>()
            for (uri in uris) {
                val documentDir = DocumentFile.fromTreeUri(applicationContext, uri)
                if (documentDir != null && documentDir.isDirectory) {
                    documentDir.listFiles().forEach { documentFile ->
                        if (documentFile.isFile) {
                            val fileName = documentFile.name ?: ""
                            if (imageExtensions.any { fileName.endsWith(".$it", ignoreCase = true) }) {
                                imageFileUris.add(documentFile.uri)
                            }
                        }
                    }
                } else {
                    Log.e(tag, "Invalid directory URI: $uri")
                }
            }

            val semaphore = Semaphore(3)
            val processedResults = coroutineScope {
                imageFileUris.map { fileUri ->
                    async {
                        semaphore.withPermit {
                            processNewImage(applicationContext, fileUri, prototypeList, embeddingHandler)
                        }
                    }
                }.awaitAll()
            }

            val processedFileCount = processedResults.count { it }

            if (processedFileCount > 0) {
                insertScanData(repository, processedFileCount)
                showNotification(applicationContext, "Smart Scan Complete", "$processedFileCount images processed."
                )
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
}


fun scheduleClassificationWorker(
    context: Context,
    uris: Array<Uri?>,
    frequency: String,
    delayInMinutes: Long? = null
) {
    val workerName = "ClassificationWorker"
    val duration = when (frequency) {
        "1 Day" -> 1L to TimeUnit.DAYS
        "1 Week" -> 7L to TimeUnit.DAYS
        else -> {
            Log.e("ClassificationWorkerScheduleError", "Invalid frequency: $frequency, defaulting to 1 Day")
            1L to TimeUnit.DAYS
        }
    }

    val uriStrings = uris.mapNotNull { it?.toString() }.toTypedArray()
    val inputData = Data.Builder()
        .putStringArray("uris", uriStrings as Array<String?>)
        .build()

    // Create the PeriodicWorkRequestBuilder
    val workRequestBuilder = PeriodicWorkRequestBuilder<ClassificationWorker>(duration.first, duration.second)
        .setInputData(inputData)
        .setConstraints(
            Constraints(
                requiresBatteryNotLow = true,
            )
        )

    if (delayInMinutes != null && delayInMinutes > 0) {
        workRequestBuilder.setInitialDelay(delayInMinutes, TimeUnit.MINUTES)
    }

    val workRequest = workRequestBuilder.build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        workerName,
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}
