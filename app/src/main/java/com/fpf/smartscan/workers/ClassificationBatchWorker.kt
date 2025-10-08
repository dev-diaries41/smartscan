package com.fpf.smartscan.workers

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fpf.smartscan.R
import com.fpf.smartscan.data.movehistory.MoveHistoryDatabase
import com.fpf.smartscan.data.movehistory.MoveHistoryRepository
import com.fpf.smartscan.data.prototypes.PrototypeEmbeddingDatabase
import com.fpf.smartscan.data.prototypes.PrototypeEmbeddingRepository
import com.fpf.smartscan.data.prototypes.toEmbedding
import com.fpf.smartscan.data.scans.AppDatabase
import com.fpf.smartscan.data.scans.ScanData
import com.fpf.smartscan.data.scans.ScanDataRepository
import com.fpf.smartscan.lib.OrganiserListener
import com.fpf.smartscan.lib.getTimeInMinutesAndSeconds
import com.fpf.smartscan.lib.readUriListFromFile
import com.fpf.smartscan.lib.showNotification
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipImageEmbedder
import com.fpf.smartscansdk.core.ml.models.ResourceId
import com.fpf.smartscansdk.core.processors.Metrics
import com.fpf.smartscansdk.extensions.organisers.Organiser
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
        private const val ERROR_COUNT = "ERROR_COUNT"
    }
    private val scanId = inputData.getInt("SCAN_ID", -1)
    private val batchIndex = inputData.getInt("BATCH_INDEX", -1)
    private val batchSize = inputData.getInt("BATCH_SIZE", -1)
    private val totalImages = inputData.getInt("TOTAL_IMAGES", -1)
    private val isLastBatch = inputData.getBoolean("IS_LAST_BATCH", false)
    private val imageUriFilePath = inputData.getString("IMAGE_URI_FILE") ?: ""
    private val threshold = inputData.getFloat("THRESHOLD", 0.4f)
    private val confidenceMargin = inputData.getFloat("CONFIDENCE_MARGIN", 0.03f)
    private val scanDataRepository = ScanDataRepository(AppDatabase.getDatabase(applicationContext as Application).scanDataDao())
    private val prototypeRepository = PrototypeEmbeddingRepository(PrototypeEmbeddingDatabase.getDatabase(context.applicationContext as Application).prototypeEmbeddingDao())
    private val moveHistoryRepository = MoveHistoryRepository(MoveHistoryDatabase.getDatabase(context.applicationContext as Application).moveHistoryDao())
    private val sharedPrefs = context.getSharedPreferences(JOB_NAME, Context.MODE_PRIVATE)
    private var errorCount = getErrorCount()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val embeddingHandler = ClipImageEmbedder(applicationContext.resources, ResourceId(R.raw.image_encoder_quant_int8))
        val prototypes = prototypeRepository.getAllEmbeddingsSync().map{it.toEmbedding()}
        val organiser = Organiser(applicationContext as Application, embeddingHandler, scanId=scanId, listener = OrganiserListener, prototypeList = prototypes, threshold = threshold, confidenceMargin = confidenceMargin)

        try {
            if (batchIndex < 0 || batchSize <= 0 || totalImages <= 0 || scanId == -1) {
                throw IllegalArgumentException("Invalid batch parameters: BATCH_INDEX=$batchIndex, BATCH_SIZE=$batchSize, TOTAL_IMAGES=$totalImages, SCAN_ID=$scanId")
            }
            if (imageUriFilePath.isEmpty()) {
                throw IllegalArgumentException("IMAGE_URI_FILE not provided")
            }

            embeddingHandler.initialize()

            val batchUriList = getBatchUris()

            Log.i(TAG, "Processing batch $batchIndex with ${batchUriList.size} images.")
            val results = organiser.run(batchUriList)

            when(results){
                is Metrics.Success -> {}
                // Let worker handle batch errors
                is Metrics.Failure -> {
                    throw results.error
                }
            }

            if (isLastBatch) {
                onAllJobsComplete()
            }

            resetErrorCount()
            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing batch $batchIndex (error count $errorCount): ${e.message}", e)
            incrementErrorCount()
            if(errorCount >= 3){
                resetErrorCount()
                // Temporary workaround to avoid modifying db schema:
                // ERROR_RESULT (-1) is used to indicate a failure with no reliable result.
                val moveHistory = moveHistoryRepository.getMoveHistory(scanId)
                if(moveHistory.isEmpty()){
                    scanDataRepository.delete(scanId)
                    showNotification(applicationContext, applicationContext.getString(R.string.notif_title_smart_scan_issue), applicationContext.getString(R.string.notif_title_smart_scan_issue_description), 1003)
                }else{
                    scanDataRepository.update(scanId, moveHistory.size, System.currentTimeMillis())
                }
                // Do not continually retry unlike indexing, because this does not have a super critical impact on usability
                return@withContext Result.failure()
            }
            return@withContext Result.retry()
        } finally {
            organiser.close()
            saveLastUsedDestinations(prototypes.map { it.id })
        }
    }
    private fun getBatchUris(): List<Uri>{
        val uriList = readUriListFromFile(imageUriFilePath)
        val startIndex = batchIndex * batchSize
        val endIndex = kotlin.math.min(startIndex + batchSize, uriList.size)
        val batchUriList = uriList.subList(startIndex, endIndex)
        return batchUriList
    }
    private fun saveLastUsedDestinations(files: List<String>) {
        sharedPrefs.edit { putStringSet(Organiser.PREF_KEY_LAST_USED_CLASSIFICATION_DIRS, files.toSet()) }
    }
    private fun resetErrorCount() {
        errorCount = 0
        sharedPrefs.edit { putInt(ERROR_COUNT, errorCount) }
    }
    private fun incrementErrorCount() {
        ++errorCount
        sharedPrefs.edit { putInt(ERROR_COUNT, errorCount) }
    }
    private fun getErrorCount(): Int {
        return sharedPrefs.getInt(ERROR_COUNT, 0)
    }
    private suspend fun onAllJobsComplete(){
        try {
            val moveHistory = moveHistoryRepository.getMoveHistory(scanId)
            if (moveHistory.isEmpty()){
                scanDataRepository.delete(scanId)
                return
            }

            val startTime = moveHistory.minOf { it.date }
            val finishTime = moveHistory.maxOf { it.date }

            scanDataRepository.update(scanId, moveHistory.size, System.currentTimeMillis())
            val totalProcessingTime = finishTime - startTime
            val (minutes, seconds) = getTimeInMinutesAndSeconds(totalProcessingTime)
            val notificationText = "Total images moved: ${moveHistory.size}, Time: ${minutes}m ${seconds}s"

            showNotification(applicationContext, applicationContext.getString(R.string.notif_title_auto_organisation_complete), notificationText, 1003)
        }
        catch (e: Exception){
            Log.e(TAG, "Error finalising $JOB_NAME jobs: ${e.message}", e)
        }
    }
}
