package com.fpf.smartscan.lib.processors

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.fpf.smartscan.data.prototypes.PrototypeEmbedding
import com.fpf.smartscan.data.prototypes.PrototypeEmbeddingDatabase
import com.fpf.smartscan.data.prototypes.PrototypeEmbeddingRepository
import com.fpf.smartscan.lib.clip.Embeddings
import com.fpf.smartscan.lib.clip.ModelType
import com.fpf.smartscan.lib.clip.getSimilarities
import com.fpf.smartscan.lib.clip.getTopN
import com.fpf.smartscan.lib.getBitmapFromUri
import com.fpf.smartscan.lib.moveFile
import com.fpf.smartscan.lib.MemoryUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.core.content.edit
import com.fpf.smartscan.data.movehistory.MoveHistory
import com.fpf.smartscan.data.movehistory.MoveHistoryDatabase
import com.fpf.smartscan.data.movehistory.MoveHistoryRepository
import com.fpf.smartscan.workers.ClassificationWorker
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipImageEmbedder
import com.fpf.smartscansdk.core.ml.models.ResourceId
import com.fpf.smartscan.R

class Organiser(private val context: Context) {
    val embeddingHandler = ClipImageEmbedder(context.resources, ResourceId(R.raw.image_encoder_quant_int8))


    companion object {
        private const val TAG = "Organiser"
        private const val PREF_KEY_LAST_USED_CLASSIFICATION_DIRS = "last_used_destinations"
    }

    private val prototypeRepository: PrototypeEmbeddingRepository = PrototypeEmbeddingRepository(PrototypeEmbeddingDatabase.getDatabase(context.applicationContext as Application).prototypeEmbeddingDao())
    private val moveHistoryRepository: MoveHistoryRepository = MoveHistoryRepository(MoveHistoryDatabase.getDatabase(context.applicationContext as Application).moveHistoryDao())

    private val memoryUtils = MemoryUtils(context)

    suspend fun processBatch(imageUris: List<Uri>, scanId: Int): Int = withContext(Dispatchers.IO) {
        if (imageUris.isEmpty()) {
            Log.i(TAG, "No image files found for classification.")
            return@withContext 0
        }

        val prototypeList: List<PrototypeEmbedding> = prototypeRepository.getAllEmbeddingsSync()
        if (prototypeList.isEmpty()) {
            Log.e(TAG, "No prototype embeddings available.")
            return@withContext 0
        }

        embeddingHandler.initialize()

        val lastUsedDestinationDirectories = prototypeList.map { it.id }

        var totalProcessed = 0

        try {
            for (chunk in imageUris.chunked(10)) {
                val currentConcurrency = memoryUtils.calculateConcurrencyLevel()
//                Log.i(
//                    TAG, "Current allowed concurrency: $currentConcurrency | Free Memory: ${
//                        memoryUtils.getFreeMemory() / (1024 * 1024)
//                    } MB"
//                )
                val semaphore = Semaphore(currentConcurrency)

                val deferredResults = chunk.map { imageUri ->
                    async {
                        try {
                            semaphore.withPermit {
                                processImage(imageUri, prototypeList, scanId)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in coroutine for $imageUri: ${e.message}", e)
                            false
                        }
                    }
                }

                // Wait for all processing in this chunk to complete
                val results = deferredResults.awaitAll()
                totalProcessed += results.count { it }
            }

            return@withContext totalProcessed
        }
        finally {
            saveLastUsedDestinations(context, lastUsedDestinationDirectories)
        }
    }

    private suspend fun processImage(imageUri: Uri, prototypeEmbeddings: List<PrototypeEmbedding>, scanId: Int): Boolean {
        return try {
            val bitmap = getBitmapFromUri(context, imageUri)
            val imageEmbedding = embeddingHandler.embed(bitmap)
            bitmap.recycle()


            val similarities = getSimilarities(imageEmbedding, prototypeEmbeddings.map { it.embeddings })
            val top2 = getTopN(similarities, 2)

            if(top2.isEmpty()) return false

            val bestIndex = top2[0]
            val bestSim = similarities[bestIndex]
            val secondSim = top2.getOrNull(1)?.let { similarities[it] } ?: 0f

            val threshold = 0.4f
            val minMargin = 0.05f

            if (bestSim < threshold) return false // don't move if below threshold
            if((bestSim - secondSim) < minMargin) return false // don't move if gap between best and second is too small

            val destinationString = prototypeEmbeddings.getOrNull(bestIndex)?.id

            if (destinationString == null) {
                Log.e(TAG, "Image classification failed for URI: $imageUri")
                false
            } else {
                val newFileUri = moveFile(context, imageUri, destinationString.toUri())
                val isMoved = newFileUri != null
                if(isMoved){
                    moveHistoryRepository.insert(
                        MoveHistory(
                            scanId = scanId,
                            sourceUri = imageUri.toString(),
                            destinationUri = newFileUri.toString(),
                            date = System.currentTimeMillis(),
                        )
                    )
                }
                isMoved
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image $imageUri: ${e.message}", e)
            false
        }
    }

    private fun saveLastUsedDestinations(context: Context, files: List<String>) {
        val prefs = context.getSharedPreferences(ClassificationWorker.JOB_NAME, Context.MODE_PRIVATE)
        prefs.edit() {
            putStringSet(PREF_KEY_LAST_USED_CLASSIFICATION_DIRS, files.toSet())
        }
    }


    fun close() {
        embeddingHandler?.closeSession()
    }
}